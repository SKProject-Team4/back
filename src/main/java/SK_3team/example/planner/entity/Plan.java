package SK_3team.example.planner.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true, length = 255)
    private String title;

    @Column(name = "start_time", nullable = true)
    private LocalDateTime start;

    @Column(name = "end_time", nullable = true)
    private LocalDateTime end;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "user_id", nullable = true)
    private Long userId; // 일정을 생성한 사용자의 ID (null일 경우 게스트)

    @OneToOne(mappedBy = "plan", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private PlanDetail planDetail;

    @Column(unique = true, nullable = true, length = 36)
    private String guestKey;


    public void setPlanDetail(PlanDetail planDetail) {
        this.planDetail = planDetail;
        if (planDetail != null) {
            planDetail.setPlan(this);
        }
    }
}