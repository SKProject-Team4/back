package SK_3team.example.planner.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "plan_details") // ⭐ 새로운 테이블명
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlanDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ai_chat_content", columnDefinition = "TEXT", nullable = true)
    private String aiChatContent;

    @Column(name = "chat_id", length = 255, nullable = true) // ⭐ 추가: chat_id 컬럼
    private String chatId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", referencedColumnName = "id")
    private Plan plan;

    public PlanDetail(String aiChatContent, Plan plan) {
        this.aiChatContent = aiChatContent;
        this.plan = plan;
    }
}
