package com.fintech.capture;

import com.fintech.capture.parser.BankSmsParser;
import com.fintech.entity.*;
import com.fintech.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.PageRequest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class CaptureService {
 private final AccountRepository accounts;
 private final DeviceRepository devices;
 private final EventRepository events;
 private final CaptureUserLock locks;
 private final UserRepository users;
 private final TransactionRepository transactions;
 private static final SecureRandom RANDOM=new SecureRandom();

 private User user(String email) { return users.findByEmail(email).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED)); }
 private User locked(User u) { return locks.lock(u.getId()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED)); }
 public static String hash(String text) {
  try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
  catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
 }
 private void bank(String bank) { if(!BankSmsParser.BANKS.containsValue(bank)) throw new IllegalArgumentException("Unsupported bank"); }
 @Transactional public CaptureDTO.DeviceCredential enroll(String email,String name) {
  User u=locked(user(email));
  if(devices.countByUserAndRevokedFalse(u)>=5) throw new IllegalArgumentException("Revoke an old device before connecting another (limit 5)");
  byte[] bytes=new byte[32]; RANDOM.nextBytes(bytes);
  String token="arvexa_device_"+Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  CaptureDevice d=new CaptureDevice();d.setUser(u);d.setName(name.trim());d.setTokenHash(hash(token));d.setExpiresAt(Instant.now().plus(Duration.ofDays(90)));
  devices.save(d);return new CaptureDTO.DeviceCredential(d.getId(),token,d.getExpiresAt());
 }
 private CaptureDevice authenticate(String token) {
  if(token==null || !token.matches("arvexa_device_[A-Za-z0-9_-]{43}")) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Device is not connected");
  CaptureDevice d=devices.findByTokenHash(hash(token)).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Device is not connected"));
  if(d.isRevoked() || !d.getExpiresAt().isAfter(Instant.now())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Reconnect your device");
  return d;
 }
 @Transactional public void heartbeat(String token) { CaptureDevice d=authenticate(token); d.setLastSeenAt(Instant.now()); }
 @Transactional public CaptureDTO.EventView ingest(String token,CaptureDTO.AlertRequest r) {
  CaptureDevice d=authenticate(token);User u=locked(d.getUser());bank(r.bank());
  if(r.observedAt().isAfter(Instant.now().plusSeconds(300)) || r.observedAt().isBefore(Instant.now().minus(Duration.ofDays(365)))) throw new IllegalArgumentException("SMS timestamp is outside the supported range");
  String identity=r.reference()!=null && r.suffix()!=null ? r.bank()+"|"+r.suffix()+"|"+r.direction()+"|"+r.reference()+"|"+r.amount().setScale(2).toPlainString() : "event|"+r.eventId();
  String key=hash(identity);
  d.setLastSeenAt(Instant.now());
  Optional<CaptureEvent> prior=events.findByUserAndDedupeKey(u,key);
  if(prior.isPresent()) return view(prior.get());
  CaptureEvent e=new CaptureEvent();e.setUser(u);e.setDedupeKey(key);e.setBank(r.bank());e.setSuffix(r.suffix());e.setAmount(r.amount());e.setDirection(TransactionType.valueOf(r.direction()));e.setReference(r.reference());e.setObservedAt(r.observedAt());e.setReceivedAt(Instant.now());e.setState("NEEDS_REVIEW");
  events.save(e);
  if(r.suffix()!=null && r.reference()!=null) accounts.findByUserAndBankAndSuffix(u,r.bank(),r.suffix()).ifPresent(a->post(e,a,"Uncategorized"));
  return view(e);
 }
 private void post(CaptureEvent e,BankAccount a,String category) {
  e.setAccount(a);
  Transaction t=Transaction.builder().user(e.getUser()).title(e.getBank()+" SMS "+(e.getDirection()==TransactionType.DEBIT?"debit":"credit"))
   .description("SMS import • account ending "+a.getSuffix()+" • reference "+Objects.toString(e.getReference(),"not provided"))
   .amount(e.getAmount()).type(e.getDirection()).category(category).status(TransactionStatus.COMPLETED)
   .transactionDate(LocalDateTime.ofInstant(e.getObservedAt(),ZoneId.of("Asia/Kolkata"))).build();
  transactions.save(t);e.setTransaction(t);e.setState("IMPORTED");
  // A matching opposite leg in a different mapped account is an internal transfer, not income/spending.
  if(e.getReference()!=null) for(CaptureEvent other:events.findByUserAndReference(e.getUser(),e.getReference())) {
   if(!Objects.equals(other.getId(),e.getId()) && other.getAccount()!=null && !Objects.equals(other.getAccount().getId(),a.getId())
     && other.getDirection()!=e.getDirection() && other.getAmount().compareTo(e.getAmount())==0 && other.getTransaction()!=null
     && "IMPORTED".equals(other.getState()) && Duration.between(other.getObservedAt(),e.getObservedAt()).abs().compareTo(Duration.ofDays(3))<=0) {
    t.setInternalTransfer(true);other.getTransaction().setInternalTransfer(true);
    e.setState("TRANSFER");other.setState("TRANSFER");break;
   }
  }
 }
 @Transactional public CaptureDTO.AccountView addAccount(String email,CaptureDTO.AccountRequest r) {
  User u=locked(user(email));bank(r.bank());
  if(accounts.findByUserAndBankAndSuffix(u,r.bank(),r.suffix()).isPresent()) throw new IllegalArgumentException("This bank/account ending is already mapped");
  BankAccount a=new BankAccount();a.setUser(u);a.setBank(r.bank());a.setSuffix(r.suffix());a.setLabel(r.label().trim());accounts.save(a);
  for(CaptureEvent e:events.findByUserAndStateAndBankAndSuffixAndReferenceIsNotNull(u,"NEEDS_REVIEW",r.bank(),r.suffix()))
   if(r.bank().equals(e.getBank()) && r.suffix().equals(e.getSuffix()) && e.getReference()!=null) post(e,a,"Uncategorized");
  return new CaptureDTO.AccountView(a.getId(),a.getBank(),a.getSuffix(),a.getLabel(),BigDecimal.ZERO,0);
 }
 @Transactional public CaptureDTO.EventView review(String email,Long id,CaptureDTO.ReviewRequest r) {
  User u=locked(user(email));CaptureEvent e=events.findByIdAndUser(id,u).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  if(!"NEEDS_REVIEW".equals(e.getState())) throw new ResponseStatusException(HttpStatus.CONFLICT,"This alert has already been handled");
  BankAccount a=accounts.findByIdAndUser(r.accountId(),u).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  if(!a.getBank().equals(e.getBank()) || (e.getSuffix()!=null && !e.getSuffix().equals(a.getSuffix()))) throw new IllegalArgumentException("Account does not match this bank alert");
  post(e,a,r.category().trim());return view(e);
 }
 @Transactional public void ignore(String email,Long id) {
  User u=locked(user(email));CaptureEvent e=events.findByIdAndUser(id,u).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  if(!"NEEDS_REVIEW".equals(e.getState())) throw new ResponseStatusException(HttpStatus.CONFLICT,"This alert has already been handled");
  e.setState("IGNORED");
 }
 @Transactional public void revoke(String email,Long id) {
  User u=locked(user(email));devices.findByIdAndUser(id,u).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)).setRevoked(true);
 }
 @Transactional(readOnly=true) public List<CaptureDTO.DeviceView> deviceList(String email) {
  return devices.findByUserOrderByIdDesc(user(email)).stream().map(d->new CaptureDTO.DeviceView(d.getId(),d.getName(),d.getExpiresAt(),d.getLastSeenAt(),d.isRevoked())).toList();
 }
 @Transactional(readOnly=true) public List<CaptureDTO.EventView> eventList(String email,String state) {
  User u=user(email);
  return (state==null?events.findByUserOrderByIdDesc(u,PageRequest.of(0,100)):events.findByUserAndStateOrderByIdDesc(u,state,PageRequest.of(0,100))).stream().map(this::view).toList();
 }
 @Transactional(readOnly=true) public List<CaptureDTO.AccountView> accountList(String email) {
  User u=user(email);List<CaptureEvent> records=events.findAccountEvents(u);
  return accounts.findByUserOrderByIdDesc(u).stream().map(a->{
   BigDecimal net=BigDecimal.ZERO;long count=0;
   for(CaptureEvent e:records) if(Objects.equals(e.getAccount().getId(),a.getId()) && e.getTransaction()!=null && ("IMPORTED".equals(e.getState())||"TRANSFER".equals(e.getState()))) {
    net=e.getDirection()==TransactionType.CREDIT?net.add(e.getAmount()):net.subtract(e.getAmount());count++;
   }
   return new CaptureDTO.AccountView(a.getId(),a.getBank(),a.getSuffix(),a.getLabel(),net,count);
  }).toList();
 }
 private CaptureDTO.EventView view(CaptureEvent e) { return new CaptureDTO.EventView(e.getId(),e.getBank(),e.getSuffix(),e.getAmount(),e.getDirection().name(),e.getReference(),e.getObservedAt(),e.getState(),e.getAccount()==null?null:e.getAccount().getId(),e.getTransaction()==null?null:e.getTransaction().getId()); }
}
