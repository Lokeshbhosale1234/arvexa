package com.fintech.capture;
import com.fintech.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
@RestController @RequestMapping("/api/capture") @RequiredArgsConstructor
public class CaptureController {
 private final CaptureService service;
 @GetMapping("/accounts") public ApiResponse<List<CaptureDTO.AccountView>> accounts(Principal p) {return ApiResponse.success(service.accountList(p.getName()));}
 @PostMapping("/accounts") public ApiResponse<CaptureDTO.AccountView> account(Principal p,@Valid @RequestBody CaptureDTO.AccountRequest r) {return ApiResponse.success(service.addAccount(p.getName(),r));}
 @GetMapping("/events") public ApiResponse<List<CaptureDTO.EventView>> events(Principal p,@RequestParam(required=false) String state) {return ApiResponse.success(service.eventList(p.getName(),state));}
 @PostMapping("/events/{id}/confirm") public ApiResponse<CaptureDTO.EventView> review(Principal p,@PathVariable Long id,@Valid @RequestBody CaptureDTO.ReviewRequest r) {return ApiResponse.success(service.review(p.getName(),id,r));}
 @PostMapping("/events/{id}/ignore") public ApiResponse<Void> ignore(Principal p,@PathVariable Long id) {service.ignore(p.getName(),id);return ApiResponse.success(null);}
 @GetMapping("/devices") public ApiResponse<List<CaptureDTO.DeviceView>> devices(Principal p) {return ApiResponse.success(service.deviceList(p.getName()));}
 @PostMapping("/devices") public ApiResponse<CaptureDTO.DeviceCredential> enroll(Principal p,@Valid @RequestBody CaptureDTO.DeviceRequest r) {return ApiResponse.success(service.enroll(p.getName(),r.name()));}
 @DeleteMapping("/devices/{id}") public ApiResponse<Void> revoke(Principal p,@PathVariable Long id) {service.revoke(p.getName(),id);return ApiResponse.success(null);}
 // Device tokens are checked by the service and never authorize general transaction/account APIs.
 @PostMapping("/device/ingest") public ApiResponse<CaptureDTO.EventView> ingest(@RequestHeader(value="X-Arvexa-Device-Token",required=false) String token,@Valid @RequestBody CaptureDTO.AlertRequest r) {return ApiResponse.success(service.ingest(token,r));}
 @PostMapping("/device/heartbeat") public ApiResponse<Void> heartbeat(@RequestHeader(value="X-Arvexa-Device-Token",required=false) String token) {service.heartbeat(token);return ApiResponse.success(null);}
}
