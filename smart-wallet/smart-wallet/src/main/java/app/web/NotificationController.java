package app.web;

import app.email.client.dto.Notification;
import app.email.client.dto.NotificationPreference;
import app.email.service.NotificationService;
import app.security.AuthenticationMetadata;
import app.user.model.User;
import app.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @Autowired
    public NotificationController(NotificationService notificationService,
                                  UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }


    @GetMapping
    public ModelAndView getNotificationPage(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User user = userService.getById (authenticationMetadata.getUserId ());

        NotificationPreference notificationPreferences = notificationService.getNotificationPreferences (user.getId ());
        List <Notification> notificationHistory = notificationService.getNotificationHistory (user.getId ());

        // get last 5 notifications
        notificationHistory = notificationHistory.stream ().limit (5).toList ();

        // get number of succeeded and failed notifications
        long succeededNotificationNumber = notificationHistory
                .stream ()
                .filter (notification -> notification.getStatus ().equals ("SUCCEEDED"))
                .count ();

        long failedNotificationNumber = notificationHistory
                .stream ()
                .filter (notification -> notification.getStatus ().equals ("FAILED"))
                .count ();

        ModelAndView modelAndView = new ModelAndView("notifications");
        modelAndView.addObject("user", user);
        modelAndView.addObject("notificationPreferences", notificationPreferences);
        modelAndView.addObject("succeededNotificationNumber", succeededNotificationNumber);
        modelAndView.addObject("failedNotificationNumber", failedNotificationNumber);
        modelAndView.addObject("notificationHistory", notificationHistory);


        return modelAndView;
    }



    @PutMapping("/user-preference")
    public String updateUserPreference(@RequestParam(name = "enabled") boolean enabled, @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        notificationService.updateNotificationPreference(authenticationMetadata.getUserId(), enabled);

        return "redirect:/notifications";
    }
}
