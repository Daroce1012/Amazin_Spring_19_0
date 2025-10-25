package com.miw.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        System.out.println("Executing SecurityFilterChain");
        http
        .authorizeHttpRequests((requests) -> requests
            .requestMatchers("/private/**", "/main/**").hasRole("ADMIN")  // Protege las rutas privadas
            .requestMatchers("/resources/**").permitAll()
            .anyRequest().authenticated()  // Requiere autenticación para cualquier otra ruta
        )
        .formLogin(form -> form
            .loginPage("/loginForm")  // Página personalizada de login
            .loginProcessingUrl("/login")  // Procesa la autenticación en esta URL
            .defaultSuccessUrl("/", true)  // Redirige a la raíz tras login exitoso
            .failureUrl("/loginForm?error=wc")  // Redirige a la misma página de login si falla
            .permitAll()
        ).exceptionHandling().accessDeniedPage("/error/403");

        
        return http.build();
    }
    

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails admin = User.builder()
            .username("admin")
            .password(passwordEncoder.encode("amazin"))  // Contraseña encriptada
            .roles("ADMIN")
            .build();

        UserDetails manager = User.builder()
            .username("manager")
            .password(passwordEncoder.encode("amazin"))  // Contraseña encriptada
            .roles("ADMIN", "MANAGER")
            .build();

        return new InMemoryUserDetailsManager(admin, manager);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();  // Usa BCrypt para encriptar contraseñas
    }
    
    @Bean
    public HandlerMappingIntrospector mvcHandlerMappingIntrospector() {
        return new HandlerMappingIntrospector();
    }
    
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        return new DefaultMethodSecurityExpressionHandler();
    }
}

