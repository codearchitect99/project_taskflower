package taskflower.taskflower.security;

import lombok.Data;

@Data
public class SignupResponse {
    public SignupResponse(String message) {
        this.message = message;
    }

    private String message;
}
