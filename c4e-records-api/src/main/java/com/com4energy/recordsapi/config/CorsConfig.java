package com.com4energy.recordsapi.config;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }

    /*
     * @Bean
     * SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
     * http
     * .cors(Customizer.withDefaults())
     * .csrf(csrf -> csrf.disable())
     * .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
     * 
     * return http.build();
     * }
     * 
     * @Bean
     * CorsConfigurationSource corsConfigurationSource() {
     * CorsConfiguration configuration = new CorsConfiguration();
     * configuration.setAllowedOrigins(List.of("http://localhost:4200"));
     * configuration.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"
     * ));
     * configuration.setAllowedHeaders(List.of("*"));
     * configuration.setAllowCredentials(true);
     * 
     * UrlBasedCorsConfigurationSource source = new
     * UrlBasedCorsConfigurationSource();
     * source.registerCorsConfiguration("/**", configuration);
     * return source;
     * }
     */
}
