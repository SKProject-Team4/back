package SK_3team.example.planner.jwt;

import SK_3team.example.planner.redis.RedisUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;

@RequiredArgsConstructor
@Component
public class LoginCheckFilter implements Filter {

    private final RedisUtil redisUtil;
    private final JWTUtil jwtUtil;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI();

        if (path.equals("/api/users/logincheck")) {
            String authHeader = request.getHeader("Authorization");
            String token = null;

            System.out.println("LoginCheckFilter: " + authHeader);

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }

            response.setContentType("application/json;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            PrintWriter out = response.getWriter();

            if (token != null) {
                try {
                    String JWTusername = jwtUtil.getUsername(token);
                    String storedUsername = redisUtil.getData(token); // token을 key로 조회

                    if (storedUsername != null && storedUsername.equals(JWTusername)) {
                        out.write("{\"status\": \"users\"}");
                    } else {
                        response.setStatus(HttpServletResponse.SC_OK);
                        out.write("{\"status\": \"Guest\"}");
                    }

                } catch (Exception e) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    out.write("{\"status\": \"Guest\"}");
                }
            } else {
                response.setStatus(HttpServletResponse.SC_OK);
                out.write("{\"status\": \"Guest\"}");
            }

            return;
        }

        // logincheck 요청이 아니면 다음 필터로 넘김
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
