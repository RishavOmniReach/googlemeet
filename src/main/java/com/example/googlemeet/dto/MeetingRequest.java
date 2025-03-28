package com.example.googlemeet.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MeetingRequest {
    private String description;
    private String startTime;
    private String endTime;
    private String organiser;
    private String[] attendees;
}
