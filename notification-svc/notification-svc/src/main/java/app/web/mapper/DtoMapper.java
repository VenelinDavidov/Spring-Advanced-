package app.web.mapper;

import app.model.Notification;
import app.model.NotificationPreference;
import app.model.NotificationType;
import app.web.dto.NotificationPreferencesResponse;
import app.web.dto.NotificationResponse;
import app.web.dto.NotificationTypeRequest;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DtoMapper {


    public static NotificationType fromNotificationTypeRequest (NotificationTypeRequest dto) {

        return switch (dto) {
            case EMAIL -> NotificationType.EMAIL;
        };
    }


    // Build dto from entity
    public static NotificationPreferencesResponse  fromNotificationPreference (NotificationPreference entity) {

        return NotificationPreferencesResponse.builder ()
                .id (entity.getId ())
                .userId (entity.getUserId ())
                .type (entity.getType ())
                .enabled (entity.isEnabled ())
                .contactInfo (entity.getContactInfo ())
                .build ();

    }

    public static NotificationResponse fromNotification(Notification entity) {

        return NotificationResponse.builder ()
                .subject (entity.getSubject ())
                .createdOn (entity.getCreatedOn ())
                .status (entity.getStatus ())
                .type (entity.getType ())
                .build ();
    }
}
