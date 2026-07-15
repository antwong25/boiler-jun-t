package org.example.boilerserver.service;

import org.example.boilerpojo.AdminUserQueryDTO;
import org.example.boilerpojo.AdminUserUpdateDTO;
import org.example.boilerpojo.CreditScoreRecalculateVO;
import org.example.boilerpojo.SellerQualificationFileUploadDTO;
import org.example.boilerpojo.SellerProfileDTO;
import org.example.boilerpojo.SellerQualificationAuditDTO;
import org.example.boilerpojo.UserDTO;
import org.example.boilerpojo.UserProfileUpdateDTO;
import org.example.boilerpojo.UserRegisterDTO;
import org.example.boilerpojo.UserVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {
    UserVO register(UserRegisterDTO dto);

    UserVO login(UserDTO dto);

    UserVO getProfile(String userId);

    UserVO updateProfile(String currentUserId, UserProfileUpdateDTO dto);

    UserVO getSellerProfile(String userId);

    UserVO upsertSellerProfile(String currentUserId, SellerProfileDTO dto);

    SellerQualificationFileUploadDTO uploadSellerQualificationFile(String currentUserId, String fileType, MultipartFile file);

    List<UserVO> adminListUsers(AdminUserQueryDTO dto);

    UserVO adminGetUserDetail(String userId);

    UserVO adminUpdateUser(AdminUserUpdateDTO dto);

    UserVO recalculateCreditScore(String userId);

    CreditScoreRecalculateVO recalculateAllCreditScores();

    UserVO auditSellerQualification(String adminUserId, SellerQualificationAuditDTO dto);

    boolean isAdmin(String userId);
}
