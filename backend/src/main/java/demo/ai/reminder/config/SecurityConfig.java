package demo.ai.reminder.config;

import demo.ai.reminder.security.ApiAuthenticationEntryPoint;
import demo.ai.reminder.security.LoginUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 세션(쿠키) 기반 인증.
 * <ul>
 *   <li>로그인/로그아웃은 폼 로그인 필터 대신 AuthController가 JSON으로 처리하고, 인증 정보를 세션에 저장한다.</li>
 *   <li>/api/auth/signup, /api/auth/login, /api/auth/logout 을 제외한 /api/** 는 로그인이 필요하며, 아니면 401(ApiResponse)로 응답한다.</li>
 *   <li>프론트엔드(localhost:3000)가 세션 쿠키를 보낼 수 있도록 CORS에서 allowCredentials를 켠다.</li>
 *   <li>CSRF 토큰은 쓰지 않는다. 세션 쿠키는 SameSite=Lax(application.yml의 server.servlet.session.cookie)라 다른 사이트의 요청에는 실리지 않고,
 *       CORS는 프론트엔드 출처만 허용하며 JSON 요청은 preflight를 거치기 때문이다 (데모 범위의 선택).</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    private static final String FRONTEND_ORIGIN = "http://localhost:3000";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ApiAuthenticationEntryPoint entryPoint)
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                // API라 로그인 후 원래 요청으로 돌아갈 일이 없으므로, 미인증 요청을 세션에 저장(세션 생성)하지 않는다.
                .requestCache(AbstractHttpConfigurer::disable)
                // H2 콘솔은 frame으로 화면을 그리므로 같은 출처의 frame은 허용한다.
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/signup", "/api/auth/login", "/api/auth/logout").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(entryPoint));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(FRONTEND_ORIGIN));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(LoginUserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
