package SK_3team.example.planner.config;

import SK_3team.example.planner.jwt.JWTFilter;
import SK_3team.example.planner.jwt.JWTUtil;
import SK_3team.example.planner.jwt.LoginFilter;
import SK_3team.example.planner.jwt.LogoutFilter;
import SK_3team.example.planner.redis.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final AuthenticationConfiguration authenticationConfiguration;
    private final JWTUtil jwtUtil;
    private final RedisUtil redisUtil;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception{
        return configuration.getAuthenticationManager();
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {

        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{

        // csrf disable
        http
                .csrf((auth) -> auth.disable());

        // Form 로그인 방식 disable
        http
                .formLogin((auth) -> auth.disable());

        // http basic 인증방식 disable
        http
                .httpBasic((auth) -> auth.disable());

        http
                .authorizeHttpRequests((auth) -> auth
                        // 1. 사용자 등록 및 로그인 경로는 모두 허용
                        .requestMatchers("/api/users/register", "/api/users/login").permitAll()
                        // ** 추가 ** 임시 계획 시작 (게스트 키 발급)
                        .requestMatchers("/plans/start").permitAll()

                        // ** 변경 ** 일정 저장/업데이트 (guestKey 또는 userId 기반)
                        .requestMatchers("/plans/save").permitAll() // 게스트도 접근해야 하므로 permitAll

                        // 3. JPG/PDF 파일 내보내기 엔드포인트는 모두 접근 허용 (게스트/회원 공용)
                        .requestMatchers("/plans/export/**").permitAll()

                        // 4. 그 외 모든 "/plans/**" 경로 (조회, 수정, 삭제)는 로그인한 사용자(인증된 사용자)만 접근 허용
                        //    (이전의 "/plans/create" 제외)
                        .requestMatchers("/plans/**").authenticated()

                        // 5. 위에 명시되지 않은 다른 모든 요청은 인증 필요   7.8 새벽 수정
                        .anyRequest().authenticated());


        http
                .addFilterBefore(new LogoutFilter(jwtUtil, redisUtil), LoginFilter.class); // 👈 여기 추가
        http
                .addFilterAt(new LoginFilter(authenticationManager(authenticationConfiguration), jwtUtil, redisUtil), UsernamePasswordAuthenticationFilter.class);
        http
                .addFilterBefore(new JWTFilter(jwtUtil, redisUtil), LoginFilter.class);


        http
                .sessionManagement((session) -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));


        return http.build();
    }
}