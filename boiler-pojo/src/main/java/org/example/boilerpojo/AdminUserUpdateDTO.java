package org.example.boilerpojo;

import lombok.Data;

@Data
public class AdminUserUpdateDTO {
    private String userId;
    private String phone;
    private String email;
    private Integer creditScore;
    private String verificationStatus;
    private String qualificationStatus;
}
