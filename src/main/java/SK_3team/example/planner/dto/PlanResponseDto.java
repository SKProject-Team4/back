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
public class PlanResponseDto {

    private String status;
    private Integer code;
    private LocalDateTime start;
    private LocalDateTime end;
    private LocalDateTime createdAt;
    private String title;
    private Long id;
    private String message;


}