package com.fintech.capture;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
public final class CaptureDTO {
 public record AccountRequest(@NotBlank @Size(max=20) String bank,@Pattern(regexp="[0-9]{4}") @NotNull String suffix,@NotBlank @Size(max=60) String label) {}
 public record DeviceRequest(@NotBlank @Size(max=60) String name) {}
 public record DeviceCredential(Long id,String token,Instant expiresAt) {}
 public record AlertRequest(@NotBlank @Size(max=80) String eventId,
   @NotBlank @Size(max=20) String bank,@Pattern(regexp="[0-9]{4}") String suffix,
   @NotNull @DecimalMin("0.01") @Digits(integer=13,fraction=2) BigDecimal amount,
   @NotNull @Pattern(regexp="CREDIT|DEBIT") String direction,
   @Pattern(regexp="[A-Z0-9]{6,40}") String reference,@NotNull Instant observedAt) {}
 public record ReviewRequest(@NotNull Long accountId,@NotBlank @Size(max=60) String category) {}
 public record EventView(Long id,String bank,String suffix,BigDecimal amount,String direction,String reference,
   Instant observedAt,String state,Long accountId,Long transactionId) {}
 public record AccountView(Long id,String bank,String suffix,String label,BigDecimal trackedNet,long transactions) {}
 public record DeviceView(Long id,String name,Instant expiresAt,Instant lastSeenAt,boolean revoked) {}
 private CaptureDTO() {}
}
