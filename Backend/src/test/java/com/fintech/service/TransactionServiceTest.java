package com.fintech.service;

import com.fintech.dto.TransactionDTO;
import com.fintech.entity.*;
import com.fintech.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock TransactionRepository transactions;
    @Mock UserRepository users;
    @InjectMocks TransactionService service;
    User owner = User.builder().id(1L).email("owner@example.test").build();

    @BeforeEach void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(owner.getEmail(), null));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void listUsesAuthenticatedIdentity() {
        when(transactions.findByUserEmailOrderByTransactionDateDesc(owner.getEmail())).thenReturn(List.of());
        assertThat(service.getAllTransactions()).isEmpty();
        verify(transactions).findByUserEmailOrderByTransactionDateDesc(owner.getEmail());
        verify(transactions, never()).findAll();
    }
    @Test void cannotDeleteAnotherOwnersTransaction() {
        when(users.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(transactions.findByIdAndUser(99L, owner)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteTransaction(99L))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verify(transactions, never()).delete(any(Transaction.class));
    }
    @Test void newTransactionGetsAuthenticatedOwnerAndExactDecimal() {
        when(users.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(transactions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service.createTransaction(TransactionDTO.builder().title("Lunch").amount(new BigDecimal("125.50"))
                .category("Food").type(TransactionType.DEBIT).build());
        ArgumentCaptor<Transaction> saved = ArgumentCaptor.forClass(Transaction.class);
        verify(transactions).save(saved.capture());
        assertThat(saved.getValue().getUser()).isSameAs(owner);
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("125.50");
        assertThat(saved.getValue().getStatus()).isEqualTo(TransactionStatus.COMPLETED);
    }
    @Test void rejectsUnknownTypeInsteadOfSilentlyRemovingFilter() {
        when(users.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        assertThatThrownBy(() -> service.searchTransactions(null,null,"INVALID",null,null,null,null))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rejectsReversedAmounts() {
        when(users.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        assertThatThrownBy(() -> service.searchTransactions(null,null,null,null,null,new BigDecimal("100"),BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void analyticsHandlesEmptyAccountWithoutNullTotals() {
        when(users.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        var analytics = service.getAnalytics();
        assertThat(analytics.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(analytics.getMonthlyTrend()).hasSize(6);
    }
}
