package com.example.googlemeet.dto;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Data
@Getter
@Setter
@NoArgsConstructor
public class MeetingRequest {

    private String summary;
    private String description;
    private String startTime;
    private String endTime;
    private List<String> attendees;

    public String getSummary() { return summary; }
    public String getDescription() { return description; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public List<String> getAttendees() { return attendees; }
}