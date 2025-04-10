package com.example.googlemeet.dto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "meeting_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MeetingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String description;
    private String startTime;
    private String endTime;
    private String organiser;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "meeting_attendees", joinColumns = @JoinColumn(name = "meeting_attendees"))
    @Column(name = "attendee_email")
    private List<String> attendees;
    private String status; // PENDING, CREATED, FAILED
    private String meetingLink;
    private LocalDateTime createdAt = LocalDateTime.now();
}
