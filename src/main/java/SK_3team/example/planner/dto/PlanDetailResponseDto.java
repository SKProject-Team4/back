package SK_3team.example.planner.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlanDetailResponseDto {
    private String status;
    private Integer code;
    private Long id; // Plan의 ID
    private String title;
    private LocalDateTime start;
    private LocalDateTime end;
    private LocalDateTime createdAt;
    private String aiChatContent;
    private String chatId;
    private String message;

}