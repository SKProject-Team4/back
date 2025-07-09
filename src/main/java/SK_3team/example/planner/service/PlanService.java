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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

//** JPG/PDF 라이브러리 관련 임포트 (예시, 실제 구현 시 필요) **
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

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

    public PlanDetailResponseDto getPlanDetailByIdForUser(Long planId, Long userId) { // 'convertToDto' 대신 명확하게 변경
        Plan plan = planRepository.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new PlanNotFoundException("일정을 찾을 수 없습니다. (ID: " + planId + ", User: " + userId + ")"));
        return convertToPlanDetailResponseDto(plan); // 상세 DTO 반환
    }
    @Transactional
    public PlanResponseDto startGuestPlan() {
        Plan plan = new Plan();
        plan.setUserId(null); // 게스트는 userId가 null
        String generatedGuestKey = UUID.randomUUID().toString(); // 고유한 UUID 생성
        plan.setGuestKey(generatedGuestKey); // Plan 엔티티에 게스트 키 저장
        plan.setCreatedAt(LocalDateTime.now());
        // 제목, 시작/종료 시간 등은 아직 설정하지 않음 (null 또는 기본값)

        Plan savedPlan = planRepository.save(plan);

        // API 명세서에 맞게 응답 DTO 생성 (제목, 시작/종료 시간은 아직 없으므로 null)
        return new PlanResponseDto(
                "success",
                201, // Created
                null, // start
                null, // end
                savedPlan.getCreatedAt(),
                null, // title
                savedPlan.getId(),
                savedPlan.getGuestKey(),
                "임시 일정이 성공적으로 생성되었습니다. (게스트 키 발급)"
        );
    }

    // 일정 생성
    // ** 변경: 기존 createPlan 메서드를 createOrUpdatePlan으로 변경 및 로직 수정 **
    // 이 메서드는 이제 사용자가 입력 페이지에서 정보를 제출할 때 호출됩니다.
    @Transactional
    public PlanResponseDto createOrUpdatePlan(PlanRequestDto requestDto, Long userId, String guestKey) {
        Plan plan;

        if (guestKey != null && !guestKey.isEmpty()) {
            // 1. guestKey가 제공되면 기존 임시 계획을 찾아서 업데이트
            plan = planRepository.findByGuestKey(guestKey)
                    .orElseThrow(() -> new PlanNotFoundException("게스트 키에 해당하는 임시 일정을 찾을 수 없습니다."));

            // 만약 로그인한 사용자라면, 이 임시 계획의 소유권을 해당 회원에게 이전
            if (userId != null) {
                plan.setUserId(userId);
                plan.setGuestKey(null); // 회원에게 이전되면 guestKey는 더 이상 필요 없음
            }
        } else if (userId != null) {
            // 2. guestKey 없이 userId만 제공되면 (로그인한 회원이 새로운 계획 생성)
            plan = new Plan();
            plan.setUserId(userId);
            plan.setGuestKey(null); // 회원 일정은 guestKey가 없음
            plan.setCreatedAt(LocalDateTime.now());
        } else {
            // 3. guestKey도 없고 userId도 없는 경우 (잘못된 요청 또는 /plans/start로 유도해야 함)
            throw new IllegalArgumentException("유효한 일정 생성 정보가 부족합니다. 먼저 임시 일정을 시작해주세요.");
        }

        // 공통적으로 Plan 정보 업데이트
        plan.setTitle(requestDto.getTitle());
        plan.setStart(requestDto.getStart());
        plan.setEnd(requestDto.getEnd());

        // PlanDetail 업데이트 또는 생성
        if (plan.getPlanDetail() != null) {
            plan.getPlanDetail().setAiChatContent(requestDto.getAiChatContent());
            plan.getPlanDetail().setChatId(requestDto.getChatId());
        } else {
            PlanDetail newPlanDetail = new PlanDetail();
            newPlanDetail.setPlan(plan);
            newPlanDetail.setAiChatContent(requestDto.getAiChatContent());
            newPlanDetail.setChatId(requestDto.getChatId()); //
            plan.setPlanDetail(newPlanDetail);
        }

        Plan savedPlan = planRepository.save(plan);

        return new PlanResponseDto(
                "success",
                200,
                savedPlan.getStart(),
                savedPlan.getEnd(),
                savedPlan.getCreatedAt(),
                savedPlan.getTitle(),
                savedPlan.getId(),
                savedPlan.getGuestKey(), // 업데이트된 guestKey (회원 전환 시 null이 될 수 있음)
                "일정이 성공적으로 저장되었습니다."
        );
    }

    // 일정 수정(로그인 회원만 가능)
    @Transactional
    public PlanResponseDto updatePlan(Long planId, PlanRequestDto requestDto, Long userId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new PlanNotFoundException("수정할 일정을 찾을 수 없습니다. (ID: " + planId + ")"));

        if (plan.getUserId() == null) {
            throw new AuthException("게스트 일정은 수정할 수 없습니다.");
        }
        if (!plan.getUserId().equals(userId)) {
            throw new AuthException("해당 일정을 수정할 권한이 없습니다.");
        }

        plan.setTitle(requestDto.getTitle());
        plan.setStart(requestDto.getStart());
        plan.setEnd(requestDto.getEnd());

        if (plan.getPlanDetail() != null) {
            plan.getPlanDetail().setAiChatContent(requestDto.getAiChatContent());
            plan.getPlanDetail().setChatId(requestDto.getChatId()); //
        } else {
            // PlanDetail이 없는 경우 새로 생성 (예외적인 상황일 수 있음)
            PlanDetail newDetail = new PlanDetail();
            newDetail.setAiChatContent(requestDto.getAiChatContent());
            newDetail.setChatId(requestDto.getChatId()); //
            newDetail.setPlan(plan); // PlanDetail과 Plan 연결
            plan.setPlanDetail(newDetail);
        }

        Plan updatedPlan = planRepository.save(plan);
        return convertToPlanResponseDto(updatedPlan);
    }

    // 일정 삭제(로그인 회원만 가능)
    @Transactional
    public void deletePlan(Long planId, Long userId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new PlanNotFoundException("삭제할 일정을 찾을 수 없습니다. (ID: " + planId + ")"));

        if (plan.getUserId() == null) {
            throw new AuthException("게스트 일정은 삭제할 수 없습니다.");
        }
        if (!plan.getUserId().equals(userId)) {
            throw new AuthException("해당 일정을 삭제할 권한이 없습니다.");
        }

        planRepository.delete(plan);
    }

    // ** 추가: 일정 조회를 위한 유틸리티 메서드 (회원 or 게스트 파일 내보내기 시 사용) **
    private Plan findPlanForExport(Long planId, String guestKey, Long userId) {
        // 1. 회원 요청인 경우 (userId가 있고, planId가 주어졌을 때)
        if (userId != null && planId != null) {
            return planRepository.findByIdAndUserId(planId, userId)
                    .orElseThrow(() -> new PlanNotFoundException("회원 일정을 찾을 수 없거나 접근 권한이 없습니다."));
        }
        // 2. 게스트 요청인 경우 (userId가 없고, guestKey가 주어졌을 때)
        else if (userId == null && guestKey != null && !guestKey.isEmpty()) {
            // guestKey로만 조회하고, 해당 플랜의 userId가 null인지 다시 확인하여 보안 강화
            return planRepository.findByGuestKeyAndUserIdIsNull(guestKey)
                    .orElseThrow(() -> new PlanNotFoundException("게스트 일정을 찾을 수 없거나 접근 권한이 없습니다."));
        }
        // 3. 유효하지 않은 요청 (둘 다 없거나 잘못된 조합)
        else {
            throw new IllegalArgumentException("유효한 일정 식별자(planId 또는 guestKey)와 사용자 정보가 필요합니다.");
        }
    }

    // ** 추가: PDF 파일 생성 메서드 (회원/게스트 공용) **
    @Transactional(readOnly = true)
    public byte[] generatePlanPdf(Long planId, String guestKey, Long userId) {
        Plan plan = findPlanForExport(planId, guestKey, userId);

        // 이 부분에 실제 PDF 생성 라이브러리(예: iText)를 사용하여 PDF를 만드는 로직을 구현합니다.
        // 현재는 예시 데이터를 반환합니다.
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         try (PdfWriter writer = new PdfWriter(baos);
              PdfDocument pdf = new PdfDocument(writer);
              Document document = new Document(pdf)) {
             document.add(new Paragraph("일정 제목: " + plan.getTitle()));
             document.add(new Paragraph("시작: " + plan.getStart().toLocalDate()));
             document.add(new Paragraph("종료: " + plan.getEnd().toLocalDate()));
             if (plan.getPlanDetail() != null && plan.getPlanDetail().getAiChatContent() != null) {
                 document.add(new Paragraph("AI 채팅 내용: " + plan.getPlanDetail().getAiChatContent()));
             }
             if (plan.getPlanDetail() != null && plan.getPlanDetail().getChatId() != null) {
                 document.add(new Paragraph("대화 ID: " + plan.getPlanDetail().getChatId()));
             }
         } catch (IOException e) {
             throw new RuntimeException("PDF 생성 중 오류 발생", e);
         }
         return baos.toByteArray();

//        System.out.println("PDF 생성 요청: " + plan.getTitle() + " (Plan ID: " + plan.getId() + ", GuestKey: " + plan.getGuestKey() + ")");
//        return ("Dummy PDF Content for " + plan.getTitle() + ". Start: " + plan.getStart()).getBytes(); // 실제 PDF 데이터 반환
    }

    // ** 추가: JPG (이미지) 파일 생성 메서드 (회원/게스트 공용) **
    @Transactional(readOnly = true)
    public byte[] generatePlanJpg(Long planId, String guestKey, Long userId) {
        Plan plan = findPlanForExport(planId, guestKey, userId);

        // 이 부분에 실제 이미지 생성 라이브러리(예: Java AWT Graphics2D, ImageIO)를 사용하여 이미지를 만드는 로직을 구현합니다.
        // 현재는 예시 데이터를 반환합니다.
         BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
         Graphics2D g2d = image.createGraphics();
         g2d.drawString("일정 제목: " + plan.getTitle(), 50, 50);
         g2d.drawString("시작일: " + plan.getStart().toLocalDate(), 50, 80);
        if (plan.getPlanDetail() != null && plan.getPlanDetail().getChatId() != null) { // ⭐ 추가: JPG에 chatId 포함
            g2d.drawString("대화 ID: " + plan.getPlanDetail().getChatId(), 50, 110);
        }
         g2d.dispose();
         ByteArrayOutputStream baos = new ByteArrayOutputStream();

         try {
             ImageIO.write(image, "jpg", baos);
         } catch (IOException e) {
             throw new RuntimeException("JPG 생성 중 오류 발생", e);
         }
         return baos.toByteArray();

//        System.out.println("JPG 생성 요청: " + plan.getTitle() + " (Plan ID: " + plan.getId() + ", GuestKey: " + plan.getGuestKey() + ")");
//        return ("Dummy JPG Content for " + plan.getTitle() + ". Start: " + plan.getStart()).getBytes(); // 실제 JPG 데이터 반환
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
                plan.getGuestKey(), // ** 변경 ** guestKey 포함
                "캘린더 조회가 완료되었습니다"
        );
    }

    // 단일 일정 상세조회할때 사용(상세내용포함)
    private PlanDetailResponseDto convertToPlanDetailResponseDto(Plan plan) {
        // PlanDetail이 null일 수 있으므로 안전하게 처리
        String aiChatContent = (plan.getPlanDetail() != null) ? plan.getPlanDetail().getAiChatContent() : null;
        String chatId = (plan.getPlanDetail() != null) ? plan.getPlanDetail().getChatId() : null;
        return new PlanDetailResponseDto(
                "success",
                200,
                plan.getId(),
                plan.getTitle(),
                plan.getStart(),
                plan.getEnd(),
                plan.getCreatedAt(),
                aiChatContent,
                chatId, // null일 수 있음
                "일정 상세 조회가 완료되었습니다."
        );
    }
}