package com.example.googlemeet.controller;
import com.example.googlemeet.dto.MeetingRequest;
import com.example.googlemeet.service.GoogleMeetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/google-meet")
@RequiredArgsConstructor
public class GoogleMeetController {

    private final GoogleMeetService googleMeetService;

    @PostMapping("/create")
    public ResponseEntity<String> createMeet(@RequestBody MeetingRequest request) {
        try {
            String meetLink = googleMeetService.createGoogleMeet(
                    request.getSummary(), request.getDescription(),
                    request.getStartTime(), request.getEndTime(), request.getAttendees());
            return ResponseEntity.ok(meetLink);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error creating meeting: " + e.getMessage());
        }
    }
}

