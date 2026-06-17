package org.example.boilerserver.controller;

import org.example.boilercommon.Result;
import org.example.boilerpojo.AdminUserQueryDTO;
import org.example.boilerpojo.AdminUserUpdateDTO;
import org.example.boilerpojo.SellerProfileDTO;
import org.example.boilerpojo.SellerQualificationAuditDTO;
import org.example.boilerpojo.UserDTO;
import org.example.boilerpojo.UserProfileUpdateDTO;
import org.example.boilerpojo.UserRegisterDTO;
import org.example.boilerpojo.UserVO;
import org.example.boilerserver.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<UserVO> register(@RequestBody UserRegisterDTO dto) {
        return Result.success(userService.register(dto));
    }

    @PostMapping("/login")
    public Result<UserVO> login(@RequestBody UserDTO dto) {
        return Result.success(userService.login(dto));
    }

    @GetMapping("/profile/{userId}")
    public Result<UserVO> getProfile(@PathVariable String userId) {
        return Result.success(userService.getProfile(userId));
    }

    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@RequestBody UserProfileUpdateDTO dto) {
        return Result.success(userService.updateProfile(dto));
    }

    @GetMapping("/seller-profile/{userId}")
    public Result<UserVO> getSellerProfile(@PathVariable String userId) {
        return Result.success(userService.getSellerProfile(userId));
    }

    @PutMapping("/seller-profile")
    public Result<UserVO> upsertSellerProfile(@RequestBody SellerProfileDTO dto) {
        return Result.success(userService.upsertSellerProfile(dto));
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
        return Result.success(userService.auditSellerQualification(dto));
    }
}
