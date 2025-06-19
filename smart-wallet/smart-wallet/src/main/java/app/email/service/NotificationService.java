package app.email.service;

import app.email.client.NotificationClient;
import app.email.client.dto.Notification;
import app.email.client.dto.NotificationPreference;
import app.email.client.dto.NotificationRequest;
import app.email.client.dto.UpsertNotificationPreference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class NotificationService {

    private final NotificationClient notificationClient;

    @Autowired
    public NotificationService(NotificationClient notificationClient) {
        this.notificationClient = notificationClient;
    }



    public void saveNotificationPreference(UUID userId, boolean isEmailEnabled,  String email) {

        UpsertNotificationPreference upsertNotificationPreference = UpsertNotificationPreference.builder()
                .userId(userId)
                .notificationEnabled(isEmailEnabled)
                .type("EMAIL")
                .contactInfo(email)
                .build();

        //Invoke Feign client and execute Http post request
        ResponseEntity <Void> httpResponse = notificationClient.upsertNotificationPreference (upsertNotificationPreference);

        try {
            // if status code is not 200 then log error,investigate time to implement error handling
            if (!httpResponse.getStatusCode ().is2xxSuccessful ()) {
                log.error ("Failed to save notification preference, for user with id [%s] ".formatted (userId));
            }

        } catch (Exception e) {
            log.error ("Unable to call notification-svc.");
        }


    }




    public NotificationPreference getNotificationPreferences(UUID userId) {

        ResponseEntity <NotificationPreference> httpResponse = notificationClient.getUserPreference (userId);

        if (!httpResponse.getStatusCode ().is2xxSuccessful ()) {
            throw new RuntimeException ("Failed to get notification preference, for user with id [%s] ".formatted (userId));
        }

        return httpResponse.getBody ();
    }



    public List <Notification> getNotificationHistory(UUID userId) {

        ResponseEntity <List <Notification>> httpResponseHistory = notificationClient.getNotificationHistory (userId);

        return httpResponseHistory.getBody ();

    }



    // send notification for transfer many
    public void sendNotification(UUID userId, String emailSubject, String emailBody) {

        NotificationRequest notificationRequest = NotificationRequest.builder ()
                .userId (userId)
                .subject (emailSubject)
                .body (emailBody)
                .build ();

        ResponseEntity <Void> httpResponse;

        try {
        httpResponse = notificationClient.sendNotification (notificationRequest);

        if (!httpResponse.getStatusCode ().is2xxSuccessful ()) {
            log.error ("Failed to send notification, for user with id [%s] ".formatted (userId));
           }
        } catch (Exception e) {
            log.warn ("Unable to call notification-svc. 500 internal server error");
        }

    }




    public void updateNotificationPreference(UUID userId, boolean enabled) {

        try {
            notificationClient.updateNotificationPreferences(userId, enabled);
        } catch (Exception e) {
            log.warn("Can't update notification preferences for user with id = [%s].".formatted(userId));
        }
    }
}
