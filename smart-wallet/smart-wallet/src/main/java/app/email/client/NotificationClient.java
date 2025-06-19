package app.email.client;

import app.email.client.dto.Notification;
import app.email.client.dto.NotificationPreference;
import app.email.client.dto.NotificationRequest;
import app.email.client.dto.UpsertNotificationPreference;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


// url - base endpoint of notification service
@FeignClient(name = "notification-svc", url = "http://localhost:8081/api/v1/notifications")
//@FeignClient(name = "notification-svc", url = "${notification-svc.base-url}")
public interface NotificationClient {




    @PostMapping("/preferences")
    ResponseEntity<Void> upsertNotificationPreference(@RequestBody UpsertNotificationPreference notificationPreference);

    @GetMapping("/preferences")
    ResponseEntity<NotificationPreference> getUserPreference(@RequestParam(name = "userId") UUID userId);

    @GetMapping
    ResponseEntity <List <Notification>>  getNotificationHistory(@RequestParam(name = "userId") UUID userId);

    @PostMapping
    ResponseEntity<Void> sendNotification(@RequestBody NotificationRequest notificationRequest);

    @PutMapping("/preferences")
    ResponseEntity<Void> updateNotificationPreferences(@RequestParam(name = "userId") UUID userId, @RequestParam(name = "enabled") boolean enabled);
}
