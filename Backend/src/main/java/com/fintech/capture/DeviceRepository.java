package com.fintech.capture;
import com.fintech.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface DeviceRepository extends JpaRepository<CaptureDevice,Long> {
 Optional<CaptureDevice> findByTokenHash(String hash);
 List<CaptureDevice> findByUserOrderByIdDesc(User user);
 Optional<CaptureDevice> findByIdAndUser(Long id,User user);
 long countByUserAndRevokedFalse(User user);
}
