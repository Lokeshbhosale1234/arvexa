package com.fintech.dto;

import com.fintech.entity.*;
import jakarta.validation.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

class TransactionValidationTest {
    @Test void preventsDatabaseRoundingOfFractionalPaise() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var dto = TransactionDTO.builder().title("Lunch").category("Food").type(TransactionType.DEBIT)
                    .status(TransactionStatus.COMPLETED).amount(new BigDecimal("1.001")).build();
            assertThat(factory.getValidator().validate(dto)).anyMatch(v -> v.getPropertyPath().toString().equals("amount"));
            dto.setAmount(new BigDecimal("1.01"));
            assertThat(factory.getValidator().validate(dto)).isEmpty();
        }
    }
}
