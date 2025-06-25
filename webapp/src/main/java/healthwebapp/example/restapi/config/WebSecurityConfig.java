package healthwebapp.example.restapi.config;

import healthwebapp.example.restapi.entity.User;
import healthwebapp.example.restapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.Optional;

@Configuration
public class WebSecurityConfig {

    @Autowired
    private UserRepository userRepository;

    // Define the PasswordEncoder bean
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Define the UserDetailsService bean
    @Bean
    public UserDetailsService userDetailsService() {
        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
                Optional<User> userOptional = userRepository.findByEmail(email);
                if (!userOptional.isPresent()) {
                    throw new UsernameNotFoundException("User not found with email: " + email);
                }
                User user = userOptional.get();
                return org.springframework.security.core.userdetails.User.builder()
                        .username(user.getEmail())
                        .password(user.getPassword())
                        .authorities(new ArrayList<>()) // Add roles/authorities if needed
                        .build();
            }
        };
    }

    // Define the AuthenticationManager bean
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    // Define the SecurityFilterChain bean
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF as we're using stateless authentication
                .csrf(csrf -> csrf.disable())

                // Define authorization rules
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/healthz").permitAll() // Public endpoint
                        .requestMatchers(HttpMethod.POST, "/v1/user").permitAll() // Allow user registration without authentication
                        .requestMatchers(HttpMethod.DELETE, "/v1/user/self").permitAll() // Allow unauthenticated delete user request
                        .anyRequest().authenticated() // All other endpoints require authentication
                )

                // Enable Basic Authentication
                .httpBasic(org.springframework.security.config.Customizer.withDefaults());

        return http.build();
    }
}
