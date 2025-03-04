package com.example.googlemeet.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class GoogleMeetService {
    private static final String APPLICATION_NAME = "Google Meet Integration";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final List<String> SCOPES = List.of("https://www.googleapis.com/auth/calendar.events");
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    private static Credential getCredentials(final HttpTransport httpTransport) throws IOException {
        InputStream in = GoogleMeetService.class.getResourceAsStream("/credentials.json");
        if (in == null) {
            throw new FileNotFoundException("Resource not found: credentials.json");
        }

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        return flow.loadCredential("user");
    }

    public String createGoogleMeet(String summary, String description, String startTime, String endTime, List<String> attendeesEmails)
            throws IOException, GeneralSecurityException {

        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = getCredentials(httpTransport);

        Calendar service = new Calendar.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        Event event = new Event()
                .setSummary(summary)
                .setDescription(description);

        EventDateTime start = new EventDateTime()
                .setDateTime(new DateTime(startTime))
                .setTimeZone("Asia/Kolkata");
        event.setStart(start);

        EventDateTime end = new EventDateTime()
                .setDateTime(new DateTime(endTime))
                .setTimeZone("Asia/Kolkata");
        event.setEnd(end);

        ConferenceSolutionKey conferenceKey = new ConferenceSolutionKey().setType("hangoutsMeet");
        CreateConferenceRequest request = new CreateConferenceRequest()
                .setRequestId("random-id")
                .setConferenceSolutionKey(conferenceKey);
        event.setConferenceData(new ConferenceData().setCreateRequest(request));

        EventAttendee[] attendees = attendeesEmails.stream()
                .map(email -> new EventAttendee().setEmail(email).setResponseStatus("needsAction"))
                .toArray(EventAttendee[]::new);
        event.setAttendees(Arrays.asList(attendees));

        Event createdEvent = service.events()
                .insert("primary", event)
                .setConferenceDataVersion(1)
                .setSendUpdates("all")
                .execute();

        log.info("✅ Google Meet Created: {}", createdEvent.getHangoutLink());
        return createdEvent.getHangoutLink();
    }
}
