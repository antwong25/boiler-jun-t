package org.example.boilerpojo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SellerEntity {
    private String sellerId;
    private String shopName;
    private String shopAddress;
    private String businessLicense;
    private String legalPersonId;
    private String businessLicenseFileUrl;
    private String legalPersonIdFileUrl;
    private String qualificationStatus;
    private String qualificationAuditRemark;
    private String qualificationAuditedBy;
    private java.time.LocalDateTime qualificationAuditTime;
    private BigDecimal guaranteeDeposit;
    private Integer completedTransactionCount;
    private BigDecimal positiveRatingRate;
}
