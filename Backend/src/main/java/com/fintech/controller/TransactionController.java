package com.fintech.controller;

import com.fintech.dto.*;
import com.fintech.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TransactionDTO>>> getAllTransactions(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount) {
        boolean filtered = (search != null && !search.isBlank()) || (category != null && !category.isBlank())
                || (type != null && !type.isBlank()) || dateFrom != null || dateTo != null
                || minAmount != null || maxAmount != null;
        return ResponseEntity.ok(ApiResponse.success(filtered
                ? transactionService.searchTransactions(search, category, type, dateFrom, dateTo, minAmount, maxAmount)
                : transactionService.getAllTransactions()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionDTO>> createTransaction(@Valid @RequestBody TransactionDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transaction created successfully", transactionService.createTransaction(dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.ok(ApiResponse.success("Transaction deleted successfully", null));
    }

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<AnalyticsDTO>> getAnalytics() {
        return ResponseEntity.ok(ApiResponse.success(transactionService.getAnalytics()));
    }
}
