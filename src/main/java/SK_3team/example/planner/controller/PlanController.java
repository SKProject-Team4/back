package SK_3team.example.planner.controller;

import SK_3team.example.planner.dto.PlanResponseDto;
import SK_3team.example.planner.dto.PlanDetailResponseDto;
import SK_3team.example.planner.dto.PlanRequestDto;
import SK_3team.example.planner.service.PlanService;
import SK_3team.example.planner.util.JwtUtil;
import SK_3team.example.planner.exception.AuthException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/plans")
public class PlanController {

    private final PlanService planService;
    private final JwtUtil jwtUtil;

    public PlanController(PlanService planService, JwtUtil jwtUtil) {
        this.planService = planService;
        this.jwtUtil = jwtUtil;
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                return jwtUtil.getUserIdFromToken(token);
            }
        }
        return null;
    }

    @GetMapping("/get_plans")
    public ResponseEntity<List<PlanResponseDto>> getAllPlans(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);

        if (userId == null) {
            throw new AuthException("로그인이 필요합니다. (회원 전용 기능)");
        }
        List<PlanResponseDto> plans = planService.getAllPlansForUser(userId);

        if (plans.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 일정이 없으면 204
        }
        return new ResponseEntity<>(plans, HttpStatus.OK);
    }

    @GetMapping("/get_plans_by_date")
    public ResponseEntity<List<PlanResponseDto>> getPlansByDate(
            @RequestParam("date") LocalDate date,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            throw new AuthException("로그인이 필요합니다. (회원 전용 기능)");
        }
        List<PlanResponseDto> plans = planService.getPlansByDateForUser(date, userId);
        if (plans.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(plans, HttpStatus.OK);
    }

    // 단일 일정 상세 정보를 가져오는 엔드포인트 (aiChatContent 포함)
    @GetMapping("/get_detail_plans")
    public ResponseEntity<PlanDetailResponseDto> getPlanDetail(
            @RequestParam("plandetails") Long id,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            throw new AuthException("로그인이 필요합니다. (회원 전용 기능)");
        }
        PlanDetailResponseDto planDetail = planService.getPlanDetailByIdForUser(id, userId);
        return new ResponseEntity<>(planDetail, HttpStatus.OK);
    }

    // 일정 생성 (로그인/게스트 가능)
    @PostMapping("/create")
    public ResponseEntity<PlanResponseDto> createPlan(@RequestBody PlanRequestDto requestDto, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        PlanResponseDto newPlan = planService.createPlan(requestDto, userId);
        return new ResponseEntity<>(newPlan, HttpStatus.CREATED);
    }

    // 일정 수정 (로그인 회원만 가능)
    @PutMapping("/update/{id}")
    public ResponseEntity<PlanResponseDto> updatePlan(@PathVariable Long id, @RequestBody PlanRequestDto requestDto, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);

        if (userId == null) {
            throw new AuthException("로그인이 필요합니다. (회원 전용 기능)");
        }

        PlanResponseDto updatedPlan = planService.updatePlan(id, requestDto, userId);
        return new ResponseEntity<>(updatedPlan, HttpStatus.OK);
    }

    // 일정 삭제 (로그인 회원만 가능)
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deletePlan(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);

        if (userId == null) {
            throw new AuthException("로그인이 필요합니다. (회원 전용 기능)");
        }

        planService.deletePlan(id, userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}