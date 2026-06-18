package org.example.boilerserver.service;

import org.example.boilerpojo.AdminUserQueryDTO;
import org.example.boilerpojo.AdminUserUpdateDTO;
import org.example.boilerpojo.SellerProfileDTO;
import org.example.boilerpojo.SellerQualificationAuditDTO;
import org.example.boilerpojo.UserDTO;
import org.example.boilerpojo.UserProfileUpdateDTO;
import org.example.boilerpojo.UserRegisterDTO;
import org.example.boilerpojo.UserVO;

import java.util.List;

public interface UserService {
    UserVO register(UserRegisterDTO dto);

    UserVO login(UserDTO dto);

    UserVO getProfile(String userId);

    UserVO updateProfile(UserProfileUpdateDTO dto);

    UserVO getSellerProfile(String userId);

    UserVO upsertSellerProfile(SellerProfileDTO dto);

    List<UserVO> adminListUsers(AdminUserQueryDTO dto);

    UserVO adminGetUserDetail(String userId);

    UserVO adminUpdateUser(AdminUserUpdateDTO dto);

    UserVO auditSellerQualification(SellerQualificationAuditDTO dto);

    boolean isAdmin(String userId);
}
