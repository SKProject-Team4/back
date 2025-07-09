package SK_3team.example.planner.controller;

import SK_3team.example.planner.dto.UserDTO;
import SK_3team.example.planner.jwt.JWTUtil;
import SK_3team.example.planner.redis.RedisUtil;
import SK_3team.example.planner.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final RedisUtil redisUtil;
    private final JWTUtil jwtUtil;

    @GetMapping("/")
    public String mainPage(){
        return "Main Page";
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody UserDTO userDTO) {
        userService.save(userDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body("register OK");
    }


    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody UserDTO userDTO) {
        UserDTO loggedInUser = userService.login(userDTO);
        if (loggedInUser != null) {
            return ResponseEntity.ok("Login OK");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }

}
