package org.example.boilerserver.controller;

import org.example.boilercommon.Result;
import org.example.boilerpojo.AdminUserQueryDTO;
import org.example.boilerpojo.AdminUserUpdateDTO;
import org.example.boilerpojo.LoginVO;
import org.example.boilerpojo.SellerQualificationFileUploadDTO;
import org.example.boilerpojo.SellerProfileDTO;
import org.example.boilerpojo.SellerQualificationAuditDTO;
import org.example.boilerpojo.UserDTO;
import org.example.boilerpojo.UserProfileUpdateDTO;
import org.example.boilerpojo.UserRegisterDTO;
import org.example.boilerpojo.UserVO;
import org.example.boilerserver.auth.AuthContext;
import org.example.boilerserver.auth.JwtTokenProvider;
import org.example.boilerserver.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    public UserController(UserService userService, JwtTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/register")
    public Result<UserVO> register(@RequestBody UserRegisterDTO dto) {
        return Result.success(userService.register(dto));
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody UserDTO dto) {
        UserVO userVO = userService.login(dto);
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(jwtTokenProvider.generateToken(userVO.getUserId(), userVO.getUserType()));
        loginVO.setTokenType("Bearer");
        loginVO.setExpiresInSeconds(jwtTokenProvider.getExpirationMinutes() * 60);
        loginVO.setUserInfo(userVO);
        return Result.success(loginVO);
    }

    @GetMapping("/profile/{userId}")
    public Result<UserVO> getProfile(@PathVariable String userId) {
        return Result.success(userService.getProfile(userId));
    }

    @GetMapping("/profile/me")
    public Result<UserVO> getCurrentProfile() {
        return Result.success(userService.getProfile(AuthContext.getRequiredUserId()));
    }

    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@RequestBody UserProfileUpdateDTO dto) {
        return Result.success(userService.updateProfile(AuthContext.getRequiredUserId(), dto));
    }

    @GetMapping("/seller-profile/{userId}")
    public Result<UserVO> getSellerProfile(@PathVariable String userId) {
        return Result.success(userService.getSellerProfile(userId));
    }

    @GetMapping("/seller-profile/me")
    public Result<UserVO> getCurrentSellerProfile() {
        return Result.success(userService.getSellerProfile(AuthContext.getRequiredUserId()));
    }

    @PutMapping("/seller-profile")
    public Result<UserVO> upsertSellerProfile(@RequestBody SellerProfileDTO dto) {
        return Result.success(userService.upsertSellerProfile(AuthContext.getRequiredUserId(), dto));
    }

    @PostMapping("/seller-profile/files")
    public Result<SellerQualificationFileUploadDTO> uploadSellerQualificationFile(
            @RequestParam String fileType,
            @RequestParam("file") MultipartFile file
    ) {
        return Result.success(userService.uploadSellerQualificationFile(AuthContext.getRequiredUserId(), fileType, file));
    }

    @GetMapping("/admin/users")
    public Result<List<UserVO>> adminListUsers(AdminUserQueryDTO dto) {
        return Result.success(userService.adminListUsers(dto));
    }

    @GetMapping("/admin/users/{userId}")
    public Result<UserVO> adminGetUserDetail(@PathVariable String userId) {
        return Result.success(userService.adminGetUserDetail(userId));
    }

    @PutMapping("/admin/users")
    public Result<UserVO> adminUpdateUser(@RequestBody AdminUserUpdateDTO dto) {
        return Result.success(userService.adminUpdateUser(dto));
    }

    @PutMapping("/admin/sellers/qualification")
    public Result<UserVO> auditSellerQualification(@RequestBody SellerQualificationAuditDTO dto) {
        return Result.success(userService.auditSellerQualification(AuthContext.getRequiredUserId(), dto));
    }
}
