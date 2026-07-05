package vn.uth.careercompass.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import vn.uth.careercompass.kernel.service.CustomOidcUserService;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, CustomOidcUserService customOidcUserService) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/", "/login", "/register", "/forgot", "/reset-password", "/oauth2/**", "/css/**", "/js/**", "/p/**").permitAll()
                    .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                    .requestMatchers("/counselor", "/counselor/**").hasRole("COUNSELOR")
                    .anyRequest().authenticated()
                )

                .formLogin(form -> form
                    .loginPage("/login")
                    .defaultSuccessUrl("/", true)
                    .permitAll()
                )

                .oauth2Login(oauth -> oauth
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .userInfoEndpoint(userInfo ->
                                userInfo.oidcUserService(customOidcUserService))
                )

                .logout(logout -> logout
                    .logoutSuccessUrl("/login?logout")
                    .permitAll()
                );

        return http.build();
    }

}
