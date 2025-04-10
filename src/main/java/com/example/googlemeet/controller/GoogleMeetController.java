package com.example.googlemeet.controller;
import com.example.googlemeet.dto.MeetingRequest;
import com.example.googlemeet.dto.User;
import com.example.googlemeet.repository.MeetingRequestRepository;
import com.example.googlemeet.repository.UserRepository;
import com.example.googlemeet.service.GoogleMeetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/master/google-meet")
@RequiredArgsConstructor
public class GoogleMeetController {
    private final GoogleMeetService googleMeetService;
    private final MeetingRequestRepository meetingRequestRepository;
    private final UserRepository userRepository;
    /**
     * Handles the OAuth callback, stores the access token, and returns it.
     */
    @GetMapping("/callback")
    public ResponseEntity<Map<String, String>> handleOAuthCallback(@RequestParam("code") String code) {
        try {
            Map<String,String> accessToken = googleMeetService.storeAccessToken(code);
            String email=accessToken.get("email");
            Optional<MeetingRequest> optMeeting = meetingRequestRepository
                    .findTopByOrganiserAndStatusOrderByCreatedAtDesc(email, "PENDING");

            if (optMeeting.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No pending meeting found"));
            }
            MeetingRequest meeting = optMeeting.get();
            try {
                String meetingLink = googleMeetService.createMeetingAndSendInvites(meeting);
                meeting.setMeetingLink(meetingLink);
                meeting.setStatus("CREATED");
                meetingRequestRepository.save(meeting);
                return ResponseEntity.ok(Map.of("meetingLink", meetingLink));
            } catch (Exception e) {
                meetingRequestRepository.delete(meeting);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Meeting creation failed"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Error retrieving access token."));
        }
    }

    @PostMapping("/create-meeting")
    public ResponseEntity<Map<String,String>> createMeeting(@RequestBody MeetingRequest meetingRequest) throws Exception {
        MeetingRequest savedRequest = new MeetingRequest();
        savedRequest.setDescription(meetingRequest.getDescription());
        savedRequest.setOrganiser(meetingRequest.getOrganiser());
        savedRequest.setAttendees( meetingRequest.getAttendees());
        savedRequest.setStartTime(meetingRequest.getStartTime());
        savedRequest.setEndTime(meetingRequest.getEndTime());
        savedRequest.setStatus("PENDING");
        meetingRequestRepository.save(savedRequest);
        User user = userRepository.findByEmail(meetingRequest.getOrganiser());

        if (user == null) {
            Map<Object, Object> authDetails = googleMeetService.getAuthDetails();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("Please Re-Authenticate", authDetails.get("authUrl").toString()));
        }

        try {
            String meetingLink = googleMeetService.createMeetingAndSendInvites(meetingRequest);
            savedRequest.setMeetingLink(meetingLink);
            savedRequest.setStatus("CREATED");
            meetingRequestRepository.save(savedRequest);
            return ResponseEntity.ok(Map.of("meetingLink",meetingLink));
        } catch (Exception e) {
            meetingRequestRepository.delete(savedRequest);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("Please Re-Authenticate",e.getMessage()));
        }
    }

}
