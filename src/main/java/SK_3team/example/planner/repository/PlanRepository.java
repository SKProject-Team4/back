package SK_3team.example.planner.repository;

import SK_3team.example.planner.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {
    // 특정 날짜(start_time 기준)의 일정을 가져오기
    List<Plan> findByStartBetween(LocalDateTime startOfDay, LocalDateTime endOfDay);

    // 특정 userId에 해당하는 모든 일정을 찾기
    List<Plan> findByUserId(Long userId);


    // 특정 userId와 날짜 범위에 해당하는 일정을 찾기(캘린더 날짜 클릭 시)
    List<Plan> findByUserIdAndStartBetween(Long userId, LocalDateTime startOfDay, LocalDateTime endOfDay);

    // 특정 userId와 Plan ID에 해당하는 단일 일정을 찾는 메서드 (상세보기 시)
    Optional<Plan> findByIdAndUserId(Long id, Long userId);

    // 게스트 키로 일정을 찾는 메서드
    Optional<Plan> findByGuestKey(String guestKey);

    // 게스트 키와 userId가 NULL인 일정을 찾는 메서드 (게스트 전용 일정 확인용)
    Optional<Plan> findByGuestKeyAndUserIdIsNull(String guestKey);


}