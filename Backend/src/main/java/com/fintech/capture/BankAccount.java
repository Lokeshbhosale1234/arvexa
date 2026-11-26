package com.fintech.capture;
import com.fintech.entity.User;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="capture_accounts", uniqueConstraints=@UniqueConstraint(columnNames={"user_id","bank","suffix"}))
@Getter @Setter @NoArgsConstructor
public class BankAccount {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
 @Column(nullable=false,length=20) private String bank;
 @Column(nullable=false,length=4) private String suffix;
 @Column(nullable=false,length=60) private String label;
}
