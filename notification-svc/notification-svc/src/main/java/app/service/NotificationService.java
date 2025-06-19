package app.service;

import app.model.Notification;
import app.model.NotificationPreference;
import app.model.NotificationStatus;
import app.model.NotificationType;
import app.repository.NotificationPreferenceRepository;
import app.repository.NotificationRepository;
import app.web.dto.NotificationRequest;
import app.web.dto.UpsertNotificationPreference;
import app.web.mapper.DtoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Slf4j
@Service
public class NotificationService {


    private final NotificationPreferenceRepository preferenceRepository;
    private final MailSender mailSender;
    private final NotificationRepository notificationRepository;


    @Autowired
    public NotificationService(NotificationPreferenceRepository notificationPreferenceRepository,
                               MailSender mailSender,
                               NotificationRepository notificationRepository) {
        this.preferenceRepository = notificationPreferenceRepository;
        this.mailSender = mailSender;
        this.notificationRepository = notificationRepository;
    }


    public NotificationPreference upsertPreference(UpsertNotificationPreference dto) {

        // upsert
        // 1. try to find if such exist in the database
        Optional <NotificationPreference> optionalNotificationPreference =
                preferenceRepository.findByUserId (dto.getUserId ());


        // 2. if exists - just update it
        if (optionalNotificationPreference.isPresent ()) {
            NotificationPreference preference = optionalNotificationPreference.get ();

            preference.setContactInfo (dto.getContactInfo ());
            preference.setEnabled (dto.isNotificationEnabled ());
            preference.setType (DtoMapper.fromNotificationTypeRequest (dto.getType ()));
            preference.setUpdatedOn (java.time.LocalDate.now ());
                return preferenceRepository.save (preference);
        }

        // Here I build a new entity object!
        // 3. if does not exist - just create new one
        NotificationPreference notificationPreference = NotificationPreference.builder ()
                .userId (dto.getUserId ())
                .contactInfo (dto.getContactInfo ())
                .enabled (dto.isNotificationEnabled ())
                .type (DtoMapper.fromNotificationTypeRequest (dto.getType ()))
                .createdOn (java.time.LocalDate.now ())
                .updatedOn (java.time.LocalDate.now ())
                .build ();
               return preferenceRepository.save (notificationPreference);
    }




    public NotificationPreference getPreferenceByUserId(UUID userId) {

       return preferenceRepository
                .findByUserId (userId).orElseThrow (() -> new NullPointerException ("NotificationPreference for user id %s was not found!"
                .formatted (userId)));// if not found - throw exception
    }






    public Notification sendNotification(NotificationRequest notificationRequest) {

        // get user preference
        UUID userId = notificationRequest.getUserId();
        NotificationPreference userPreference = getPreferenceByUserId(userId);

        // check if notification is enabled
        if (!userPreference.isEnabled()) {
            throw new IllegalArgumentException("User with id %s does not allow to receive notifications.".formatted(userId));
        }

        //  build message
        SimpleMailMessage message = new SimpleMailMessage ();
        message.setTo (userPreference.getContactInfo ());
        message.setSubject (notificationRequest.getSubject ());
        message.setText (notificationRequest.getBody ());

        // build sending notification
        Notification notification = Notification.builder()
                .subject(notificationRequest.getSubject())
                .body(notificationRequest.getBody())
                .createdOn(LocalDateTime.now())
                .userId(userId)
                .deleted(false)
                .type(NotificationType.EMAIL)
                .build();

        try {
            mailSender.send(message);
            notification.setStatus(NotificationStatus.SUCCEEDED);
        } catch (Exception e) {
            notification.setStatus(NotificationStatus.FAILED);
            log.warn("There was an issue sending an email to %s due to %s.".formatted(userPreference.getContactInfo(), e.getMessage()));
        }

       return notificationRepository.save (notification);
    }





    public List <Notification> getNotificationHistory(UUID userId) {

      return notificationRepository.findAllByUserIdAndDeletedIsFalse (userId);
    }




    public NotificationPreference changeNotificationPreference(UUID userId, boolean enabled) {

        NotificationPreference notificationPreference = getPreferenceByUserId (userId);
        notificationPreference.setEnabled (enabled);
        return preferenceRepository.save (notificationPreference);
    }
}
