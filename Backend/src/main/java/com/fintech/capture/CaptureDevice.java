package com.fintech.capture;
import com.fintech.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Table(name="capture_devices") @Getter @Setter @NoArgsConstructor
public class CaptureDevice {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
 @Column(nullable=false,length=60) private String name;
 @Column(nullable=false,unique=true,length=64) private String tokenHash;
 @Column(nullable=false) private Instant expiresAt;
 private Instant lastSeenAt;
 private boolean revoked;
}
