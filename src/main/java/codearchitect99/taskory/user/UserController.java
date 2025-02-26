package codearchitect99.taskory.user;

import codearchitect99.taskory.security.model.UserPrincipal;
import codearchitect99.taskory.user.exception.UsernameAlreadyExistsException;
import codearchitect99.taskory.user.payload.ProfileUpdateRequest;
import codearchitect99.taskory.user.payload.UserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("${app.url-base}/user")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(@CurrentUser UserPrincipal userPrincipal) {
        log.info("=====[LOG] UserController.getProfile()=====");
        UserResponse response = userService.getByUsername(userPrincipal.getUsername());
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/check-username")
    public ResponseEntity<Boolean> checkUsername(@RequestParam("username") String username) {
        boolean isAvailable = userService.isUsernameAvailable(username);
        return ResponseEntity.ok().body(isAvailable);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@CurrentUser UserPrincipal userPrincipal, @RequestBody ProfileUpdateRequest request) {
        try {
            UserResponse response = userService.updateProfile(userPrincipal.getId(), request);
            return ResponseEntity.ok().body(response);
        } catch (UsernameAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
}
