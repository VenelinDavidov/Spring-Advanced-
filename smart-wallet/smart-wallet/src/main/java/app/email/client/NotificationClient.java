package app.email.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;


// url - base endpoint of notification service
@FeignClient(name = "notification-svc", url = "http://localhost:8081/api/v1/notifications")
public interface NotificationClient {

//
//    @GetMapping("/test")
//    ResponseEntity <String> getHelloMassage(@RequestParam("name") String name);



}
