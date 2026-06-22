package org.example.boilerpojo;

import lombok.Data;

@Data
public class LoginVO {
    private String token;
    private String tokenType;
    private Long expiresInSeconds;
    private UserVO userInfo;
}
