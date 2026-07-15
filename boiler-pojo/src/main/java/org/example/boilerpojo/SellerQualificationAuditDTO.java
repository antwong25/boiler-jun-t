package org.example.boilerpojo;

import lombok.Data;

@Data
public class SellerQualificationAuditDTO {
    private String sellerId;
    private String targetStatus;
    private String auditRemark;
    private String adminUserId;
}
