package SK_3team.example.planner.service;

import SK_3team.example.planner.entity.Plan;
import SK_3team.example.planner.entity.PlanDetail;
import SK_3team.example.planner.repository.PlanRepository;
import SK_3team.example.planner.dto.PlanResponseDto;
import SK_3team.example.planner.dto.PlanDetailResponseDto;
import SK_3team.example.planner.dto.PlanRequestDto;
import SK_3team.example.planner.exception.PlanNotFoundException;
import SK_3team.example.planner.exception.AuthException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID; // UUID 사용
import java.util.stream.Collectors;
import java.util.Optional; // Optional 임포트 추가

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;


@Service
@Transactional(readOnly = true)
public class PlanService {

    private final PlanRepository planRepository;

    public PlanService(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    // 모든 일정 조회 (회원 전용)
    public List<PlanResponseDto> getAllPlansForUser(Long userId) {
        List<Plan> plans = planRepository.findByUserId(userId); // PlanRepository에 findByUserId 메서드가 있다고 가정
        return plans.stream()
                .map(this::convertToPlanResponseDto)
                .collect(Collectors.toList());
    }

    public List<PlanResponseDto> getPlansByDateForUser(Long userId, LocalDate date) { // ⭐ userId가 먼저 오도록 통일
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        List<Plan> plans = planRepository.findByUserIdAndStartBetween(userId, startOfDay, endOfDay);
        return plans.stream()
                .map(this::convertToPlanResponseDto)
                .collect(Collectors.toList());
    }

    public PlanDetailResponseDto getPlanDetailByIdForUser(Long id, Long userId) {
        Plan plan = planRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new PlanNotFoundException("해당 일정을 찾을 수 없습니다. (ID: " + id + ", UserID: " + userId + ")"));
        return convertToPlanDetailResponseDto(plan);
    }

    // ** 추가: 임시 계획 시작 (게스트 키 발급) 메서드 **
    @Transactional
    public PlanResponseDto startGuestPlan() {
        Plan plan = new Plan();
        String guestKey = UUID.randomUUID().toString();
        plan.setGuestKey(guestKey);
        plan.setUserId(null); // 게스트 플랜은 userId가 null
        plan.setCreatedAt(LocalDateTime.now());
        plan.setPlanDetail(null); // PlanDetail은 나중에 save 할 때 연결

        Plan savedPlan = planRepository.save(plan);
        return convertToPlanResponseDto(savedPlan);
    }

    // ** 변경: 기존 createPlan 대신 savePlan으로 변경 (또는 updatePlanDetails 등) **
    // 사용자가 입력 페이지에서 정보를 제출할 때 호출 (guestKey 또는 userId 기반으로 기존 Plan 업데이트/생성)
    @Transactional
    public PlanResponseDto createOrUpdatePlan(PlanRequestDto requestDto, Long userId, String guestKey) { // guestKey 파라미터 추가
        Plan plan;
        String guestKeyFromRequest = guestKey; // 컨트롤러에서 받은 guestKey 사용

        if (guestKeyFromRequest != null && !guestKeyFromRequest.isEmpty()) {
            // Case 1: 요청에 guestKey가 있는 경우 (기존 게스트 일정 로드)
            plan = planRepository.findByGuestKey(guestKeyFromRequest)
                    .orElseThrow(() -> new PlanNotFoundException("해당 게스트 일정을 찾을 수 없습니다. (GuestKey: " + guestKeyFromRequest + ")"));

            // 게스트 키로 찾은 일정이 이미 회원에게 귀속된 경우
            if (plan.getUserId() != null && !plan.getUserId().equals(userId)) { // 로그인한 유저가 다른 유저의 게스트플랜을 수정하려고 할 때
                throw new AuthException("이 일정은 이미 다른 회원에게 귀속되었습니다. 해당 일정은 수정할 수 없습니다.");
            } else if (plan.getUserId() != null && plan.getUserId().equals(userId)) { // 로그인한 유저가 본인의 게스트플랜을 수정하려고 할 때
                // 이 경우, userId가 있는 상태에서 guestKey로 접근한 것이므로, 기존 plan.userId를 유지하고, guestKey를 null로 설정
                plan.setGuestKey(null); // 더 이상 게스트 플랜이 아님
            } else if (userId != null) { // 게스트 플랜을 회원에게 귀속시키려 할 때
                plan.setUserId(userId);
                plan.setGuestKey(null); // 더 이상 게스트 플랜이 아님
            }
        } else if (userId != null) {
            // Case 2: 요청에 guestKey가 없고 userId가 있는 경우 (로그인한 회원의 새 일정 생성)
            plan = new Plan();
            plan.setUserId(userId);
            plan.setGuestKey(null);
            plan.setCreatedAt(LocalDateTime.now());
        } else {
            // Case 3: guestKey도 없고 userId도 없는 경우 (게스트 새 일정 생성)
            plan = new Plan();
            String generatedGuestKey = UUID.randomUUID().toString(); // 새로운 guestKey 생성
            plan.setGuestKey(generatedGuestKey);
            plan.setUserId(null);
            plan.setCreatedAt(LocalDateTime.now());
        }

        // 공통 로직 (PlanDetail 포함)
        plan.setTitle(requestDto.getTitle());
        plan.setStart(requestDto.getStart());
        plan.setEnd(requestDto.getEnd());

        PlanDetail planDetail = plan.getPlanDetail();
        if (planDetail == null) {
            planDetail = new PlanDetail();
            plan.setPlanDetail(planDetail);
            planDetail.setPlan(plan);
        }
        planDetail.setAiChatContent(requestDto.getAiChatContent());
        // planDetail에 chatId 필드가 있다면 추가 (PlanRequestDto에 chatId가 있어야 함)
        // if (requestDto.getChatId() != null) {
        //     planDetail.setChatId(requestDto.getChatId());
        // }

        Plan savedPlan = planRepository.save(plan);
        return convertToPlanResponseDto(savedPlan);
    }



