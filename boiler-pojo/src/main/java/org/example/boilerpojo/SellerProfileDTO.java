package org.example.boilerpojo;

import lombok.Data;

@Data
public class SellerProfileDTO {
    private String userId;
    private String shopName;
    private String shopAddress;
    private String businessLicense;
    private String legalPersonId;
    private String businessLicenseFileUrl;
    private String legalPersonIdFileUrl;
}
