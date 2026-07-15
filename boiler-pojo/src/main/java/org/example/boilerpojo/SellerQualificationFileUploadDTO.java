package org.example.boilerpojo;

import lombok.Data;

@Data
public class SellerQualificationFileUploadDTO {
    private String userId;
    private String fileType;
    private String fileName;
    private String fileUrl;
}
