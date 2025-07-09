package SK_3team.example.planner.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// @ResponseStatus를 사용하여 이 예외가 발생하면 HTTP 404 NOT_FOUND를 반환하도록 설정
@ResponseStatus(HttpStatus.NOT_FOUND)
public class PlanNotFoundException extends RuntimeException {
    public PlanNotFoundException(String message) {
        super(message);
    }
}