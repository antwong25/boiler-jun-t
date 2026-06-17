package org.example.boilerpojo;

import lombok.Data;

@Data
public class UserProfileUpdateDTO {
    private String userId;
    private String phone;
    private String email;
    private String shippingAddress;
}
