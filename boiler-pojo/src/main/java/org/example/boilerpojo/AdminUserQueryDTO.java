package org.example.boilerpojo;

import lombok.Data;

@Data
public class AdminUserQueryDTO {
    private String username;
    private String userType;
    private String verificationStatus;
    private String qualificationStatus;
}
