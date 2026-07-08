package org.example.boilerserver.service.impl;

import org.example.boilercommon.PageResult;
import org.example.boilerpojo.BoilerDetailDTO;
import org.example.boilerpojo.BoilerDetailVO;
import org.example.boilerpojo.BoilerEntity;
import org.example.boilerpojo.PostCreateDTO;
import org.example.boilerpojo.PostEntity;
import org.example.boilerpojo.PostPageQueryDTO;
import org.example.boilerpojo.PostUpdateDTO;
import org.example.boilerpojo.PostVO;
import org.example.boilerpojo.SellerEntity;
import org.example.constant.PostConstant;
import org.example.boilerserver.mapper.BoilerMapper;
import org.example.boilerserver.mapper.PostMapper;
import org.example.boilerserver.mapper.SellerMapper;
import org.example.boilerserver.service.PostEmbeddingService;
import org.example.boilerserver.service.PostService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PostServiceImpl implements PostService {
    private final PostMapper postMapper;
    private final BoilerMapper boilerMapper;
    private final SellerMapper sellerMapper;
    private final PostEmbeddingService postEmbeddingService;

    public PostServiceImpl(
            PostMapper postMapper,
            BoilerMapper boilerMapper,
            SellerMapper sellerMapper,
            PostEmbeddingService postEmbeddingService
    ) {
        this.postMapper = postMapper;
        this.boilerMapper = boilerMapper;
        this.sellerMapper = sellerMapper;
        this.postEmbeddingService = postEmbeddingService;
    }

    @Override
    @Transactional
    public PostVO createPost(PostCreateDTO dto) {
        validateCreateRequest(dto);
        ensureSellerExists(dto.getSellerId());
        BoilerDetailDTO boilerDetail = dto.getBoilerDetail();
        validateBoilerDetail(boilerDetail);

        // 帖子与锅炉详情分表存储，先落锅炉再关联帖子
        String boilerId = generateId();
        BoilerEntity boilerEntity = buildBoilerEntity(boilerId, boilerDetail);
        boilerMapper.insert(boilerEntity);

        String postId = generateId();
        LocalDate today = LocalDate.now();
        PostEntity postEntity = new PostEntity();
        postEntity.setPostId(postId);
        postEntity.setSellerId(dto.getSellerId().trim());
        postEntity.setTitle(resolveTitle(dto.getTitle(), boilerEntity));
        postEntity.setPrice(dto.getPrice());
        postEntity.setDescription(trimToNull(dto.getDescription()));
        postEntity.setStatus(PostConstant.STATUS_PUBLISHED);
        postEntity.setPublishTime(today);
        postEntity.setUpdateTime(today);
        postEntity.setViewCount(0);
        postEntity.setMediaFiles(trimToNull(dto.getMediaFiles()));
        postEntity.setAiValuationRange(calculateAiValuationRange(boilerEntity));
        postEntity.setCity(normalizeCity(dto.getCity()));
        postEntity.setBoilerId(boilerId);
        postMapper.insert(postEntity);

        PostVO result = buildPostVO(postEntity, boilerEntity);
        postEmbeddingService.vectorizePost(postEntity.getPostId());
        return result;
    }

    @Override
    @Transactional
    public PostVO getPostDetail(String postId) {
        PostEntity postEntity = getExistingPost(postId);
        // 每次打开详情页都按需求累计一次浏览量
        postMapper.incrementViewCount(postEntity.getPostId());
        PostEntity latestPostEntity = getExistingPost(postId);
        BoilerEntity boilerEntity = getExistingBoiler(latestPostEntity.getBoilerId());
        return buildPostVO(latestPostEntity, boilerEntity);
    }

    @Override
    @Transactional
    public PostVO updatePost(PostUpdateDTO dto) {
        validateUpdateRequest(dto);
        ensureSellerExists(dto.getSellerId());
        BoilerDetailDTO boilerDetail = dto.getBoilerDetail();
        validateBoilerDetail(boilerDetail);

        PostEntity existingPost = getExistingPost(dto.getPostId());
        validatePostOwnership(existingPost, dto.getSellerId());

        BoilerEntity boilerEntity = buildBoilerEntity(existingPost.getBoilerId(), boilerDetail);
        boilerMapper.update(boilerEntity);

        existingPost.setTitle(resolveTitle(dto.getTitle(), boilerEntity));
        existingPost.setPrice(dto.getPrice());
        existingPost.setDescription(trimToNull(dto.getDescription()));
        existingPost.setMediaFiles(trimToNull(dto.getMediaFiles()));
        existingPost.setCity(normalizeCity(dto.getCity()));
        existingPost.setAiValuationRange(calculateAiValuationRange(boilerEntity));
        // 编辑后状态保持或更新为 PUBLISHED
        existingPost.setStatus(PostConstant.STATUS_PUBLISHED);
        existingPost.setUpdateTime(LocalDate.now());
        postMapper.update(existingPost);

        PostEntity latestPostEntity = getExistingPost(existingPost.getPostId());
        PostVO result = buildPostVO(latestPostEntity, getExistingBoiler(latestPostEntity.getBoilerId()));
        postEmbeddingService.vectorizePost(latestPostEntity.getPostId());
        return result;
    }

    @Override
    @Transactional
    public void deletePost(String postId, String sellerId) {
        PostEntity postEntity = getExistingPost(postId);
        validatePostOwnership(postEntity, sellerId);
        postEmbeddingService.deletePostVector(postEntity.getPostId());
        postMapper.deleteByPostId(postEntity.getPostId());
        boilerMapper.deleteByBoilerId(postEntity.getBoilerId());
    }

    @Override
    @Transactional
    public void delistPost(String postId, String sellerId) {
        PostEntity postEntity = getExistingPost(postId);
        validatePostOwnership(postEntity, sellerId);
        if (PostConstant.STATUS_DELISTED.equals(postEntity.getStatus())) {
            throw new IllegalArgumentException("该帖子已经下架");
        }
        postMapper.updateStatus(postEntity.getPostId(), PostConstant.STATUS_DELISTED);
    }

    @Override
    @Transactional
    public void banPost(String postId, String adminUserId) {
        if (!StringUtils.hasText(adminUserId)) {
            throw new IllegalArgumentException("管理员ID不能为空");
        }
        PostEntity postEntity = getExistingPost(postId);
        if (PostConstant.STATUS_BANNED.equals(postEntity.getStatus())) {
            throw new IllegalArgumentException("该帖子已经被封禁");
        }
        postMapper.updateStatus(postEntity.getPostId(), PostConstant.STATUS_BANNED);
    }

    @Override
    public PageResult<PostVO> listPublishedPosts(PostPageQueryDTO dto) {
        PostPageQueryDTO query = normalizePageQuery(dto, false);
        long total = postMapper.countPublishedPosts();
        List<PostVO> records = postMapper
                .listPublishedPosts(query.getOffset(), query.getPageSize(), query.getSortField(), query.getSortOrder())
                .stream()
                .map(this::buildPostVO)
                .toList();
        return PageResult.of(records, total, query.getPageNum(), query.getPageSize());
    }

    @Override
    public PageResult<PostVO> filterPublishedPosts(PostPageQueryDTO dto) {
        PostPageQueryDTO query = normalizePageQuery(dto, true);
        long total = postMapper.countPublishedPostsByFilter(query);
        List<PostVO> records = postMapper.listPublishedPostsByFilter(query)
                .stream()
                .map(this::buildPostVO)
                .toList();
        return PageResult.of(records, total, query.getPageNum(), query.getPageSize());
    }

    private void validateCreateRequest(PostCreateDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("帖子信息不能为空");
        }
        if (!StringUtils.hasText(dto.getSellerId())) {
            throw new IllegalArgumentException("卖家ID不能为空");
        }
        if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("价格必须大于 0");
        }
        if (dto.getBoilerDetail() == null) {
            throw new IllegalArgumentException("锅炉详情不能为空");
        }
    }

    private void validateUpdateRequest(PostUpdateDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("帖子信息不能为空");
        }
        if (!StringUtils.hasText(dto.getPostId())) {
            throw new IllegalArgumentException("帖子ID不能为空");
        }
        if (!StringUtils.hasText(dto.getSellerId())) {
            throw new IllegalArgumentException("卖家ID不能为空");
        }
        if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("价格必须大于 0");
        }
        if (dto.getBoilerDetail() == null) {
            throw new IllegalArgumentException("锅炉详情不能为空");
        }
    }

    private void validateBoilerDetail(BoilerDetailDTO boilerDetail) {
        if (boilerDetail == null) {
            throw new IllegalArgumentException("锅炉详情不能为空");
        }
        if (!StringUtils.hasText(boilerDetail.getModel())) {
            throw new IllegalArgumentException("锅炉型号不能为空");
        }
        if (!StringUtils.hasText(boilerDetail.getBrand())) {
            throw new IllegalArgumentException("品牌不能为空");
        }
        normalizeBoilerType(boilerDetail.getBoilerType());
        if (!StringUtils.hasText(boilerDetail.getFuelType())) {
            throw new IllegalArgumentException("燃料类型不能为空");
        }
        requirePositive(boilerDetail.getNoxEmissions(), "氮氧化物排放");
        requirePositive(boilerDetail.getWorkingPressure(), "工作压力");
        requirePositive(boilerDetail.getFootprintArea(), "占地面积");
        if (boilerDetail.getManufactureStartDate() == null || boilerDetail.getManufactureEndDate() == null) {
            throw new IllegalArgumentException("生产日期范围不能为空");
        }
        if (boilerDetail.getManufactureEndDate().isBefore(boilerDetail.getManufactureStartDate())) {
            throw new IllegalArgumentException("生产结束日期不能早于开始日期");
        }
        if (!StringUtils.hasText(boilerDetail.getEquipmentCondition())) {
            throw new IllegalArgumentException("锅炉成色不能为空");
        }

        String boilerType = normalizeBoilerType(boilerDetail.getBoilerType());
        // 热水锅炉和蒸汽锅炉的核心能力字段不同，按类型分别校验
        if (PostConstant.BOILER_TYPE_HOT_WATER.equals(boilerType)) {
            requirePositive(boilerDetail.getRatedThermalPower(), "额定热功率");
        }
        if (PostConstant.BOILER_TYPE_STEAM.equals(boilerType)) {
            requirePositive(boilerDetail.getEvaporationCapacity(), "蒸发量");
        }

        if (boilerDetail.getTonnage() != null && boilerDetail.getTonnage().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("吨位必须大于 0");
        }
        if (boilerDetail.getThermalEfficiency() != null && boilerDetail.getThermalEfficiency().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("热效率必须大于 0");
        }
        if (boilerDetail.getUsageHours() != null && boilerDetail.getUsageHours().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("使用时长不能小于 0");
        }
        if (boilerDetail.getRatedOutletWaterTemperature() != null
                && boilerDetail.getRatedOutletWaterTemperature().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("额定出水温度必须大于 0");
        }
    }

    private void requirePositive(BigDecimal value, String fieldName) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(fieldName + "必须大于 0");
        }
    }

    private void ensureSellerExists(String sellerId) {
        SellerEntity sellerEntity = sellerMapper.getBySellerId(sellerId.trim());
        if (sellerEntity == null) {
            throw new IllegalArgumentException("当前用户不是卖家或卖家不存在");
        }
    }

    private void validatePostOwnership(PostEntity postEntity, String sellerId) {
        if (!postEntity.getSellerId().equals(sellerId.trim())) {
            throw new IllegalArgumentException("仅发帖卖家本人可以操作该帖子");
        }
    }

    private PostEntity getExistingPost(String postId) {
        if (!StringUtils.hasText(postId)) {
            throw new IllegalArgumentException("帖子ID不能为空");
        }
        PostEntity postEntity = postMapper.getByPostId(postId.trim());
        if (postEntity == null) {
            throw new IllegalArgumentException("帖子不存在");
        }
        return postEntity;
    }

    private BoilerEntity getExistingBoiler(String boilerId) {
        BoilerEntity boilerEntity = boilerMapper.getByBoilerId(boilerId);
        if (boilerEntity == null) {
            throw new IllegalArgumentException("锅炉详情不存在");
        }
        return boilerEntity;
    }

    private BoilerEntity buildBoilerEntity(String boilerId, BoilerDetailDTO dto) {
        BoilerEntity boilerEntity = new BoilerEntity();
        boilerEntity.setBoilerId(boilerId);
        boilerEntity.setModel(dto.getModel().trim());
        boilerEntity.setBrand(dto.getBrand().trim());
        boilerEntity.setBoilerType(normalizeBoilerType(dto.getBoilerType()));
        boilerEntity.setTonnage(dto.getTonnage());
        boilerEntity.setFuelType(dto.getFuelType().trim());
        boilerEntity.setWorkingPressure(dto.getWorkingPressure());
        boilerEntity.setNoxEmissions(dto.getNoxEmissions());
        boilerEntity.setFootprintArea(dto.getFootprintArea());
        // 数据库仅保留单个 manufactureDate 列，继续映射到 manufactureYear 属性以兼容现有估值逻辑。
        boilerEntity.setManufactureYear(resolveManufactureYear(dto));
        boilerEntity.setEvaporationCapacity(dto.getEvaporationCapacity());
        boilerEntity.setRatedThermalPower(dto.getRatedThermalPower());
        boilerEntity.setThermalEfficiency(dto.getThermalEfficiency());
        boilerEntity.setEquipmentCondition(dto.getEquipmentCondition().trim());
        boilerEntity.setUsageHours(dto.getUsageHours());
        boilerEntity.setTestReport(trimToNull(dto.getTestReport()));
        boilerEntity.setRatedOutletWaterTemperature(dto.getRatedOutletWaterTemperature());
        return boilerEntity;
    }

    private PostVO buildPostVO(PostEntity postEntity) {
        return buildPostVO(postEntity, getExistingBoiler(postEntity.getBoilerId()));
    }

    private PostVO buildPostVO(PostEntity postEntity, BoilerEntity boilerEntity) {
        PostVO postVO = new PostVO();
        postVO.setPostId(postEntity.getPostId());
        postVO.setSellerId(postEntity.getSellerId());
        postVO.setTitle(postEntity.getTitle());
        postVO.setPrice(postEntity.getPrice());
        postVO.setDescription(postEntity.getDescription());
        postVO.setStatus(postEntity.getStatus());
        postVO.setPublishTime(postEntity.getPublishTime());
        postVO.setUpdateTime(postEntity.getUpdateTime());
        postVO.setViewCount(postEntity.getViewCount());
        postVO.setMediaFiles(postEntity.getMediaFiles());
        postVO.setAiValuationRange(postEntity.getAiValuationRange());
        postVO.setCity(postEntity.getCity());
        postVO.setBoilerDetail(buildBoilerDetailVO(boilerEntity));
        return postVO;
    }

    private BoilerDetailVO buildBoilerDetailVO(BoilerEntity boilerEntity) {
        BoilerDetailVO boilerDetailVO = new BoilerDetailVO();
        boilerDetailVO.setBoilerId(boilerEntity.getBoilerId());
        boilerDetailVO.setModel(boilerEntity.getModel());
        boilerDetailVO.setBrand(boilerEntity.getBrand());
        boilerDetailVO.setBoilerType(boilerEntity.getBoilerType());
        boilerDetailVO.setTonnage(boilerEntity.getTonnage());
        boilerDetailVO.setFuelType(boilerEntity.getFuelType());
        boilerDetailVO.setWorkingPressure(boilerEntity.getWorkingPressure());
        boilerDetailVO.setNoxEmissions(boilerEntity.getNoxEmissions());
        boilerDetailVO.setFootprintArea(boilerEntity.getFootprintArea());
        boilerDetailVO.setManufactureStartDate(boilerEntity.getManufactureYear());
        boilerDetailVO.setManufactureEndDate(boilerEntity.getManufactureYear());
        boilerDetailVO.setEvaporationCapacity(boilerEntity.getEvaporationCapacity());
        boilerDetailVO.setRatedThermalPower(boilerEntity.getRatedThermalPower());
        boilerDetailVO.setThermalEfficiency(boilerEntity.getThermalEfficiency());
        boilerDetailVO.setEquipmentCondition(boilerEntity.getEquipmentCondition());
        boilerDetailVO.setUsageHours(boilerEntity.getUsageHours());
        boilerDetailVO.setTestReport(boilerEntity.getTestReport());
        boilerDetailVO.setRatedOutletWaterTemperature(boilerEntity.getRatedOutletWaterTemperature());
        return boilerDetailVO;
    }

    private String calculateAiValuationRange(BoilerEntity boilerEntity) {
        // 先给一个基础估值，再按规格、年限和成色做简单加减权重
        BigDecimal estimate = BigDecimal.valueOf(80000);
        if (boilerEntity.getRatedThermalPower() != null) {
            estimate = estimate.add(boilerEntity.getRatedThermalPower().multiply(BigDecimal.valueOf(40)));
        }
        if (boilerEntity.getEvaporationCapacity() != null) {
            estimate = estimate.add(boilerEntity.getEvaporationCapacity().multiply(BigDecimal.valueOf(90000)));
        }
        if (boilerEntity.getTonnage() != null) {
            estimate = estimate.add(boilerEntity.getTonnage().multiply(BigDecimal.valueOf(6000)));
        }
        if (boilerEntity.getThermalEfficiency() != null) {
            estimate = estimate.add(boilerEntity.getThermalEfficiency().multiply(BigDecimal.valueOf(500)));
        }

        long serviceYears = ChronoUnit.YEARS.between(
                boilerEntity.getManufactureYear(),
                LocalDate.now()
        );
        if (serviceYears > 0) {
            estimate = estimate.subtract(BigDecimal.valueOf(serviceYears).multiply(BigDecimal.valueOf(3000)));
        }

        String condition = boilerEntity.getEquipmentCondition().trim().toUpperCase(Locale.ROOT);
        if (condition.contains("NEW") || condition.contains("全新")) {
            estimate = estimate.add(BigDecimal.valueOf(20000));
        } else if (condition.contains("GOOD") || condition.contains("良")) {
            estimate = estimate.add(BigDecimal.valueOf(10000));
        }

        BigDecimal minimum = estimate.multiply(BigDecimal.valueOf(0.9)).setScale(0, RoundingMode.HALF_UP);
        BigDecimal maximum = estimate.multiply(BigDecimal.valueOf(1.1)).setScale(0, RoundingMode.HALF_UP);
        if (minimum.compareTo(BigDecimal.ZERO) < 0) {
            minimum = BigDecimal.ZERO;
        }
        return "CNY " + minimum.toPlainString() + " - " + maximum.toPlainString();
    }

    private String resolveTitle(String inputTitle, BoilerEntity boilerEntity) {
        if (StringUtils.hasText(inputTitle)) {
            return inputTitle.trim();
        }
        // 未传标题时，使用品牌 + 型号 + 类型自动生成
        return boilerEntity.getBrand() + " " + boilerEntity.getModel() + " " + boilerEntity.getBoilerType();
    }

    private String normalizeBoilerType(String boilerType) {
        if (!StringUtils.hasText(boilerType)) {
            throw new IllegalArgumentException("锅炉类型不能为空");
        }
        String normalized = boilerType.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        if ("HOT_WATER_BOILER".equals(normalized)) {
            normalized = PostConstant.BOILER_TYPE_HOT_WATER;
        }
        if ("STEAM_BOILER".equals(normalized)) {
            normalized = PostConstant.BOILER_TYPE_STEAM;
        }
        if (!PostConstant.BOILER_TYPE_HOT_WATER.equals(normalized)
                && !PostConstant.BOILER_TYPE_STEAM.equals(normalized)) {
            throw new IllegalArgumentException("锅炉类型仅支持 HOT_WATER 或 STEAM");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeCity(String city) {
        String normalized = trimToNull(city);
        if (normalized == null) {
            return null;
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private PostPageQueryDTO normalizePageQuery(PostPageQueryDTO dto, boolean requireFilter) {
        PostPageQueryDTO query = dto == null ? new PostPageQueryDTO() : dto;
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : query.getPageSize();
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        query.setOffset((pageNum - 1) * pageSize);
        query.setSortField(validateSortField(
                query.getSortField(),
                new String[]{"publishTime", "updateTime", "price", "viewCount"}
        ));
        query.setSortOrder("asc".equalsIgnoreCase(query.getSortOrder()) ? "asc" : "desc");
        query.setCity(normalizeCity(query.getCity()));
        query.setBrand(trimToNull(query.getBrand()));
        query.setFuelType(trimToNull(query.getFuelType()));
        if (StringUtils.hasText(query.getBoilerType())) {
            query.setBoilerType(normalizeBoilerType(query.getBoilerType()));
        }
        if (requireFilter
                && !StringUtils.hasText(query.getCity())
                && !StringUtils.hasText(query.getBoilerType())
                && !StringUtils.hasText(query.getBrand())
                && !StringUtils.hasText(query.getFuelType())) {
            throw new IllegalArgumentException("至少需要提供一个筛选条件");
        }
        return query;
    }

    private String validateSortField(String sortField, String[] allowedFields) {
        if (!StringUtils.hasText(sortField)) {
            return null;
        }
        return Arrays.stream(allowedFields)
                .filter(field -> field.equals(sortField))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的排序字段"));
    }

    private LocalDate resolveManufactureYear(BoilerDetailDTO dto) {
        if (dto.getManufactureEndDate() != null) {
            return dto.getManufactureEndDate();
        }
        return dto.getManufactureStartDate();
    }

    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
