package com.example.googlemeet.service;
import com.example.googlemeet.dto.MeetingRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import com.google.apps.meet.v2.CreateSpaceRequest;
import com.google.apps.meet.v2.Space;
import com.google.apps.meet.v2.SpacesServiceClient;
import com.google.apps.meet.v2.SpacesServiceSettings;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.ClientId;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserAuthorizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.mail.javamail.JavaMailSender;


import java.net.URI;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleMeetService {

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${google.oauth.client-secret}")
    private String clientSecret;

    @Value("${google.oauth.redirect-uri}")
    private String redirectUri;

    @Value("${google.oauth.token-url}")
    private String tokenUrl;

    private final RestTemplate restTemplate;
    private final JavaMailSender mailSender;
    private final Map<String, String> tokenStorage = new HashMap<>();

    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    /**
     * Exchanges an authorization code for an access token.
     */
    private String getAccessToken(String code) throws Exception {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, requestEntity, String.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            JsonNode jsonNode = new ObjectMapper().readTree(response.getBody());
            String accessToken = jsonNode.path("access_token").asText();
            tokenStorage.put("accessToken", accessToken);
            return accessToken;
        } else {
            throw new Exception("Failed to retrieve access token. Response: " + response.getBody());
        }
    }

    /**
     * Generates the OAuth authorization URL.
     */
    public Map<String, String> getAuthDetails() throws Exception {
        UserAuthorizer authorizer = UserAuthorizer.newBuilder()
                .setClientId(ClientId.newBuilder().setClientId(clientId).build())
                .setCallbackUri(URI.create(redirectUri))
                .setScopes(Arrays.asList(
                        "https://www.googleapis.com/auth/meetings.space.created",
                        "https://www.googleapis.com/auth/meetings",
                        "https://www.googleapis.com/auth/calendar",
                        "https://www.googleapis.com/auth/userinfo.email"
                ))
                .build();

        String state = UUID.randomUUID().toString();
        String authorizationUrl = authorizer.getAuthorizationUrl("user", state, null).toString();

        log.info("Generated Auth URL: {}", authorizationUrl);

        return Map.of("authUrl", authorizationUrl);
    }

    /**
     * Exchanges the code for an access token and stores it.
     */
    public String storeAccessToken(String code) throws Exception {
        String accessToken = getAccessToken(code);
        tokenStorage.put("accessToken", accessToken);
        return accessToken;
    }

    public String storeCode(String code) throws Exception {
        tokenStorage.put("code", code);
        return code;
    }

    public String getCode() {
        return tokenStorage.get("code");
    }

    /**
     * Retrieves the stored access token.
     */
    public String getStoredAccessToken() {
        return tokenStorage.get("accessToken");
    }

    /**
     * Retrieves the stored access token or fetches a new one.
     */
    private String getValidAccessToken(String code) throws Exception {
        String accessToken = tokenStorage.get("accessToken");
        if (StringUtils.isEmpty(accessToken)) {
            log.info("No stored access token, fetching new token...");
            return getAccessToken(code);
        }
        return accessToken;
    }

    /**
     * Creates a Google Meet meeting and sends Calendar invites.
     */
    public String createMeetingAndSendInvites(MeetingRequest meetingRequest) throws Exception {
        String code = getCode();
        String accessToken = getValidAccessToken(code);
        if (StringUtils.isEmpty(accessToken)) {
            log.info("Fetching new access token...");
            accessToken = getAccessToken(code);
        }

        log.info("Using access token: {}", accessToken);

        GoogleCredentials credentials = GoogleCredentials.create(new AccessToken(accessToken, null));

        SpacesServiceSettings settings = SpacesServiceSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();

        try (SpacesServiceClient spacesServiceClient = SpacesServiceClient.create(settings)) {
            CreateSpaceRequest request = CreateSpaceRequest.newBuilder()
                    .setSpace(Space.newBuilder().build())
                    .build();


            Space space = spacesServiceClient.createSpace(request);
            if (space == null || space.getMeetingUri() == null) {
                throw new Exception("Failed to create meeting. Response from Google Meet API is null.");
            }

            String meetingLink = space.getMeetingUri();
            sendCalendarInvites(meetingRequest, meetingLink, accessToken);

            return meetingLink;
        }catch (Exception e) {
            log.error("Error creating Google Meet meeting.", e);
            throw new Exception("Error creating Google Meet meeting: " + e.getMessage());
        }
    }

    /**
     * Sends Calendar invites instead of email.
     */
    private void sendCalendarInvites(MeetingRequest meetingRequest, String meetingLink, String accessToken) throws Exception {
        GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);
        Calendar service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName("Google Meet Integration")
                .build();

        Event event = new Event()
                .setSummary(meetingRequest.getDescription())
                .setLocation(meetingLink)
                .setDescription("Google Meet Meeting: " + meetingLink);
        EventDateTime start = new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(meetingRequest.getStartTime()));
        EventDateTime end = new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(meetingRequest.getEndTime()));
        event.setStart(start);
        event.setEnd(end);

//       Add attendees
        List<EventAttendee> eventAttendees = new ArrayList<>();
        for (String attendee : meetingRequest.getAttendees()) {
            eventAttendees.add(new EventAttendee().setEmail(attendee));
        }
        event.setAttendees(eventAttendees);

        // Set conference details
        ConferenceSolutionKey conferenceSolutionKey = new ConferenceSolutionKey().setType("hangoutsMeet");
        CreateConferenceRequest createConferenceRequest = new CreateConferenceRequest()
                .setRequestId(UUID.randomUUID().toString())
                .setConferenceSolutionKey(conferenceSolutionKey);
        ConferenceData conferenceData = new ConferenceData().setCreateRequest(createConferenceRequest);
        event.setConferenceData(conferenceData);

        // Insert event into calendar
        String calendarId = "primary"; // User's primary calendar
        Event createdEvent = service.events().insert(calendarId, event)
                .setConferenceDataVersion(1)
                .setSendUpdates("all")
                .execute();

        log.info("Created Calendar Event: {}", createdEvent.getHtmlLink());
    }

}
