package com.example.googlemeet.controller;
import com.example.googlemeet.dto.MeetingRequest;
import com.example.googlemeet.service.GoogleMeetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/master/google-meet")
@RequiredArgsConstructor
public class GoogleMeetController {
    private final GoogleMeetService googleMeetService;

    /**
     * Generates an authorization URL for Google OAuth.
     */
    @GetMapping("/auth")
    public ResponseEntity<Map<String, String>> getAuthDetails() throws Exception {
        Map<String, String> authDetails = googleMeetService.getAuthDetails();
        return ResponseEntity.ok(authDetails);
    }

    /**
     * Handles the OAuth callback, stores the access token, and returns it.
     */
    @GetMapping("/callback")
    public ResponseEntity<Map<String, String>> handleOAuthCallback(@RequestParam("code") String code) {
        try {
            String accessToken = googleMeetService.storeAccessToken(code);
            String _code=googleMeetService.storeCode(code);
            Map<String,String> res= new HashMap<>();
            res.put("message",accessToken);
            res.put("code",_code);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Error retrieving access token."));
        }
    }

    /**
     * Fetches the stored access token.
     */
    @GetMapping("/get-token")
    public ResponseEntity<Map<String, String>> getStoredToken() {
        String accessToken = googleMeetService.getStoredAccessToken();
        String code=googleMeetService.getCode();
        if (accessToken != null) {
            Map<String,String> res= new HashMap<>();
            res.put("message",accessToken);
            res.put("code",code);
            return ResponseEntity.ok(res);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "No access token found. Authenticate first."));
        }
    }

    /**
     * Creates a Google Meet meeting using the stored access token.
     */
    @PostMapping("/create-meeting")
    public ResponseEntity<String> createMeeting(@RequestBody MeetingRequest meetingRequest) {
        try {
            String meetingLink = googleMeetService.createMeetingAndSendInvites(meetingRequest);
            return ResponseEntity.ok(meetingLink);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error creating Google Meet meeting.");
        }
    }
}
