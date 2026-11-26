package com.fintech.capture;
import com.fintech.entity.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity @Table(name="capture_events",indexes={@Index(name="idx_capture_user_state",columnList="user_id,state"),@Index(name="idx_capture_user_ref",columnList="user_id,bank_reference")},uniqueConstraints=@UniqueConstraint(columnNames={"user_id","dedupe_key"}))
@Getter @Setter @NoArgsConstructor
public class CaptureEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
 @Column(name="dedupe_key",nullable=false,length=64) private String dedupeKey;
 @Column(nullable=false,length=20) private String bank;
 @Column(length=4) private String suffix;
 @Column(nullable=false,precision=15,scale=2) private BigDecimal amount;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=10) private TransactionType direction;
 @Column(name="bank_reference",length=40) private String reference;
 @Column(nullable=false) private Instant observedAt;
 @Column(nullable=false) private Instant receivedAt;
 @Column(nullable=false,length=20) private String state;
 @ManyToOne(fetch=FetchType.LAZY) private BankAccount account;
 @OneToOne(fetch=FetchType.LAZY) @JoinColumn(unique=true) @org.hibernate.annotations.OnDelete(action=org.hibernate.annotations.OnDeleteAction.SET_NULL) private Transaction transaction;
}
