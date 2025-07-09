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

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }

            response.setContentType("application/json;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            PrintWriter out = response.getWriter();

            if (token != null && redisUtil.hasKey(token)) {
                out.write("{\"status\": \"users\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_OK);
                out.write("{\"status\": \"Guest\"}");
            }
            return;
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }
}