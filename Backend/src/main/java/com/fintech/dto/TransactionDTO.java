package com.fintech.dto;

import com.fintech.entity.TransactionStatus;
import com.fintech.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDTO {

    private Long id;

    @Size(max = 255, message = "Title must be at most 255 characters")
    @NotBlank(message = "Title is required")
    private String title;

    @Size(max = 255, message = "Description must be at most 255 characters")
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 13, fraction = 2, message = "Amount must have at most 13 integer digits and 2 decimal places")
    private BigDecimal amount;

    @NotNull(message = "Transaction type is required")
    private TransactionType type;

    @NotNull(message = "Status is required")
    private TransactionStatus status;

    @Size(max = 255, message = "Category must be at most 255 characters")
    @NotBlank(message = "Category is required")
    private String category;

    private LocalDateTime transactionDate;

    private LocalDateTime createdAt;
}
