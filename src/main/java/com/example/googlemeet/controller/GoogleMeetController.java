package com.example.googlemeet.controller;
import com.example.googlemeet.dto.MeetingRequest;
import com.example.googlemeet.service.GoogleMeetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/master/google-meet")
@RequiredArgsConstructor
public class GoogleMeetController {
    private final GoogleMeetService googleMeetService;
    private final RestTemplate restTemplate;

    /**
     * Generates an authorization URL for Google OAuth.
     */
    @GetMapping("/auth")
    public ResponseEntity<Map<Object, Object>> getAuthDetails() throws Exception {
        Map<Object, Object> authDetails = googleMeetService.getAuthDetails();
        return ResponseEntity.ok(authDetails);
    }

    /**
     * Handles the OAuth callback, stores the access token, and returns it.
     */
    @GetMapping("/callback")
    public ResponseEntity<Map<String, String>> handleOAuthCallback(@RequestParam("code") String code) {
        try {
            String accessToken = googleMeetService.storeAccessToken(code);
            Map<String,String> res= new HashMap<>();
            res.put("message",accessToken);
            return ResponseEntity.ok(Map.of("accessToken", accessToken));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Error retrieving access token."));
        }
    }

    @PostMapping("/create-meeting")
    public ResponseEntity<Map<String,String>> createMeeting(@RequestBody MeetingRequest meetingRequest) {
        try {
            String meetingLink = googleMeetService.createMeetingAndSendInvites(meetingRequest);
            return ResponseEntity.ok(Map.of("meetingLink",meetingLink));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("Please Re-Authenticate",e.getMessage()));
        }
    }

}
