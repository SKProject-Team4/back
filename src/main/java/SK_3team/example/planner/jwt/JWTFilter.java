package SK_3team.example.planner.jwt;

import SK_3team.example.planner.dto.CustomUserDetails;
import SK_3team.example.planner.entity.UserEntity;
import SK_3team.example.planner.redis.RedisUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final RedisUtil redisUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");
        String requestURI = request.getRequestURI();

        // 토큰 잘 불러와지나 체크
        System.out.println(authorization);

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");

        // 여기서 token 필요없는 기능들 다 제끼면 될듯
        // 회원가입, 로그인 요청은 인증없이 통과
        if (requestURI.matches(".*/api/users/register$") ||
            requestURI.matches(".*/api/users/login$") ||
            requestURI.matches(".*/plans/start$") ||
            requestURI.matches(".*/plans/save$")
            ){
                filterChain.doFilter(request, response);
                return;
            }


        // Authorization 상태 이상하면 에러
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json; charset=UTF-8");

            response.getWriter().write("{\"message\": \"인증 토큰이 필요합니다.\"}");
            return;
        }

        // 토큰 만료됐는지, 블랙리스튼지 등 확인
        String token = authorization.substring(7);

        if (jwtUtil.isExpired(token)) {

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"message\": \"토큰이 만료되었습니다.\"}");
            return;
        }

        // redis에서 데이터 가져오기
        String username = jwtUtil.getUsername(token);
        String storedUsername = redisUtil.getData(token);

        if (storedUsername == null || !storedUsername.equals(username)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"message\": \"유효하지 않은 토큰입니다.\"}");
            return;
        }

        if (redisUtil.isBlackListed(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"message\": \"로그아웃 된 토큰입니다.\"}");
            return;
        }

        // 인증처리
        String role = jwtUtil.getRole(token);

        //userEntity를 생성하여 값 set
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(username);
        userEntity.setPassword("temppassword");
        userEntity.setRole(role);

        CustomUserDetails customUserDetails = new CustomUserDetails(userEntity);

        Authentication authToken = new UsernamePasswordAuthenticationToken(
                customUserDetails, null, customUserDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);

    }
}
