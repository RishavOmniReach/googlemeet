package com.example.googlemeet.dto;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import java.util.Date;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity(name="user_token")
public class User{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NonNull
    private String email;
    private String accessToken;
    private String refreshToken;
    private Date lastUpdated;

    public User(String email, String accessToken, String refreshToken,Date lastUpdated) {
        this.email = email;
        this.accessToken = accessToken;
        this.refreshToken=refreshToken;
        this.lastUpdated = lastUpdated;
    }
}
