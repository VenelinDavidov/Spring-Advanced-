package app.config;

import app.security.SessionCheckInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {


    @Autowired
    private SessionCheckInterceptor interceptor;


    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // ** -> всичко след
        registry.addInterceptor (interceptor)
                .addPathPatterns ("/**")
                .excludePathPatterns ("/css/**", "/images/**");
    }



    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // authorizeHttpRequests -> конфигурация за достъпа на еднпоинт
        // requestMatchers -> достъп до даден endpoint
        // permitAll-> достъп от всички
        // anyRequest -> всички които не съм изброил
        //authenticated -> достъп само от аутентикирани потребители
        http
                .authorizeHttpRequests (matcher -> matcher
                .requestMatchers (PathRequest.toStaticResources ().atCommonLocations ()).permitAll ()
                .requestMatchers ("/", "/register").permitAll ()
                .anyRequest ().authenticated ()
                )
                .formLogin (form -> form
                        .loginPage ("/login")
                        .usernameParameter ("username")   //default
                        .passwordParameter ("password")    // default
                        .defaultSuccessUrl ("/home")   // after login, go to home
                        .failureUrl ("/login?error")   // if login fail -> go to login page with error
                        .permitAll ());

        return http.build ();
    }
}
