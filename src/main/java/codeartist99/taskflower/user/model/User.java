package codeartist99.taskflower.user.model;

import codeartist99.taskflower.common.Timezone;
import codeartist99.taskflower.user.payload.ProfileUpdateRequest;
import jakarta.persistence.*;
import lombok.*;

import java.util.*;

@Entity(name = "User")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;
    @Column(name = "username", nullable = false, unique = true)
    private String username;
    @Enumerated(EnumType.STRING)  // Use the Timezone enum as String in the database
    @Column(name = "timezone", nullable = false)
    private Timezone timezone;
    @OneToMany
    private Set<SocialAccount> socialAccounts;
    @Column(name = "roles", nullable = false)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "User_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(value = EnumType.STRING)
    private List<Role> roles = new ArrayList<>();

    public void updateProfile(ProfileUpdateRequest profileUpdateRequest) {
        this.username = profileUpdateRequest.getUsername();
        this.timezone = Timezone.fromString(profileUpdateRequest.getTimezone());
    }

    public void upgradeToOfficial() {
        this.roles = new ArrayList<>(Collections.singletonList(Role.USER));
    }
}
