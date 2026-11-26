package com.fintech.capture;
import com.fintech.capture.parser.BankSmsParser;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;
class BankSmsParserTest {
 @Test void extractsDebitWithoutConfusingBalanceWithAmount(){
  var a=BankSmsParser.parse("VM-HDFCBK-S","A/c XX1234 debited INR 450.00 via UPI Ref 123456789012. Avl bal INR 8,550.00").orElseThrow();
  assertThat(a.amount).isEqualByComparingTo("450.00");assertThat(a.bank).isEqualTo("HDFC");assertThat(a.accountSuffix).isEqualTo("1234");assertThat(a.reference).isEqualTo("123456789012");assertThat(a.direction).isEqualTo("DEBIT");
 }
 @Test void extractsCreditAndIndianGrouping(){
  var a=BankSmsParser.parse("AX-ICICIB","Your account ending 9876 credited with Rs. 1,25,000.50. UTR ABC123456XYZ. Available balance Rs 200000").orElseThrow();
  assertThat(a.amount).isEqualByComparingTo("125000.50");assertThat(a.direction).isEqualTo("CREDIT");
 }
 @Test void missingAccountAndReferenceAreExplicitNotInvented(){
  var a=BankSmsParser.parse("HDFCBK","INR 200 credited").orElseThrow();assertThat(a.accountSuffix).isNull();assertThat(a.reference).isNull();
 }
 @ParameterizedTest @ValueSource(strings={"OTP 123456 for INR 100 debited", "INR 100 debit failed", "INR 100 debited pending", "INR 100 will be debited", "INR 100 payment request received", "You are eligible for a loan INR 100", "INR 100 credited and INR 100 debited", "INR 100 debited and fee INR 20", "INR 0 debited", "INR 1.001 debited", "INR 1,23.45 debited"})
 void ignoresUnsafeOrAmbiguousMessages(String body){assertThat(BankSmsParser.parse("VM-HDFCBK",body)).isEmpty();}
 @ParameterizedTest @ValueSource(strings={"+919876543210","MYBANK","XX-HDFCBK-fake","PAYTM","HDFCBK123"})
 void ignoresUnsupportedSender(String sender){assertThat(BankSmsParser.parse(sender,"A/c XX1234 debited INR 100 Ref 123456789")).isEmpty();}
 @Test void refusesMultipleAccounts(){assertThat(BankSmsParser.parse("HDFCBK","A/c XX1234 debited INR 100 to account XX5678 Ref 123456789")).isEmpty();}
 @Test void boundsInput(){assertThat(BankSmsParser.parse("HDFCBK","a".repeat(4097))).isEmpty();assertThat(BankSmsParser.parse(null,null)).isEmpty();}
}
