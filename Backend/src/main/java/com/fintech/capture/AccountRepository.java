package com.fintech.capture;
import com.fintech.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AccountRepository extends JpaRepository<BankAccount,Long> {
 List<BankAccount> findByUserOrderByIdDesc(User user);
 Optional<BankAccount> findByIdAndUser(Long id,User user);
 Optional<BankAccount> findByUserAndBankAndSuffix(User user,String bank,String suffix);
}
