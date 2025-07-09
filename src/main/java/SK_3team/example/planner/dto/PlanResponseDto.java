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
    private String guestKey;
    private String message;

    // 예시: PlanService의 convertToPlanResponseDto에서 사용하는 생성자 순서 (id, title, start, end, createdAt, guestKey, message)
    //public PlanResponseDto(Long id, String title, LocalDateTime start, LocalDateTime end, LocalDateTime createdAt, String guestKey, String message) {
    //    this.id = id;
    //    this.title = title;
    //    this.start = start;
    //    this.end = end;
    //    this.createdAt = createdAt;
    //    this.guestKey = guestKey;
    //    this.message = message;
    //    this.status = "success"; // 기본값 설정
    //    this.code = 200; // 기본값 설정
    //}

}