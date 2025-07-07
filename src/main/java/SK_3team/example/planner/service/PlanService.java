package SK_3team.example.planner.service;

import SK_3team.example.planner.entity.Plan;
import SK_3team.example.planner.entity.PlanDetail; // ⭐ 추가
import SK_3team.example.planner.repository.PlanRepository;
import SK_3team.example.planner.dto.PlanResponseDto;
import SK_3team.example.planner.dto.PlanDetailResponseDto; // ⭐ 추가
import SK_3team.example.planner.dto.PlanRequestDto;
import SK_3team.example.planner.exception.PlanNotFoundException;
import SK_3team.example.planner.exception.AuthException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PlanService {

    private final PlanRepository planRepository;

    public PlanService(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    public List<PlanResponseDto> getAllPlansForUser(Long userId) {
        List<Plan> plans = planRepository.findByUserId(userId);
        return plans.stream()
                .map(this::convertToPlanResponseDto)
                .collect(Collectors.toList());
    }

    public List<PlanResponseDto> getPlansByDateForUser(LocalDate date, Long userId) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        List<Plan> plans = planRepository.findByUserIdAndStartBetween(userId, startOfDay, endOfDay);
        return plans.stream()
                .map(this::convertToPlanResponseDto)
                .collect(Collectors.toList());
    }
    //팝업창에 상세 내용을 보여줄때사용할메서드
    public PlanDetailResponseDto getPlanDetailByIdForUser(Long planId, Long userId) {
        Plan plan = planRepository.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new PlanNotFoundException("일정을 찾을 수 없습니다. (ID: " + planId + ", User: " + userId + ")"));
        return convertToPlanDetailResponseDto(plan);
    }

    // 일정생성(로그인/게스트 모두사용가능)
    @Transactional
    public PlanResponseDto createPlan(PlanRequestDto requestDto, Long userId) {
        Plan plan = new Plan();
        plan.setTitle(requestDto.getTitle());
        plan.setStart(requestDto.getStart());
        plan.setEnd(requestDto.getEnd());
        plan.setCreatedAt(LocalDateTime.now());
        plan.setUserId(userId);

        PlanDetail planDetail = new PlanDetail();
        planDetail.setAiChatContent(requestDto.getAiChatContent());
        plan.setPlanDetail(planDetail);

        Plan savedPlan = planRepository.save(plan);
        return convertToPlanResponseDto(savedPlan);
    }
    //일정 수정(로그인회원만가능)
    @Transactional
    public PlanResponseDto updatePlan(Long planId, PlanRequestDto requestDto, Long userId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new PlanNotFoundException("수정할 일정을 찾을 수 없습니다. (ID: " + planId + ")"));

        if (plan.getUserId() == null || !plan.getUserId().equals(userId)) {
            throw new AuthException("해당 일정을 수정할 권한이 없습니다.");
        }

        plan.setTitle(requestDto.getTitle());
        plan.setStart(requestDto.getStart());
        plan.setEnd(requestDto.getEnd());

        if (plan.getPlanDetail() != null) {
            plan.getPlanDetail().setAiChatContent(requestDto.getAiChatContent());
        } else {
            PlanDetail newDetail = new PlanDetail();
            newDetail.setAiChatContent(requestDto.getAiChatContent());
            plan.setPlanDetail(newDetail);
        }

        Plan updatedPlan = planRepository.save(plan);
        return convertToPlanResponseDto(updatedPlan);
    }

    //일정 삭제(로그인회원만가능)
    @Transactional
    public void deletePlan(Long planId, Long userId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new PlanNotFoundException("삭제할 일정을 찾을 수 없습니다. (ID: " + planId + ")"));

        if (plan.getUserId() == null || !plan.getUserId().equals(userId)) {
            throw new AuthException("해당 일정을 삭제할 권한이 없습니다.");
        }

        planRepository.delete(plan);
    }

    // 캘린더 목록 조회 시 사용
    private PlanResponseDto convertToPlanResponseDto(Plan plan) {
        return new PlanResponseDto(
                "success",
                200,
                plan.getStart(),
                plan.getEnd(),
                plan.getCreatedAt(),
                plan.getTitle(),
                plan.getId(),
                "캘린더 조회가 완료되었습니다"
        );
    }

    // 단일 일정 상세조회할때 사용(상세내용포함)
    private PlanDetailResponseDto convertToPlanDetailResponseDto(Plan plan) {
        String aiChatContent = (plan.getPlanDetail() != null) ? plan.getPlanDetail().getAiChatContent() : null;
        return new PlanDetailResponseDto(
                "success",
                200,
                plan.getId(),
                plan.getTitle(),
                plan.getStart(),
                plan.getEnd(),
                plan.getCreatedAt(),
                aiChatContent,
                "일정 상세 조회가 완료되었습니다"
        );
    }
}