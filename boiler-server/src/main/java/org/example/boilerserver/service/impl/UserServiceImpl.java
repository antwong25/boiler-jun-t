package org.example.boilerserver.service.impl;

import org.example.constant.UserConstant;
import org.example.boilerpojo.AdminUserQueryDTO;
import org.example.boilerpojo.AdminUserUpdateDTO;
import org.example.boilerpojo.BuyerEntity;
import org.example.boilerpojo.SellerEntity;
import org.example.boilerpojo.SellerQualificationFileUploadDTO;
import org.example.boilerpojo.SellerProfileDTO;
import org.example.boilerpojo.SellerQualificationAuditDTO;
import org.example.boilerpojo.UserDTO;
import org.example.boilerpojo.UserEntity;
import org.example.boilerpojo.UserProfileUpdateDTO;
import org.example.boilerpojo.UserRegisterDTO;
import org.example.boilerpojo.UserVO;
import org.example.boilerserver.config.FileStorageProperties;
import org.example.boilerserver.mapper.BuyerMapper;
import org.example.boilerserver.mapper.ReviewMapper;
import org.example.boilerserver.mapper.SellerMapper;
import org.example.boilerserver.mapper.TransactionMapper;
import org.example.boilerserver.mapper.UserMapper;
import org.example.boilerserver.service.UserService;
import org.example.constant.ReviewConstant;
import org.example.constant.TransactionConstant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {
    private static final Set<String> QUALIFICATION_FILE_TYPES = Set.of("BUSINESS_LICENSE", "LEGAL_PERSON_ID");
    private static final Set<String> ALLOWED_FILE_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".pdf");

    private final UserMapper userMapper;
    private final BuyerMapper buyerMapper;
    private final SellerMapper sellerMapper;
    private final TransactionMapper transactionMapper;
    private final ReviewMapper reviewMapper;
    private final FileStorageProperties fileStorageProperties;

    public UserServiceImpl(
            UserMapper userMapper,
            BuyerMapper buyerMapper,
            SellerMapper sellerMapper,
            TransactionMapper transactionMapper,
            ReviewMapper reviewMapper,
            FileStorageProperties fileStorageProperties
    ) {
        this.userMapper = userMapper;
        this.buyerMapper = buyerMapper;
        this.sellerMapper = sellerMapper;
        this.transactionMapper = transactionMapper;
        this.reviewMapper = reviewMapper;
        this.fileStorageProperties = fileStorageProperties;
    }

    @Override
    @Transactional
    public UserVO register(UserRegisterDTO dto) {
        validateRegisterRequest(dto);
        if (userMapper.countByUsername(dto.getUsername()) > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }

        String userId = UUID.randomUUID().toString().replace("-", "");
        UserEntity userEntity = new UserEntity();
        userEntity.setUserId(userId);
        userEntity.setUsername(dto.getUsername().trim());
        userEntity.setPassword(dto.getPassword());
        userEntity.setPhone(trimToNull(dto.getPhone()));
        userEntity.setEmail(trimToNull(dto.getEmail()));
        userEntity.setUserType(normalizeUserType(dto.getUserType()));
        userEntity.setCreditScore(UserConstant.DEFAULT_CREDIT_SCORE);
        userEntity.setRegistrationDate(LocalDate.now());
        userEntity.setVerificationStatus(UserConstant.DEFAULT_VERIFICATION_STATUS);
        userMapper.insert(userEntity);

        String userType = userEntity.getUserType();
        if (UserConstant.USER_TYPE_BUYER.equals(userType)) {
            BuyerEntity buyerEntity = new BuyerEntity();
            buyerEntity.setBuyerId(userId);
            buyerMapper.insert(buyerEntity);
        } else {
            SellerEntity sellerEntity = new SellerEntity();
            sellerEntity.setSellerId(userId);
            sellerEntity.setShopName(dto.getShopName().trim());
            sellerEntity.setShopAddress(trimToNull(dto.getShopAddress()));
            sellerEntity.setBusinessLicense(trimToNull(dto.getBusinessLicense()));
            sellerEntity.setLegalPersonId(trimToNull(dto.getLegalPersonId()));
            sellerEntity.setQualificationStatus(UserConstant.DEFAULT_QUALIFICATION_STATUS);
            sellerEntity.setGuaranteeDeposit(BigDecimal.ZERO);
            sellerEntity.setCompletedTransactionCount(0);
            sellerEntity.setPositiveRatingRate(BigDecimal.ZERO);
            sellerMapper.insert(sellerEntity);
        }

        return buildUserVO(userMapper.getByUserId(userId));
    }

    @Override
    public UserVO login(UserDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getUsername()) || !StringUtils.hasText(dto.getPassword())) {
            throw new IllegalArgumentException("用户名和密码不能为空");
        }

        UserEntity userEntity = userMapper.getByUsername(dto.getUsername());
        if (userEntity == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (!dto.getPassword().equals(userEntity.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        return buildUserVO(userEntity);
    }

    @Override
    public UserVO getProfile(String userId) {
        return buildUserVO(getExistingUser(userId));
    }

    @Override
    @Transactional
    public UserVO updateProfile(String currentUserId, UserProfileUpdateDTO dto) {
        if (!StringUtils.hasText(currentUserId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (dto == null) {
            throw new IllegalArgumentException("用户资料不能为空");
        }
        dto.setUserId(currentUserId.trim());

        UserEntity userEntity = getExistingUser(dto.getUserId());
        userEntity.setPhone(trimToNull(dto.getPhone()));
        userEntity.setEmail(trimToNull(dto.getEmail()));
        userMapper.update(userEntity);

        return buildUserVO(userMapper.getByUserId(dto.getUserId()));
    }

    @Override
    public UserVO getSellerProfile(String userId) {
        UserVO userVO = buildUserVO(getExistingUser(userId));
        if (!UserConstant.USER_TYPE_SELLER.equals(userVO.getUserType())) {
            throw new IllegalArgumentException("当前用户不是卖家");
        }
        return userVO;
    }

    @Override
    @Transactional
    public UserVO upsertSellerProfile(String currentUserId, SellerProfileDTO dto) {
        if (!StringUtils.hasText(currentUserId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (dto == null) {
            throw new IllegalArgumentException("卖家资料不能为空");
        }
        dto.setUserId(currentUserId.trim());
        UserEntity userEntity = getExistingUser(dto.getUserId());
        if (!UserConstant.USER_TYPE_SELLER.equalsIgnoreCase(userEntity.getUserType())) {
            throw new IllegalArgumentException("当前用户不是卖家");
        }
        SellerEntity sellerEntity = sellerMapper.getByUserId(dto.getUserId());
        if (sellerEntity == null) {
            if (!StringUtils.hasText(dto.getShopName())) {
                throw new IllegalArgumentException("店铺名称不能为空");
            }
            sellerEntity = new SellerEntity();
            sellerEntity.setSellerId(dto.getUserId());
            sellerEntity.setShopName(dto.getShopName().trim());
            sellerEntity.setShopAddress(trimToNull(dto.getShopAddress()));
            sellerEntity.setBusinessLicense(trimToNull(dto.getBusinessLicense()));
            sellerEntity.setLegalPersonId(trimToNull(dto.getLegalPersonId()));
            sellerEntity.setBusinessLicenseFileUrl(trimToNull(dto.getBusinessLicenseFileUrl()));
            sellerEntity.setLegalPersonIdFileUrl(trimToNull(dto.getLegalPersonIdFileUrl()));
            sellerEntity.setQualificationStatus(UserConstant.DEFAULT_QUALIFICATION_STATUS);
            sellerEntity.setGuaranteeDeposit(BigDecimal.ZERO);
            sellerEntity.setCompletedTransactionCount(0);
            sellerEntity.setPositiveRatingRate(BigDecimal.ZERO);
            sellerMapper.insert(sellerEntity);
        } else {
            boolean qualificationChanged = hasSellerQualificationChanged(sellerEntity, dto);
            if (StringUtils.hasText(dto.getShopName())) {
                sellerEntity.setShopName(dto.getShopName().trim());
            }
            sellerEntity.setShopAddress(trimToNull(dto.getShopAddress()));
            sellerEntity.setBusinessLicense(trimToNull(dto.getBusinessLicense()));
            sellerEntity.setLegalPersonId(trimToNull(dto.getLegalPersonId()));
            sellerEntity.setBusinessLicenseFileUrl(trimToNull(dto.getBusinessLicenseFileUrl()));
            sellerEntity.setLegalPersonIdFileUrl(trimToNull(dto.getLegalPersonIdFileUrl()));
            resetQualificationIfNeeded(sellerEntity, qualificationChanged);
            sellerMapper.update(sellerEntity);
        }
        return buildUserVO(userEntity);
    }

    @Override
    @Transactional
    public SellerQualificationFileUploadDTO uploadSellerQualificationFile(String userId, String fileType, MultipartFile file) {
        UserEntity userEntity = getExistingUser(userId);
        if (!UserConstant.USER_TYPE_SELLER.equalsIgnoreCase(userEntity.getUserType())) {
            throw new IllegalArgumentException("当前用户不是卖家");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String normalizedFileType = normalizeQualificationFileType(fileType);
        SellerEntity sellerEntity = getExistingSeller(userId);
        String originalFileName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "qualification-file";
        String safeExtension = extractSafeExtension(originalFileName);
        validateQualificationFileExtension(safeExtension);
        String storedFileName = normalizedFileType.toLowerCase(Locale.ROOT) + "-" + UUID.randomUUID().toString().replace("-", "") + safeExtension;
        Path targetDirectory = resolveSellerQualificationDirectory(userId);
        Path targetFile = targetDirectory.resolve(storedFileName).normalize();

        try {
            Files.createDirectories(targetDirectory);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("保存资质文件失败");
        }

        String relativeUrl = "/uploads/seller-qualification/" + userId.trim() + "/" + storedFileName;
        if ("BUSINESS_LICENSE".equals(normalizedFileType)) {
            sellerEntity.setBusinessLicenseFileUrl(relativeUrl);
        } else {
            sellerEntity.setLegalPersonIdFileUrl(relativeUrl);
        }
        resetQualificationIfNeeded(sellerEntity, true);
        sellerMapper.update(sellerEntity);

        SellerQualificationFileUploadDTO result = new SellerQualificationFileUploadDTO();
        result.setUserId(userId.trim());
        result.setFileType(normalizedFileType);
        result.setFileName(storedFileName);
        result.setFileUrl(relativeUrl);
        return result;
    }

    @Override
    public List<UserVO> adminListUsers(AdminUserQueryDTO dto) {
        AdminUserQueryDTO queryDTO = normalizeAdminQuery(dto);
        return userMapper.listUsers(queryDTO).stream()
                .map(this::buildUserVO)
                .toList();
    }

    @Override
    public UserVO adminGetUserDetail(String userId) {
        return buildUserVO(getExistingUser(userId));
    }

    @Override
    @Transactional
    public UserVO adminUpdateUser(AdminUserUpdateDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getUserId())) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        UserEntity userEntity = getExistingUser(dto.getUserId());
        userEntity.setPhone(trimToNull(dto.getPhone()));
        userEntity.setEmail(trimToNull(dto.getEmail()));
        if (dto.getCreditScore() != null) {
            validateCreditScore(dto.getCreditScore());
            userEntity.setCreditScore(dto.getCreditScore());
        }
        if (StringUtils.hasText(dto.getVerificationStatus())) {
            userEntity.setVerificationStatus(normalizeVerificationStatus(dto.getVerificationStatus()));
        }
        userMapper.update(userEntity);

        if (StringUtils.hasText(dto.getQualificationStatus())) {
            if (!UserConstant.USER_TYPE_SELLER.equalsIgnoreCase(userEntity.getUserType())) {
                throw new IllegalArgumentException("仅卖家用户支持资质状态管理");
            }
            SellerEntity sellerEntity = getExistingSeller(dto.getUserId());
            sellerEntity.setQualificationStatus(normalizeQualificationStatus(dto.getQualificationStatus()));
            sellerMapper.update(sellerEntity);
        }

        return buildUserVO(userMapper.getByUserId(dto.getUserId()));
    }

    @Override
    @Transactional
    public UserVO auditSellerQualification(String adminUserId, SellerQualificationAuditDTO dto) {
        if (!StringUtils.hasText(adminUserId)) {
            throw new IllegalArgumentException("管理员ID不能为空");
        }
        if (dto == null || !StringUtils.hasText(dto.getSellerId())) {
            throw new IllegalArgumentException("卖家ID不能为空");
        }
        dto.setAdminUserId(adminUserId.trim());

        String targetStatus = normalizeQualificationStatus(dto.getTargetStatus());
        if (UserConstant.QUALIFICATION_STATUS_PENDING.equals(targetStatus)) {
            throw new IllegalArgumentException("审核结果只能是 APPROVED 或 REJECTED");
        }

        SellerEntity sellerEntity = getExistingSeller(dto.getSellerId());
        String currentStatus = normalizeQualificationStatus(sellerEntity.getQualificationStatus());
        if (targetStatus.equals(currentStatus)) {
            throw new IllegalArgumentException("当前卖家资质已是该状态，无需重复审核");
        }
        if (!UserConstant.QUALIFICATION_STATUS_PENDING.equals(currentStatus)) {
            throw new IllegalArgumentException("仅待审核状态的卖家可以执行审核");
        }

        sellerEntity.setQualificationStatus(targetStatus);
        sellerEntity.setQualificationAuditRemark(trimToNull(dto.getAuditRemark()));
        sellerEntity.setQualificationAuditedBy(trimToNull(dto.getAdminUserId()));
        sellerEntity.setQualificationAuditTime(LocalDateTime.now());
        sellerMapper.update(sellerEntity);

        UserEntity userEntity = getExistingUser(dto.getSellerId());
        if (UserConstant.QUALIFICATION_STATUS_APPROVED.equals(targetStatus)) {
            applySellerApprovedCreditRule(userEntity);
            userMapper.update(userEntity);
        }

        return buildUserVO(userMapper.getByUserId(dto.getSellerId()));
    }

    @Override
    public boolean isAdmin(String userId) {
        return StringUtils.hasText(userId) && userMapper.countAdministratorByUserId(userId.trim()) > 0;
    }

    private void validateRegisterRequest(UserRegisterDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("注册信息不能为空");
        }
        if (!StringUtils.hasText(dto.getUsername())) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new IllegalArgumentException("密码不能为空");
        }
        String userType = normalizeUserType(dto.getUserType());
        if (UserConstant.USER_TYPE_SELLER.equals(userType) && !StringUtils.hasText(dto.getShopName())) {
            throw new IllegalArgumentException("卖家注册时店铺名称不能为空");
        }
    }

    private AdminUserQueryDTO normalizeAdminQuery(AdminUserQueryDTO dto) {
        AdminUserQueryDTO queryDTO = dto == null ? new AdminUserQueryDTO() : dto;
        queryDTO.setUsername(trimToNull(queryDTO.getUsername()));
        if (StringUtils.hasText(queryDTO.getUserType())) {
            queryDTO.setUserType(normalizeManageableUserType(queryDTO.getUserType()));
        }
        if (StringUtils.hasText(queryDTO.getVerificationStatus())) {
            queryDTO.setVerificationStatus(normalizeVerificationStatus(queryDTO.getVerificationStatus()));
        }
        if (StringUtils.hasText(queryDTO.getQualificationStatus())) {
            queryDTO.setQualificationStatus(normalizeQualificationStatus(queryDTO.getQualificationStatus()));
        }
        return queryDTO;
    }

    private UserEntity getExistingUser(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        UserEntity userEntity = userMapper.getByUserId(userId);
        if (userEntity == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return userEntity;
    }

    private SellerEntity getExistingSeller(String sellerId) {
        if (!StringUtils.hasText(sellerId)) {
            throw new IllegalArgumentException("卖家ID不能为空");
        }
        SellerEntity sellerEntity = sellerMapper.getBySellerId(sellerId.trim());
        if (sellerEntity == null) {
            throw new IllegalArgumentException("卖家不存在");
        }
        return sellerEntity;
    }

    private UserVO buildUserVO(UserEntity userEntity) {
        UserVO userVO = new UserVO();
        userVO.setUserId(userEntity.getUserId());
        userVO.setUsername(userEntity.getUsername());
        userVO.setPhone(userEntity.getPhone());
        userVO.setEmail(userEntity.getEmail());
        userVO.setUserType(userEntity.getUserType());
        userVO.setCreditScore(userEntity.getCreditScore());
        userVO.setRegistrationDate(userEntity.getRegistrationDate());
        userVO.setVerificationStatus(userEntity.getVerificationStatus());

        if (UserConstant.USER_TYPE_SELLER.equalsIgnoreCase(userEntity.getUserType())) {
            SellerEntity sellerEntity = sellerMapper.getByUserId(userEntity.getUserId());
            if (sellerEntity == null) {
                return userVO;
            }
            userVO.setSellerId(sellerEntity.getSellerId());
            userVO.setShopName(sellerEntity.getShopName());
            userVO.setShopAddress(sellerEntity.getShopAddress());
            userVO.setBusinessLicense(sellerEntity.getBusinessLicense());
            userVO.setLegalPersonId(sellerEntity.getLegalPersonId());
            userVO.setBusinessLicenseFileUrl(sellerEntity.getBusinessLicenseFileUrl());
            userVO.setLegalPersonIdFileUrl(sellerEntity.getLegalPersonIdFileUrl());
            userVO.setQualificationStatus(sellerEntity.getQualificationStatus());
            userVO.setQualificationAuditRemark(sellerEntity.getQualificationAuditRemark());
            userVO.setQualificationAuditedBy(sellerEntity.getQualificationAuditedBy());
            userVO.setQualificationAuditTime(sellerEntity.getQualificationAuditTime());
            userVO.setGuaranteeDeposit(sellerEntity.getGuaranteeDeposit());
            userVO.setCompletedTransactionCount(transactionMapper.countCompletedBySellerId(
                    userEntity.getUserId(),
                    TransactionConstant.STATUS_COMPLETED
            ));

            int totalReviews = reviewMapper.countByRevieweeId(userEntity.getUserId());
            if (totalReviews == 0) {
                userVO.setPositiveRatingRate(BigDecimal.ZERO);
            } else {
                int positiveCount = reviewMapper.countPositiveByRevieweeId(
                        userEntity.getUserId(),
                        ReviewConstant.POSITIVE_RATING_THRESHOLD
                );
                userVO.setPositiveRatingRate(BigDecimal.valueOf(positiveCount)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalReviews), 2, RoundingMode.HALF_UP));
            }
            return userVO;
        }

        if (UserConstant.USER_TYPE_BUYER.equalsIgnoreCase(userEntity.getUserType())) {
            BuyerEntity buyerEntity = buyerMapper.getByUserId(userEntity.getUserId());
            if (buyerEntity == null) {
                return userVO;
            }
            userVO.setBuyerId(buyerEntity.getBuyerId());
            return userVO;
        }

        if (userMapper.countAdministratorByUserId(userEntity.getUserId()) > 0) {
            userVO.setUserType(UserConstant.USER_TYPE_ADMIN);
            return userVO;
        }

        userVO.setUserType("USER");
        return userVO;
    }

    private String normalizeUserType(String userType) {
        if (!StringUtils.hasText(userType)) {
            throw new IllegalArgumentException("用户类型不能为空");
        }
        String normalized = userType.trim().toUpperCase(Locale.ROOT);
        if (!UserConstant.USER_TYPE_BUYER.equals(normalized) && !UserConstant.USER_TYPE_SELLER.equals(normalized)) {
            throw new IllegalArgumentException("用户类型仅支持 BUYER 或 SELLER");
        }
        return normalized;
    }

    private String normalizeManageableUserType(String userType) {
        if (!StringUtils.hasText(userType)) {
            throw new IllegalArgumentException("用户类型不能为空");
        }
        String normalized = userType.trim().toUpperCase(Locale.ROOT);
        if (!Set.of(
                UserConstant.USER_TYPE_BUYER,
                UserConstant.USER_TYPE_SELLER,
                UserConstant.USER_TYPE_ADMIN
        ).contains(normalized)) {
            throw new IllegalArgumentException("用户类型仅支持 BUYER、SELLER 或 ADMIN");
        }
        return normalized;
    }

    private String normalizeVerificationStatus(String verificationStatus) {
        if (!StringUtils.hasText(verificationStatus)) {
            throw new IllegalArgumentException("用户认证状态不能为空");
        }
        String normalized = verificationStatus.trim().toUpperCase(Locale.ROOT);
        if (!Set.of(
                UserConstant.VERIFICATION_STATUS_UNVERIFIED,
                UserConstant.VERIFICATION_STATUS_VERIFIED,
                UserConstant.VERIFICATION_STATUS_SUSPENDED
        ).contains(normalized)) {
            throw new IllegalArgumentException("认证状态仅支持 UNVERIFIED、VERIFIED 或 SUSPENDED");
        }
        return normalized;
    }

    private String normalizeQualificationStatus(String qualificationStatus) {
        if (!StringUtils.hasText(qualificationStatus)) {
            throw new IllegalArgumentException("卖家资质状态不能为空");
        }
        String normalized = qualificationStatus.trim().toUpperCase(Locale.ROOT);
        if (!Set.of(
                UserConstant.QUALIFICATION_STATUS_PENDING,
                UserConstant.QUALIFICATION_STATUS_APPROVED,
                UserConstant.QUALIFICATION_STATUS_REJECTED
        ).contains(normalized)) {
            throw new IllegalArgumentException("资质状态仅支持 PENDING、APPROVED 或 REJECTED");
        }
        return normalized;
    }

    private void validateCreditScore(Integer creditScore) {
        if (creditScore < UserConstant.MIN_CREDIT_SCORE || creditScore > UserConstant.MAX_CREDIT_SCORE) {
            throw new IllegalArgumentException("信用分范围必须在 0 到 100 之间");
        }
    }

    private void applySellerApprovedCreditRule(UserEntity userEntity) {
        int currentCreditScore = userEntity.getCreditScore() == null
                ? UserConstant.DEFAULT_CREDIT_SCORE
                : userEntity.getCreditScore();
        userEntity.setCreditScore(Math.max(currentCreditScore, UserConstant.SELLER_APPROVAL_CREDIT_SCORE));
    }

    private boolean hasSellerQualificationChanged(SellerEntity sellerEntity, SellerProfileDTO dto) {
        if (StringUtils.hasText(dto.getShopName()) && !dto.getShopName().trim().equals(sellerEntity.getShopName())) {
            return true;
        }
        if (dto.getShopAddress() != null
                && !Objects.equals(trimToNull(dto.getShopAddress()), sellerEntity.getShopAddress())) {
            return true;
        }
        if (dto.getBusinessLicense() != null
                && !Objects.equals(trimToNull(dto.getBusinessLicense()), sellerEntity.getBusinessLicense())) {
            return true;
        }
        if (dto.getLegalPersonId() != null
                && !Objects.equals(trimToNull(dto.getLegalPersonId()), sellerEntity.getLegalPersonId())) {
            return true;
        }
        if (dto.getBusinessLicenseFileUrl() != null
                && !Objects.equals(trimToNull(dto.getBusinessLicenseFileUrl()), sellerEntity.getBusinessLicenseFileUrl())) {
            return true;
        }
        return dto.getLegalPersonIdFileUrl() != null
                && !Objects.equals(trimToNull(dto.getLegalPersonIdFileUrl()), sellerEntity.getLegalPersonIdFileUrl());
    }

    private void resetQualificationIfNeeded(SellerEntity sellerEntity, boolean qualificationChanged) {
        if (!qualificationChanged || UserConstant.QUALIFICATION_STATUS_PENDING.equals(sellerEntity.getQualificationStatus())) {
            return;
        }
        sellerEntity.setQualificationStatus(UserConstant.QUALIFICATION_STATUS_PENDING);
        sellerEntity.setQualificationAuditRemark(null);
        sellerEntity.setQualificationAuditedBy(null);
        sellerEntity.setQualificationAuditTime(null);
    }

    private String normalizeQualificationFileType(String fileType) {
        if (!StringUtils.hasText(fileType)) {
            throw new IllegalArgumentException("文件类型不能为空");
        }
        String normalized = fileType.trim().toUpperCase(Locale.ROOT);
        if (!QUALIFICATION_FILE_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("文件类型仅支持 BUSINESS_LICENSE 或 LEGAL_PERSON_ID");
        }
        return normalized;
    }

    private Path resolveSellerQualificationDirectory(String userId) {
        return Paths.get(fileStorageProperties.getSellerQualificationDir())
                .toAbsolutePath()
                .normalize()
                .resolve(userId.trim())
                .normalize();
    }

    private String extractSafeExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        String extension = fileName.substring(index).toLowerCase(Locale.ROOT);
        return extension.matches("\\.[a-z0-9]{1,10}") ? extension : "";
    }

    private void validateQualificationFileExtension(String extension) {
        if (!ALLOWED_FILE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("资质文件仅支持 png、jpg、jpeg 或 pdf");
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
