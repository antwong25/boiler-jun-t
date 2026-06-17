package org.example.boilerpojo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UserVO{
    private String userId;
    private String username;
    private String phone;
    private String email;
    private String userType;
    private Integer creditScore;
    private LocalDate registrationDate;
    private String verificationStatus;
    private String buyerId;
    private String sellerId;
    private String shopName;
    private String businessLicense;
    private String legalPersonId;
    private String qualificationStatus;
    private BigDecimal guaranteeDeposit;
    private Integer completedTransactionCount;
    private BigDecimal positiveRatingRate;
}
