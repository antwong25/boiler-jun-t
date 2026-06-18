package org.example.boilerpojo;

import lombok.Data;

@Data
public class UserRegisterDTO {
    private String username;
    private String password;
    private String phone;
    private String email;
    private String userType;
    private String shippingAddress;
    private String shopName;
    private String businessLicense;
    private String legalPersonId;
    private String shopAddress;
}
