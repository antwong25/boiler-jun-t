package org.example.boilerpojo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SellerEntity {
    private String sellerId;
    private String shopName;
    private String businessLicense;
    private String legalPersonId;
    private String qualificationStatus;
    private BigDecimal guaranteeDeposit;
    private Integer completedTransactionCount;
    private BigDecimal positiveRatingRate;
}