    @Transactional
    public PlanResponseDto updatePlan(Long id, PlanRequestDto requestDto, Long userId) {
        Plan plan = planRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new PlanNotFoundException("수정할 일정을 찾을 수 없습니다. (ID: " + id + ", UserID: " + userId + ")"));

        plan.setTitle(requestDto.getTitle());
        plan.setStart(requestDto.getStart());
        plan.setEnd(requestDto.getEnd());

        PlanDetail planDetail = plan.getPlanDetail();
        if (planDetail == null) {
            planDetail = new PlanDetail();
            plan.setPlanDetail(planDetail);
            planDetail.setPlan(plan);
        }
        planDetail.setAiChatContent(requestDto.getAiChatContent());
        // planDetail에 chatId 필드가 있다면 추가 (PlanRequestDto에 chatId가 있어야 함)
        // if (requestDto.getChatId() != null) {
        //     planDetail.setChatId(requestDto.getChatId());
        // }

        Plan updatedPlan = planRepository.save(plan);
        return convertToPlanResponseDto(updatedPlan);
    }

    @Transactional
    public void deletePlan(Long planId, Long userId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new PlanNotFoundException("삭제할 일정을 찾을 수 없습니다. (ID: " + planId + ")"));

        if (plan.getUserId() == null) { // 게스트 일정을 회원이 삭제 시도
            throw new AuthException("게스트 일정은 삭제할 수 없습니다.");
        }
        if (!plan.getUserId().equals(userId)) {
            throw new AuthException("해당 일정을 삭제할 권한이 없습니다.");
        }

        planRepository.delete(plan);
    }

    // PDF 생성 메서드 (회원/게스트 공용)
    @Transactional(readOnly = true)
    public byte[] generatePlanPdf(Long planId, String guestKey, Long userId) throws IOException { // IOException 추가
        Plan plan = findPlanForExport(planId, guestKey, userId); // 공통 로직으로 일정 찾기

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph("Plan Title: " + plan.getTitle()));
        document.add(new Paragraph("Start Date: " + plan.getStart()));
        document.add(new Paragraph("End Date: " + plan.getEnd()));
        if (plan.getPlanDetail() != null && plan.getPlanDetail().getAiChatContent() != null) {
            document.add(new Paragraph("AI Chat Content: " + plan.getPlanDetail().getAiChatContent()));
        }
        document.close();
        pdf.close();
        writer.close();

        return baos.toByteArray();
    }

    // JPG 생성 메서드 (회원/게스트 공용)
    @Transactional(readOnly = true)
    public byte[] generatePlanJpg(Long planId, String guestKey, Long userId) throws IOException { // IOException 추가
        Plan plan = findPlanForExport(planId, guestKey, userId); // 공통 로직으로 일정 찾기

        int width = 800;
        int height = 600;
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bufferedImage.createGraphics();

        // 배경 설정
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        // 텍스트 설정
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 24)); // 한글 폰트 (예시, 시스템에 설치된 폰트 사용)

        int y = 50;
        g2d.drawString("Plan Title: " + plan.getTitle(), 50, y);
        y += 30;
        g2d.drawString("Start Date: " + plan.getStart(), 50, y);
        y += 30;
        g2d.drawString("End Date: " + plan.getEnd(), 50, y);
        y += 30;
        if (plan.getPlanDetail() != null && plan.getPlanDetail().getAiChatContent() != null) {
            g2d.setFont(new Font("Malgun Gothic", Font.PLAIN, 16));
            g2d.drawString("AI Chat Content:", 50, y);
            y += 20;
            // 긴 내용은 여러 줄로 나누어 그리기
            String content = plan.getPlanDetail().getAiChatContent();
            // 간단하게 줄바꿈 처리 (실제로는 더 복잡한 로직 필요)
            int charLimit = 80; // 한 줄에 표시할 최대 문자 수
            for (int i = 0; i < content.length(); i += charLimit) {
                String line = content.substring(i, Math.min(i + charLimit, content.length()));
                g2d.drawString(line, 50, y += 20);
            }
        }

        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "jpg", baos); // IOException이 발생할 수 있음
        return baos.toByteArray();
    }

    // PDF/JPG 내보내기 시 일정을 찾는 공통 로직
    private Plan findPlanForExport(Long planId, String guestKey, Long userId) {
        if (planId != null) { // Plan ID가 제공된 경우 (회원 일정 또는 회원 계정으로 게스트 일정 조회)
            if (userId != null) {
                return planRepository.findByIdAndUserId(planId, userId)
                        .orElseThrow(() -> new PlanNotFoundException("해당 회원 일정을 찾을 수 없습니다. (ID: " + planId + ", UserID: " + userId + ")"));
            } else {
                // 로그인하지 않은 상태에서 planId로 요청 -> 잘못된 요청 또는 게스트 키를 사용해야 함
                throw new IllegalArgumentException("로그인 없이 Plan ID로 일정을 조회할 수 없습니다. 게스트 키를 사용해주세요.");
            }
        } else if (guestKey != null && !guestKey.isEmpty()) { // Guest Key가 제공된 경우
            Optional<Plan> planOptional = planRepository.findByGuestKey(guestKey);
            if (planOptional.isPresent()) {
                Plan plan = planOptional.get();
                // 게스트 키로 찾은 일정이 회원에게 귀속된 경우, 요청한 userId와 일치하는지 확인
                if (plan.getUserId() != null && userId != null && !plan.getUserId().equals(userId)) {
                    throw new AuthException("해당 게스트 일정을 조회할 권한이 없습니다. (이미 다른 회원에게 귀속)");
                }
                return plan;
            } else {
                throw new PlanNotFoundException("해당 게스트 일정을 찾을 수 없습니다. (GuestKey: " + guestKey + ")");
            }
        } else {
            throw new IllegalArgumentException("일정 ID 또는 게스트 키가 필요합니다.");
        }
    }


    // DTO 변환 메서드들
    private PlanResponseDto convertToPlanResponseDto(Plan plan) {
        return new PlanResponseDto(
                "success",
                200,
                plan.getStart(),
                plan.getEnd(),
                plan.getCreatedAt(),
                plan.getTitle(),
                plan.getId(),
                plan.getGuestKey(), // ⭐ guestKey 포함
                "캘린더 생성 및 저장이 완료되었습니다"
        );
    }

    private PlanDetailResponseDto convertToPlanDetailResponseDto(Plan plan) {
        String aiChatContent = (plan.getPlanDetail() != null) ? plan.getPlanDetail().getAiChatContent() : null;
        String chatId = (plan.getPlanDetail() != null) ? plan.getPlanDetail().getChatId() : null; // ⭐ chatId 추가 (PlanDetail에 chatId 필드가 있다면)

        return new PlanDetailResponseDto(
                "success",
                200,
                plan.getId(),
                plan.getTitle(),
                plan.getStart(),
                plan.getEnd(),
                plan.getCreatedAt(),
                aiChatContent,
                chatId, // ⭐ chatId 추가 (PlanDetailResponseDto에 chatId 필드가 있다면)
                "단일 일정 상세 조회가 완료되었습니다"
        );
    }
}