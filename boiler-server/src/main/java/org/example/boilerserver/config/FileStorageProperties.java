package org.example.boilerserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.file-storage")
public class FileStorageProperties {
    private String sellerQualificationDir = "uploads/seller-qualification";

    public String getSellerQualificationDir() {
        return sellerQualificationDir;
    }

    public void setSellerQualificationDir(String sellerQualificationDir) {
        this.sellerQualificationDir = sellerQualificationDir;
    }
}
