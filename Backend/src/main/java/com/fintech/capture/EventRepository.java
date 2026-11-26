package com.fintech.capture;
import com.fintech.entity.User;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.*;
public interface EventRepository extends JpaRepository<CaptureEvent,Long> {
 Optional<CaptureEvent> findByUserAndDedupeKey(User user,String key);
 Optional<CaptureEvent> findByIdAndUser(Long id,User user);
 List<CaptureEvent> findByUserOrderByIdDesc(User user, Pageable page);
 List<CaptureEvent> findByUserAndStateOrderByIdDesc(User user,String state, Pageable page);
 List<CaptureEvent> findByUserAndReference(User user,String reference);
 @Query("select e from CaptureEvent e left join fetch e.transaction where e.user=:user and e.account is not null")
 List<CaptureEvent> findAccountEvents(@Param("user") User user);
}
