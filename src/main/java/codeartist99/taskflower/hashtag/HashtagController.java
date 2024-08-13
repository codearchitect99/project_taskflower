package codeartist99.taskflower.hashtag;

import codeartist99.taskflower.user.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${app.url-base}/hashtags")
public class HashtagController {

    private final HashtagService hashtagService;

    @Autowired
    public HashtagController(HashtagService hashtagService) {
        this.hashtagService = hashtagService;
    }

    /**
     * Create a new hashtag
     * @param user Authenticated user
     * @param saveHashtagRequest Request body containing hashtag details
     * @return ResponseEntity with HashtagResponse and HTTP status
     */
    @PostMapping
    public ResponseEntity<HashtagResponse> createHashtag(@AuthenticationPrincipal User user,
                                                         @RequestBody SaveHashtagRequest saveHashtagRequest) {
        HashtagResponse hashtagResponse = hashtagService.save(user, saveHashtagRequest);
        return new ResponseEntity<>(hashtagResponse, HttpStatus.CREATED);
    }

    /**
     * Get a hashtag by its ID
     * @param id Hashtag ID
     * @return ResponseEntity with HashtagResponse and HTTP status
     */
    @GetMapping("/{id}")
    public ResponseEntity<HashtagResponse> getHashtagById(@PathVariable Long id) {
        try {
            HashtagResponse hashtagResponse = hashtagService.getById(id);
            return new ResponseEntity<>(hashtagResponse, HttpStatus.OK);
        } catch (HashtagNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Get all hashtags for a user
     * @param user Authenticated user
     * @return ResponseEntity with List of HashtagResponse and HTTP status
     */
    @GetMapping
    public ResponseEntity<List<HashtagResponse>> getAllHashtags(@AuthenticationPrincipal User user) {
        List<HashtagResponse> hashtagResponses = hashtagService.findAll(user);
        return new ResponseEntity<>(hashtagResponses, HttpStatus.OK);
    }

    /**
     * Update a hashtag by its ID
     * @param id Hashtag ID
     * @param saveHashtagRequest Request body containing updated hashtag details
     * @return ResponseEntity with HashtagResponse and HTTP status
     */
    @PutMapping("/{id}")
    public ResponseEntity<HashtagResponse> updateHashtag(@PathVariable Long id,
                                                         @RequestBody SaveHashtagRequest saveHashtagRequest) {
        try {
            HashtagResponse hashtagResponse = hashtagService.updateHashtag(id, saveHashtagRequest);
            return new ResponseEntity<>(hashtagResponse, HttpStatus.OK);
        } catch (HashtagNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Delete a hashtag by its ID
     * @param id Hashtag ID
     * @return ResponseEntity with HTTP status
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHashtag(@PathVariable Long id) {
        try {
            hashtagService.deleteById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (HashtagNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
