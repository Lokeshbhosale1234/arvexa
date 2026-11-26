package com.fintech.capture;

import com.fintech.entity.*;
import com.fintech.repository.*;
import com.fintech.service.TransactionService;
import com.fintech.security.JwtUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named="CAPTURE_INTEGRATION",matches="true")
class CaptureIntegrationTest {
 @Autowired CaptureService service; @Autowired UserRepository users; @Autowired TransactionRepository transactions;
 @Autowired EventRepository events; @Autowired AccountRepository accounts; @Autowired MockMvc mvc; @Autowired JwtUtil jwt;
 @Autowired TransactionService transactionService;
 private com.fintech.entity.User owner; private String token;
 @BeforeEach void setup(){
  owner=users.save(com.fintech.entity.User.builder().email("capture-"+UUID.randomUUID()+"@example.test").name("Test").password("not-a-login-password").build());
  token=service.enroll(owner.getEmail(),"Test phone").token();
 }
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 private CaptureDTO.AlertRequest alert(String id,String ref,String suffix,String bank,String direction){return new CaptureDTO.AlertRequest(id,bank,suffix,new BigDecimal("450.00"),direction,ref,Instant.now());}
 private CaptureDTO.AccountView account(String bank,String suffix){return service.addAccount(owner.getEmail(),new CaptureDTO.AccountRequest(bank,suffix,"Test account"));}
 @Test void automaticImportAndRetryAreAtomic(){
  account("HDFC","1234");var request=alert("one","123456789012","1234","HDFC","DEBIT");
  var first=service.ingest(token,request);var retry=service.ingest(token,request);
  assertThat(first.state()).isEqualTo("IMPORTED");assertThat(retry.id()).isEqualTo(first.id());
  assertThat(transactions.countByUser(owner)).isEqualTo(1);assertThat(service.accountList(owner.getEmail()).get(0).trackedNet()).isEqualByComparingTo("-450.00");
 }
 @Test void concurrentDuplicateDeliveryCreatesOneTransaction() throws Exception {
  account("HDFC","1234");var pool=Executors.newFixedThreadPool(4);
  try{List<Callable<Long>> tasks=new ArrayList<>();for(int i=0;i<8;i++)tasks.add(()->service.ingest(token,alert("same","123456789012","1234","HDFC","DEBIT")).id());
   Set<Long> ids=new HashSet<>();for(var f:pool.invokeAll(tasks))ids.add(f.get());assertThat(ids).hasSize(1);assertThat(transactions.countByUser(owner)).isEqualTo(1);
  }finally{pool.shutdownNow();}
 }
 @Test void missingMappingIsHeldThenMappedAutomatically(){
  var e=service.ingest(token,alert("unmapped","123456789012","1234","HDFC","DEBIT"));
  assertThat(e.state()).isEqualTo("NEEDS_REVIEW");assertThat(transactions.countByUser(owner)).isZero();account("HDFC","1234");
  assertThat(service.eventList(owner.getEmail(),null).get(0).state()).isEqualTo("IMPORTED");
 }
 @Test void missingReferenceRequiresOneExplicitReview(){
  var a=account("HDFC","1234");var e=service.ingest(token,alert("no-ref",null,"1234","HDFC","DEBIT"));
  assertThat(e.state()).isEqualTo("NEEDS_REVIEW");service.review(owner.getEmail(),e.id(),new CaptureDTO.ReviewRequest(a.id(),"Food"));
  assertThatThrownBy(()->service.review(owner.getEmail(),e.id(),new CaptureDTO.ReviewRequest(a.id(),"Food"))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
  assertThat(transactions.countByUser(owner)).isEqualTo(1);
 }
 @Test void cannotReviewOrRevokeAnotherUsersData(){
  var other=users.save(com.fintech.entity.User.builder().email(UUID.randomUUID()+"@example.test").name("Other").password("x").build());
  var a=account("HDFC","1234");var e=service.ingest(token,alert("held",null,"1234","HDFC","DEBIT"));
  assertThatThrownBy(()->service.review(other.getEmail(),e.id(),new CaptureDTO.ReviewRequest(a.id(),"Other"))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
  assertThat(service.eventList(other.getEmail(),null)).isEmpty();assertThat(service.accountList(other.getEmail())).isEmpty();
 }
 @Test void deviceRevocationBlocksFurtherSync(){
  Long id=service.deviceList(owner.getEmail()).get(0).id();service.revoke(owner.getEmail(),id);
  assertThatThrownBy(()->service.ingest(token,alert("revoked","123456789012","1234","HDFC","DEBIT"))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
 }
 @Test void internalTransferIsExcludedFromPostedTotalsButRetainedInAccountMovement(){
  account("HDFC","1234");account("SBI","5678");
  service.ingest(token,alert("out","123456789012","1234","HDFC","DEBIT"));service.ingest(token,alert("in","123456789012","5678","SBI","CREDIT"));
  assertThat(service.eventList(owner.getEmail(),null)).allMatch(e->e.state().equals("TRANSFER"));
  assertThat(transactions.sumByUserAndType(owner,TransactionType.DEBIT)).isEqualByComparingTo("0");
  assertThat(transactions.sumByUserAndType(owner,TransactionType.CREDIT)).isEqualByComparingTo("0");
  assertThat(transactions.sumSpentByUserCategoryAndPeriod(owner,"Uncategorized",LocalDate.now(ZoneId.of("Asia/Kolkata")).getMonthValue(),LocalDate.now(ZoneId.of("Asia/Kolkata")).getYear(),TransactionType.DEBIT)).isEqualByComparingTo("0");
  assertThat(service.accountList(owner.getEmail())).extracting(CaptureDTO.AccountView::trackedNet).containsExactlyInAnyOrder(new BigDecimal("450.00"),new BigDecimal("-450.00"));
 }
 @Test void deletingImportedTransactionDoesNotResurrectItOnRetry(){
  account("HDFC","1234");var request=alert("deleted","123456789012","1234","HDFC","DEBIT");var e=service.ingest(token,request);
  SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(owner.getEmail(),null));transactionService.deleteTransaction(e.transactionId());
  var retry=service.ingest(token,request);assertThat(retry.transactionId()).isNull();assertThat(transactions.countByUser(owner)).isZero();
 }
 @Test void deviceTokenCannotReadGeneralApisAndIsRequiredForIngest() throws Exception {
  mvc.perform(get("/api/transactions").header("X-Arvexa-Device-Token",token)).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/capture/device/heartbeat")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/capture/device/heartbeat").header("X-Arvexa-Device-Token",token)).andExpect(status().isOk());
 }
 @Test void httpValidationRejectsFractionalPaise() throws Exception {
  String payload="{\"eventId\":\"invalid\",\"bank\":\"HDFC\",\"suffix\":\"1234\",\"direction\":\"DEBIT\",\"amount\":1.001,\"observedAt\":\""+Instant.now()+"\"}";
  mvc.perform(post("/api/capture/device/ingest").header("X-Arvexa-Device-Token",token).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isBadRequest());
 }
 @Test void nonCompletedTransactionsDoNotAffectTotals(){
  for(TransactionStatus status:List.of(TransactionStatus.FAILED,TransactionStatus.CANCELLED,TransactionStatus.PENDING)) transactions.save(Transaction.builder().user(owner).title("Not posted").description("").amount(BigDecimal.TEN).type(TransactionType.DEBIT).status(status).category("Other").transactionDate(LocalDateTime.now()).build());
  assertThat(transactions.sumByUserAndType(owner,TransactionType.DEBIT)).isEqualByComparingTo("0");
 }
}
