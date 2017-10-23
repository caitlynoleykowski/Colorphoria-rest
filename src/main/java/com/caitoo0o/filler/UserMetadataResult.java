package com.caitoo0o.filler;

/**
 * Created by caitoo0o on 10/22/2017.
 */
public class UserMetadataResult {
    private String userId;
    private String email;
    private String fullName;
    private String username;
    private String ageOnSignupDate;
    private String signupDate;

    public UserMetadataResult(String userId, String email, String fullName, String username,
                                   String ageOnSignupDate, String signupDate) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.username = username;
        this.ageOnSignupDate = ageOnSignupDate;
        this.signupDate = signupDate;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getUsername() {
        return username;
    }

    public String getAgeOnSignupDate() {
        return ageOnSignupDate;
    }

    public String getSignupDate() {
        return signupDate;
    }
}
