package SK_3team.example.planner.controller;

import SK_3team.example.planner.dto.PlanResponseDto;
import SK_3team.example.planner.dto.PlanDetailResponseDto;
import SK_3team.example.planner.dto.PlanRequestDto;
import SK_3team.example.planner.jwt.JWTUtil;
import SK_3team.example.planner.service.PlanService;
import SK_3team.example.planner.exception.AuthException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.io.IOException; // IOException 임포트 추가

@RestController
@RequestMapping("/plans")
public class PlanController {

    private final PlanService planService;
    private final JWTUtil jwtUtil;

    public PlanController(PlanService planService, JWTUtil jwtUtil) {
        this.planService = planService;
        this.jwtUtil = jwtUtil;
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        System.out.println("권한헤더 " + authorizationHeader);
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                return jwtUtil.getUserIdFromToken(token);
            }
        }
        return null;
    }

    // 특정 날짜의 일정 목록을 가져오는 엔드포인트 (회원 전용)
    @GetMapping("/get_plans_by_date")
    public ResponseEntity<List<PlanResponseDto>> getPlansByDate(
            @RequestParam("date") LocalDate date,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            throw new AuthException("로그인이 필요합니다. (회원 전용 기능)");
        }
        // PlanService의 메서드 파라미터 순서에 맞춰 userId를 먼저 전달
        List<PlanResponseDto> plans = planService.getPlansByDateForUser(userId, date);
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

    @GetMapping("/get_plans") //
    public ResponseEntity<List<PlanResponseDto>> getAllPlans(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);

        if (userId == null) {
            throw new AuthException("로그인이 필요합니다. (회원 전용 기능)");
        }

        List<PlanResponseDto> plans = planService.getAllPlansForUser(userId);

        if (plans.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(plans, HttpStatus.OK);
    }

    // ** 추가: 임시 계획 시작 (게스트 키 발급) 엔드포인트 **
    @PostMapping("/start")
    public ResponseEntity<PlanResponseDto> startPlan() {
        // 이 엔드포인트는 게스트 키 발급만을 위한 것이므로,
        // 어떠한 HTTP 요청 객체나 인증 정보도 필요로 하지 않습니다.
        PlanResponseDto newPlan = planService.startGuestPlan(); // PlanService에 새로운 메서드 호출
        return new ResponseEntity<>(newPlan, HttpStatus.CREATED);
    }

    // ** 변경: 기존 createPlan 대신 savePlan으로 변경 (또는 updatePlanDetails 등) **
    // 사용자가 입력 페이지에서 정보를 제출할 때 호출 (guestKey 또는 userId 기반으로 기존 Plan 업데이트/생성)
    @PostMapping("/save") // 엔드포인트 이름 변경 (create 대신 save/update 개념)
    public ResponseEntity<PlanResponseDto> savePlan(
            @RequestBody PlanRequestDto requestDto,
            HttpServletRequest request,
            @RequestParam(required = false) String guestKey) { // guestKey를 쿼리 파라미터로 받을 수 있도록

        Long userId = getUserIdFromRequest(request);

        // 서비스 메서드 호출 시 guestKey도 함께 전달
        PlanResponseDto savedPlan = planService.createOrUpdatePlan(requestDto, userId, guestKey);
        return new ResponseEntity<>(savedPlan, HttpStatus.OK); // 200 OK (업데이트 개념이 강하므로)
    }

    // JPG/PDF 파일 저장 엔드포인트 (회원/게스트 공용)
    // planId (회원) 또는 guestKey (게스트) 둘 중 하나는 필수로 전달되어야 합니다.
    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPlanAsPdf(
            HttpServletRequest request, // userId 추출용
            @RequestParam(required = false) Long planId, // 회원 일정용
            @RequestParam(required = false) String guestKey) throws IOException { // IOException 처리 추가

        Long userId = getUserIdFromRequest(request); // JWT에서 userId 추출

        // planId와 guestKey 중 하나라도 없으면 에러 처리
        if (planId == null && (guestKey == null || guestKey.isEmpty())) {
            throw new IllegalArgumentException("일정 ID 또는 게스트 키가 필요합니다.");
        }
        // 로그인 상태인데 guestKey가 주어지면 (잘못된 요청일 수 있음, 또는 게스트 키로 자신의 게스트 일정 찾기)
        // userId가 있고 planId가 없는 경우 guestKey를 사용하는 시나리오도 허용.
        // PlanService의 findPlanForExport에서 복합적으로 처리.

        byte[] pdfBytes = planService.generatePlanPdf(planId, guestKey, userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        String fileName = "plan_" + (planId != null ? planId : (guestKey != null ? guestKey.substring(0, 8) : "unknown")) + ".pdf"; // 파일명 유동적 생성
        headers.setContentDispositionFormData("attachment", fileName);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    // JPG 파일 저장 엔드포인트 (회원/게스트 공용)
    @GetMapping("/export/jpg")
    public ResponseEntity<byte[]> exportPlanAsJpg(
            HttpServletRequest request,
            @RequestParam(required = false) Long planId,
            @RequestParam(required = false) String guestKey) throws IOException { // IOException 처리 추가

        Long userId = getUserIdFromRequest(request);

        if (planId == null && (guestKey == null || guestKey.isEmpty())) {
            throw new IllegalArgumentException("일정 ID 또는 게스트 키가 필요합니다.");
        }

        byte[] jpgBytes = planService.generatePlanJpg(planId, guestKey, userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        String fileName = "plan_" + (planId != null ? planId : (guestKey != null ? guestKey.substring(0, 8) : "unknown")) + ".jpg";
        headers.setContentDispositionFormData("attachment", fileName);

        return new ResponseEntity<>(jpgBytes, headers, HttpStatus.OK);
    }

    // 일정 수정 (로그인 회원만 가능) - 기존 PUT /plans/update/{id} 유지 (save와 역할 분리)
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