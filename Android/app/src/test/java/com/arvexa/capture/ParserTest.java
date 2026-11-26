package com.arvexa.capture;
import com.fintech.capture.parser.BankSmsParser;
import org.junit.Test;
import static org.junit.Assert.*;
public class ParserTest {
 @Test public void bankAlertParses(){assertTrue(BankSmsParser.parse("VM-HDFCBK","A/c XX1234 debited INR 450.00 via UPI Ref 123456789012. Avl bal INR 8550.00").isPresent());}
 @Test public void otpIsIgnored(){assertFalse(BankSmsParser.parse("VM-HDFCBK","OTP 123456 for payment INR 450 debited from A/c XX1234").isPresent());}
}
