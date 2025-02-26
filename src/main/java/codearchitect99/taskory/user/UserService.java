package codearchitect99.taskory.user;

import codearchitect99.taskory.event.EventRepository;
import codearchitect99.taskory.hashtag.HashtagRepository;
import codearchitect99.taskory.routine.repository.RoutineHistoryRepository;
import codearchitect99.taskory.routine.repository.RoutineRepository;
import codearchitect99.taskory.security.model.OAuth2UserInfo;
import codearchitect99.taskory.tag.TagRepository;
import codearchitect99.taskory.tag.model.Color;
import codearchitect99.taskory.tag.model.Tag;
import codearchitect99.taskory.task.service.TaskService;
import codearchitect99.taskory.user.exception.UsernameAlreadyExistsException;
import codearchitect99.taskory.user.model.Role;
import codearchitect99.taskory.user.model.SocialAccount;
import codearchitect99.taskory.user.model.User;
import codearchitect99.taskory.user.payload.ProfileUpdateRequest;
import codearchitect99.taskory.user.payload.UserResponse;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

/**
 * Service for manage user info and user crud
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final TagRepository tagRepository;
    private final TaskService taskService;
    private final RoutineRepository routineRepository;
    private final RoutineHistoryRepository routineHistoryRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final HashtagRepository hashtagRepository;

    @Autowired
    public UserService(UserRepository userRepository, EventRepository eventRepository, TagRepository tagRepository, TaskService taskService, RoutineRepository routineRepository, RoutineHistoryRepository routineHistoryRepository, SocialAccountRepository socialAccountRepository, HashtagRepository hashtagRepository) {
        this.userRepository = userRepository;
        this.taskService = taskService;
        this.eventRepository = eventRepository;
        this.tagRepository = tagRepository;
        this.routineRepository = routineRepository;
        this.routineHistoryRepository = routineHistoryRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.hashtagRepository = hashtagRepository;
    }

    /**
     * Get user by username
     * @param username Username
     * @return UserResponse
     */
    public UserResponse getByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));
        return new UserResponse(user);
    }

    /**
     * Update user profile
     * @param userId User id
     * @param profileUpdateRequest Information for profile update
     * @return UserResponse
     */
    public UserResponse updateProfile(Long userId, ProfileUpdateRequest profileUpdateRequest) throws UsernameAlreadyExistsException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (userRepository.existsByUsername(profileUpdateRequest.getUsername()) && !Objects.equals(profileUpdateRequest.getUsername(), user.getUsername())) {
                throw new UsernameAlreadyExistsException();
        }

        user.updateProfile(profileUpdateRequest);

        for (Role role : user.getRoles()) {
            if (role == Role.TEMP_USER) user.upgradeToOfficial();
        }

        User updatedUser = userRepository.save(user);
        return new UserResponse(updatedUser);
    }

    /**
     * Delete user
     * @param id User id for delete
     */
    @Transactional
    public void deleteById(Long id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isEmpty()) {
            throw new UsernameNotFoundException("User not found");
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public User registerTempUser(OAuth2UserInfo oAuth2UserInfo, String socialProvider) {
        User user = User.builder()
                .username(oAuth2UserInfo.getEmail())
                .roles(Collections.singletonList(Role.TEMP_USER))
                .build();
        userRepository.save(user);

        SocialAccount socialAccount = SocialAccount.builder()
                .user(user)
                .subId(oAuth2UserInfo.getSubId())
                .username(oAuth2UserInfo.getEmail())
                .provider(socialProvider)
                .build();
        socialAccountRepository.save(socialAccount);


        // create default tag
        tagRepository.save(
                Tag.builder()
                        .user(user)
                        .title("Default")
                        .color(Color.BLUE)
                        .build()
        );

        return user;
    }

    public boolean isUsernameAvailable(String username) {
        if (userRepository.existsByUsername(username)) {
            return false;
        } else return true;
    }
}
