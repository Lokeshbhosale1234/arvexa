package com.fintech.capture.parser;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.*;

/** Conservative, deterministic parser shared by the server tests and Android. Never retains raw SMS. */
public final class BankSmsParser {
    public static final Map<String, String> BANKS;
    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("HDFCBK", "HDFC"); m.put("HDFCBN", "HDFC");
        m.put("SBIINB", "SBI"); m.put("SBIPSG", "SBI"); m.put("SBMSMS", "SBI");
        m.put("ICICIB", "ICICI"); m.put("AXISBK", "AXIS"); m.put("KOTAKB", "KOTAK");
        m.put("PNBSMS", "PNB"); m.put("BOIIND", "BOI"); m.put("IDFCFB", "IDFC");
        m.put("INDUSB", "INDUSIND"); m.put("CANBNK", "CANARA"); m.put("UNIONB", "UNION");
        BANKS = Collections.unmodifiableMap(m);
    }
    private static final Pattern SENDER = Pattern.compile("^(?:[A-Z]{2}-)?([A-Z]{6})(?:-[A-Z])?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNSAFE = Pattern.compile("\\b(?:otp|one[ -]time|verification code|failed|declined|unsuccessful|pending|collect request|payment request|requested|scheduled|will be debited|due date|minimum due|autopay mandate)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ACCOUNT = Pattern.compile("(?i)\\b(?:a/c|a\\s*cct|acct|account|ac)(?:\\s*(?:no\\.?|number|ending(?:\\s+in)?))?\\s*[:.-]?\\s*([xX*#]+\\s*\\d{4,}|\\d{4,})");
    private static final String MONEY = "(?:INR|Rs\\.?|₹)\\s*([0-9]+(?:,[0-9]+)*(?:\\.[0-9]{1,2})?)(?![0-9,]|\\.[0-9])";
    private static final Pattern AMOUNT = Pattern.compile(MONEY, Pattern.CASE_INSENSITIVE);
    private static final Pattern DIRECTION = Pattern.compile("\\b(debited|credited|withdrawn|spent|paid|received|debit|credit)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern REF = Pattern.compile("(?i)\\b(?:UPI\\s*(?:ref(?:erence)?|txn(?:\\s*id)?)?|ref(?:erence)?(?:\\s*(?:no\\.?|number))?|UTR|RRN|txn(?:\\s*id)?|transaction\\s*(?:id|ref))\\s*[:#.-]?\\s*([A-Z0-9]{6,40})\\b");

    public static final class Alert {
        public final String bank, accountSuffix, direction, reference;
        public final BigDecimal amount;
        public Alert(String bank, String accountSuffix, String direction, BigDecimal amount, String reference) {
            this.bank=bank;this.accountSuffix=accountSuffix;this.direction=direction;this.amount=amount;this.reference=reference;
        }
    }
    public static Optional<Alert> parse(String sender, String body) {
        if (sender==null || body==null || body.length()>4096) return Optional.empty();
        Matcher sm=SENDER.matcher(sender.trim());
        if (!sm.matches()) return Optional.empty();
        String bank=BANKS.get(sm.group(1).toUpperCase(Locale.ROOT));
        if (bank==null || UNSAFE.matcher(body).find()) return Optional.empty();
        Matcher dm=DIRECTION.matcher(body);
        Set<String> directions=new HashSet<>();
        while(dm.find()) directions.add(Set.of("credited","credit","received").contains(dm.group(1).toLowerCase(Locale.ROOT)) ? "CREDIT" : "DEBIT");
        if(directions.size()!=1) return Optional.empty();
        // The transaction amount must precede any balance text. Multiple different amounts need review outside this parser.
        String transactionText=body.split("(?i)\\b(?:avl|available|avail|closing|current|remaining)\\.?\\s*(?:bal(?:ance)?\\.?)?",2)[0];
        Matcher am=AMOUNT.matcher(transactionText);
        Set<BigDecimal> amounts=new HashSet<>();
        while(am.find()) {
            try {
                String value=am.group(1);
                if(value.contains(",") && !value.matches("(?:[0-9]{1,3}(?:,[0-9]{3})+|[0-9]{1,2}(?:,[0-9]{2})*,[0-9]{3})(?:\\.[0-9]{1,2})?")) return Optional.empty();
                BigDecimal n=new BigDecimal(value.replace(",","")).setScale(2);
                if(n.signum()<=0 || n.precision()>15) return Optional.empty();
                amounts.add(n);
            } catch (NumberFormatException e) { return Optional.empty(); }
        }
        if(amounts.size()!=1) return Optional.empty();
        Matcher ac=ACCOUNT.matcher(body); Set<String> suffixes=new HashSet<>();
        while(ac.find()) { String digits=ac.group(1).replaceAll("\\D",""); if(digits.length()>=4) suffixes.add(digits.substring(digits.length()-4)); }
        if(suffixes.size()>1) return Optional.empty();
        Matcher rm=REF.matcher(body); Set<String> refs=new HashSet<>();
        while(rm.find()) {
            String value=rm.group(1).toUpperCase(Locale.ROOT);
            if(value.matches(".*[0-9].*")) refs.add(value);
        }
        if(refs.size()>1) return Optional.empty();
        return Optional.of(new Alert(bank, suffixes.isEmpty()?null:suffixes.iterator().next(), directions.iterator().next(), amounts.iterator().next(), refs.isEmpty()?null:refs.iterator().next()));
    }
    private BankSmsParser() {}
}
