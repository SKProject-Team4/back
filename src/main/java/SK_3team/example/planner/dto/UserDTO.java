package SK_3team.example.planner.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String password;
    private LocalDateTime createdAt;
    private String role;
}
