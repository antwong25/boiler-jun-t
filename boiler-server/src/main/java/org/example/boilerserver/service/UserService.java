package org.example.boilerserver.service;

import org.example.boilerpojo.AdminUserQueryDTO;
import org.example.boilerpojo.AdminUserUpdateDTO;
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

    UserVO updateProfile(UserProfileUpdateDTO dto);

    UserVO getSellerProfile(String userId);

    UserVO upsertSellerProfile(SellerProfileDTO dto);

    SellerQualificationFileUploadDTO uploadSellerQualificationFile(String userId, String fileType, MultipartFile file);

    List<UserVO> adminListUsers(AdminUserQueryDTO dto);

    UserVO adminGetUserDetail(String userId);

    UserVO adminUpdateUser(AdminUserUpdateDTO dto);

    UserVO auditSellerQualification(SellerQualificationAuditDTO dto);

    boolean isAdmin(String userId);
}
