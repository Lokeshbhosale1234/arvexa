package com.fintech.capture;
import com.fintech.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface CaptureUserLock extends JpaRepository<User,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select u from User u where u.id=:id")
 Optional<User> lock(@Param("id") Long id);
}
