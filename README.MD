# ✈️ AI 여행 플래너
AI와 함께하는 똑똑한 여행 일정 관리 서비스

---

## 📌 1. 프로젝트 개요
AI 여행 플래너는 사용자가 AI 추천을 통해 여행 일정을 손쉽게 생성하고 관리할 수 있도록 돕는 웹 기반 일정 관리 서비스입니다.
캘린더 기반 UI를 활용하여 직관적인 일정 관리가 가능하며, 로그인 없이 게스트 사용자도 임시 키로 일정을 생성/관리할 수 있습니다.

---

## 🚀 2. 주요 기능

### ✅ AI 기반 일정 추천
AI가 날짜에 맞는 여행지 및 활동을 추천
채팅 기반 인터페이스 제공
채팅 내용과 추천 일정은 상세 정보로 저장

### ✅ 일정 생성 / 조회 / 수정 / 삭제 기능
일정 생성: AI 추천을 통해 새 일정 생성 가능 (회원/비회원)
일정 조회: 캘린더에서 일정 클릭 시 상세 정보 확인 가능 (회원)
일정 수정: 등록된 일정의 제목, 시간, 메모 등 수정 가능 (회원)
일정 삭제: 사용자가 기존 일정을 삭제할 수 있음 (회원)

### ✅ 인증 및 권한
JWT 기반 인증 시스템
일반 사용자(USER) / 게스트 사용자(GUEST)

### ✅ 일정 내보내기 
JPG 또는 PDF 형식으로 일정 내보내기 가능

---

## 🛠️ 3. 기술 스택
🔹 Back-End
항목	내용
언어	Java 17
프레임워크	Spring Boot 3.x
ORM	Spring Data JPA
보안	Spring Security + JWT
API 통신	RestTemplate
빌드 도구	Maven
DB	MySQL (MariaDB)
캐시	Redis
환경 변수	dotenv (.env 파일 사용)

🔹 DevOps / 협업 도구
형상 관리: GitHub
문서화: Notion, Google Docs
API 테스트: Postman
컨테이너: Docker, Kubernetes
모니터링: Redis
AI 도구 참고: ChatGPT, Deepseek, Google Gemini

---

## 🧾 4. 백엔드 실행 방법

### ✅ 4.1. MYSQL(MariaDB) 설정

#1. mariaDB 접속
mysql-u root -p

#2. 비번 입력 후 데이터베이스 보기
MariaDB [(none)]> show databases;

#3. 목록에 mysql이 있다면 선택
MariaDB [(none)]> use mysql;

#4. planner_db 생성 및 설정
MariaDB [(mysql)]CREATE DATABASE planner_db;
MariaDB [(mysql)]CREATE USER 'planner'@'%' IDENTIFIED BY 'planner';
MariaDB [(mysql)]GRANT ALL PRIVILEGES ON planner_db.* TO 'planner'@'%';
MariaDB [(mysql)]FLUSH PRIVILEGES;
MariaDB [(mysql)]EXIT;

#5. planner_db 사용자 계정 접속

mysql -u root -p
use planner_db 입력
MariaDB [(mysql)] use planner_db;

### ✅ 4.2. application.yml 설정
spring:
    datasource:
        url: jdbc:mariadb://localhost:3306/planner_db
        username: {사용자이름}
        password: {비밀번호}
    jpa:
    hibernate:
        ddl-auto: update
    show-sql: true

---

🗂️ 5. 백엔드 프로젝트 구조

SK_3team.example.planner/
├── .idea/                           # IntelliJ IDEA 프로젝트 설정 파일 (IDE 관련)
├── .mvn/                            # Maven Wrapper 관련 파일
│   └── wrapper/
│       └── maven-wrapper.properties
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── SK_3team.example.planner/
│   │   │       ├── config/              # 애플리케이션 전반의 설정 클래스
│   │   │       │   ├── RedisConfig      # Redis 관련 설정
│   │   │       │   └── SecurityConfig   # Spring Security 설정
│   │   │       ├── controller/          # REST API 엔드포인트 정의
│   │   │       │   ├── PlanController   # 일정 관련 API 처리
│   │   │       │   └── UserController   # 사용자 관련 API 처리
│   │   │       ├── dto/                 # 클라이언트와 주고받는 데이터 전송 객체 (DTO)
│   │   │       │   ├── mapper           # DTO와 엔티티 간 변환 (매퍼)
│   │   │       │   ├── CustomUserDetails
│   │   │       │   ├── ErrorResponseDto
│   │   │       │   ├── PlanDetailResponseDto # 일정 상세 조회 응답 DTO
│   │   │       │   ├── PlanRequestDto   # 일정 생성/수정 요청 DTO
│   │   │       │   ├── PlanResponseDto  # 일정 조회 응답 DTO
│   │   │       │   └── UserDTO
│   │   │       ├── entity/              # JPA 엔티티 클래스 (데이터베이스 테이블 매핑)
│   │   │       │   ├── Plan             # 핵심 일정 정보
│   │   │       │   ├── PlanDetail       # 일정 상세 정보 (AI 채팅 내용, chat_id 등)
│   │   │       │   └── UserEntity       # 사용자 정보
│   │   │       ├── exception/           # 커스텀 예외 및 전역 예외 처리
│   │   │       │   ├── advice/          # 예외 핸들러 어드바이스
│   │   │       │   ├── AuthException
│   │   │       │   ├── GlobalExceptionHandler
│   │   │       │   ├── PlanNotFoundException
│   │   │       │   └── ResourceNotFoundException
│   │   │       ├── jwt/                 # JWT(JSON Web Token) 관련 유틸리티 및 필터
│   │   │       │   ├── JWTFilter        # JWT 인증 필터
│   │   │       │   ├── JWTUtil          # JWT 토큰 생성, 검증 유틸
│   │   │       │   ├── LoginCheckFilter
│   │   │       │   └── LoginFilter
│   │   │       ├── redis/               # Redis 관련 유틸리티
│   │   │       │   └── RedisUtil
│   │   │       ├── repository/          # JPA Repository 인터페이스 (DB 접근)
│   │   │       │   ├── PlanRepository   # Plan 엔티티 관련 DB 접근
│   │   │       │   └── UserRepository   # User 엔티티 관련 DB 접근
│   │   │       ├── service/             # 비즈니스 로직 처리
│   │   │       │   ├── CustomUserDetailsService # 사용자 인증 서비스
│   │   │       │   ├── PlanService      # 일정 관련 비즈니스 로직
│   │   │       │   └── UserService      # 사용자 관련 비즈니스 로직
│   │   │       └── PlannerApplication.java # Spring Boot 메인 애플리케이션 클래스
│   │   └── resources/           # 애플리케이션 리소스 파일
│   │       └── application.yml  # Spring Boot 설정 파일 (DB 연결, JWT Secret 등)
│   └── test/                    # 테스트 코드 (JUnit 등)
├── target/                      # 빌드 결과물 (컴파일된 클래스, .jar 파일 등)
├── .gitattributes
├── .gitignore                   # Git 버전 관리에서 제외할 파일 목록
├── mvnw                         # Maven Wrapper 스크립트 (Linux/macOS)
├── mvnw.cmd                     # Maven Wrapper 스크립트 (Windows)
└── pom.xml                      # Maven 프로젝트 설정 파일 (의존성, 빌드 설정 등)




