package org.example.boilerserver.service.impl;

import org.example.boilerpojo.BoilerDetailDTO;
import org.example.boilerpojo.BoilerDetailVO;
import org.example.boilerpojo.BoilerEntity;
import org.example.boilerpojo.PostCreateDTO;
import org.example.boilerpojo.PostEntity;
import org.example.boilerpojo.PostUpdateDTO;
import org.example.boilerpojo.PostVO;
import org.example.boilerpojo.SellerEntity;
import org.example.constant.PostConstant;
import org.example.boilerserver.mapper.BoilerMapper;
import org.example.boilerserver.mapper.PostMapper;
import org.example.boilerserver.mapper.SellerMapper;
import org.example.boilerserver.service.PostService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class PostServiceImpl implements PostService {
    private final PostMapper postMapper;
    private final BoilerMapper boilerMapper;
    private final SellerMapper sellerMapper;

    public PostServiceImpl(PostMapper postMapper, BoilerMapper boilerMapper, SellerMapper sellerMapper) {
        this.postMapper = postMapper;
        this.boilerMapper = boilerMapper;
        this.sellerMapper = sellerMapper;
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
        postEntity.setStatus(PostConstant.POST_STATUS_PENDING_REVIEW);
        postEntity.setPublishTime(today);
        postEntity.setUpdateTime(today);
        postEntity.setViewCount(0);
        postEntity.setMediaFiles(trimToNull(dto.getMediaFiles()));
        postEntity.setAiValuationRange(calculateAiValuationRange(boilerEntity));
        postEntity.setCity(trimToNull(dto.getCity()));
        postEntity.setBoilerId(boilerId);
        postMapper.insert(postEntity);

        return buildPostVO(postEntity, boilerEntity);
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
        existingPost.setCity(trimToNull(dto.getCity()));
        existingPost.setAiValuationRange(calculateAiValuationRange(boilerEntity));
        // 编辑后需要重新进入审核流程
        existingPost.setStatus(PostConstant.POST_STATUS_PENDING_REVIEW);
        existingPost.setUpdateTime(LocalDate.now());
        postMapper.update(existingPost);

        PostEntity latestPostEntity = getExistingPost(existingPost.getPostId());
        return buildPostVO(latestPostEntity, getExistingBoiler(latestPostEntity.getBoilerId()));
    }

    @Override
    @Transactional
    public void deletePost(String postId, String sellerId) {
        PostEntity postEntity = getExistingPost(postId);
        validatePostOwnership(postEntity, sellerId);
        postMapper.deleteByPostId(postEntity.getPostId());
        boilerMapper.deleteByBoilerId(postEntity.getBoilerId());
    }


    public String evaluateAiValuationRange(BoilerDetailDTO boilerDetail) {
        validateBoilerDetail(boilerDetail);
        BoilerEntity boilerEntity = buildBoilerEntity(generateId(), boilerDetail);
        return calculateAiValuationRange(boilerEntity);
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
        boilerEntity.setManufactureStartDate(dto.getManufactureStartDate());
        boilerEntity.setManufactureEndDate(dto.getManufactureEndDate());
        boilerEntity.setEvaporationCapacity(dto.getEvaporationCapacity());
        boilerEntity.setRatedThermalPower(dto.getRatedThermalPower());
        boilerEntity.setThermalEfficiency(dto.getThermalEfficiency());
        boilerEntity.setEquipmentCondition(dto.getEquipmentCondition().trim());
        boilerEntity.setUsageHours(dto.getUsageHours());
        boilerEntity.setTestReport(trimToNull(dto.getTestReport()));
        boilerEntity.setRatedOutletWaterTemperature(dto.getRatedOutletWaterTemperature());
        boilerEntity.setApplicationScenario(trimToNull(dto.getApplicationScenario()));
        return boilerEntity;
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
        boilerDetailVO.setManufactureStartDate(boilerEntity.getManufactureStartDate());
        boilerDetailVO.setManufactureEndDate(boilerEntity.getManufactureEndDate());
        boilerDetailVO.setEvaporationCapacity(boilerEntity.getEvaporationCapacity());
        boilerDetailVO.setRatedThermalPower(boilerEntity.getRatedThermalPower());
        boilerDetailVO.setThermalEfficiency(boilerEntity.getThermalEfficiency());
        boilerDetailVO.setEquipmentCondition(boilerEntity.getEquipmentCondition());
        boilerDetailVO.setUsageHours(boilerEntity.getUsageHours());
        boilerDetailVO.setTestReport(boilerEntity.getTestReport());
        boilerDetailVO.setRatedOutletWaterTemperature(boilerEntity.getRatedOutletWaterTemperature());
        boilerDetailVO.setApplicationScenario(boilerEntity.getApplicationScenario());
        return boilerDetailVO;
    }

    private String calculateAiValuationRange(BoilerEntity boilerEntity) {
        String fittedRange = calculateAiValuationRangeByLinearFit(boilerEntity);
        if (fittedRange != null) {
            return fittedRange;
        }

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
                boilerEntity.getManufactureEndDate(),
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

    private String calculateAiValuationRangeByLinearFit(BoilerEntity boilerEntity) {
        List<Map<String, Object>> rows = postMapper.listAiValuationTrainingRows(300);
        if (rows == null || rows.size() < 12) {
            return null;
        }

        int featureCount = 11;
        int sampleCount = 0;
        double[][] x = new double[rows.size()][featureCount];
        double[] y = new double[rows.size()];
        for (Map<String, Object> row : rows) {
            Double price = toDouble(row.get("price"));
            if (price == null || price <= 0) {
                continue;
            }
            double[] features = buildTrainingFeatures(
                    row.get("ratedThermalPower"),
                    row.get("evaporationCapacity"),
                    row.get("tonnage"),
                    row.get("thermalEfficiency"),
                    row.get("manufactureEndDate"),
                    row.get("boilerType"),
                    row.get("fuelType"),
                    row.get("noxEmissions"),
                    row.get("workingPressure"),
                    row.get("footprintArea")
            );
            if (features == null) {
                continue;
            }
            x[sampleCount] = features;
            y[sampleCount] = price;
            sampleCount++;
        }

        if (sampleCount < 12) {
            return null;
        }

        double[] beta = ridgeFit(x, y, sampleCount, featureCount, 1.0);
        if (beta == null) {
            return null;
        }

        double predicted = dot(beta, buildInputFeatures(boilerEntity));
        if (!Double.isFinite(predicted) || predicted <= 0) {
            return null;
        }

        double residualStd = computeResidualStd(x, y, beta, sampleCount);
        double delta = Math.max(predicted * 0.12, residualStd);

        BigDecimal minimum = BigDecimal.valueOf(Math.max(0, predicted - delta)).setScale(0, RoundingMode.HALF_UP);
        BigDecimal maximum = BigDecimal.valueOf(predicted + delta).setScale(0, RoundingMode.HALF_UP);
        return "CNY " + minimum.toPlainString() + " - " + maximum.toPlainString();
    }

    private double[] buildInputFeatures(BoilerEntity boilerEntity) {
        String fuelType = boilerEntity.getFuelType();
        String boilerType = boilerEntity.getBoilerType();
        return buildFeatures(
                boilerEntity.getRatedThermalPower(),
                boilerEntity.getEvaporationCapacity(),
                boilerEntity.getTonnage(),
                boilerEntity.getThermalEfficiency(),
                boilerEntity.getManufactureEndDate(),
                boilerType,
                fuelType,
                boilerEntity.getNoxEmissions(),
                boilerEntity.getWorkingPressure(),
                boilerEntity.getFootprintArea()
        );
    }

    private double[] buildTrainingFeatures(Object ratedThermalPower,
                                           Object evaporationCapacity,
                                           Object tonnage,
                                           Object thermalEfficiency,
                                           Object manufactureEndDate,
                                           Object boilerType,
                                           Object fuelType,
                                           Object noxEmissions,
                                           Object workingPressure,
                                           Object footprintArea) {
        return buildFeatures(
                toBigDecimal(ratedThermalPower),
                toBigDecimal(evaporationCapacity),
                toBigDecimal(tonnage),
                toBigDecimal(thermalEfficiency),
                toLocalDate(manufactureEndDate),
                boilerType == null ? null : boilerType.toString(),
                fuelType == null ? null : fuelType.toString(),
                toBigDecimal(noxEmissions),
                toBigDecimal(workingPressure),
                toBigDecimal(footprintArea)
        );
    }

    private double[] buildFeatures(BigDecimal ratedThermalPower,
                                   BigDecimal evaporationCapacity,
                                   BigDecimal tonnage,
                                   BigDecimal thermalEfficiency,
                                   LocalDate manufactureEndDate,
                                   String boilerType,
                                   String fuelType,
                                   BigDecimal noxEmissions,
                                   BigDecimal workingPressure,
                                   BigDecimal footprintArea) {
        if (manufactureEndDate == null) {
            return null;
        }

        double serviceYears = ChronoUnit.DAYS.between(manufactureEndDate, LocalDate.now()) / 365.25;
        if (!Double.isFinite(serviceYears) || serviceYears < 0) {
            serviceYears = 0;
        }

        FuelCategory fuelCategory = categorizeFuel(fuelType);
        double isSteam = "STEAM".equalsIgnoreCase(normalizeText(boilerType)) ? 1.0 : 0.0;

        return new double[]{
                1.0,
                toDoubleOrZero(ratedThermalPower),
                toDoubleOrZero(evaporationCapacity),
                toDoubleOrZero(tonnage),
                toDoubleOrZero(thermalEfficiency),
                serviceYears,
                isSteam,
                fuelCategory == FuelCategory.GAS ? 1.0 : 0.0,
                fuelCategory == FuelCategory.OIL ? 1.0 : 0.0,
                fuelCategory == FuelCategory.BIOMASS ? 1.0 : 0.0,
                fuelCategory == FuelCategory.ELECTRIC ? 1.0 : 0.0
        };
    }

    private enum FuelCategory {
        GAS,
        OIL,
        BIOMASS,
        ELECTRIC,
        OTHER
    }

    private FuelCategory categorizeFuel(String fuelType) {
        String normalized = normalizeText(fuelType);
        if (normalized == null) {
            return FuelCategory.OTHER;
        }
        if (normalized.contains("ELECTRIC") || normalized.contains("电")) {
            return FuelCategory.ELECTRIC;
        }
        if (normalized.contains("BIOMASS") || normalized.contains("生物质")) {
            return FuelCategory.BIOMASS;
        }
        if (normalized.contains("OIL") || normalized.contains("柴油") || normalized.contains("燃油")) {
            return FuelCategory.OIL;
        }
        if (normalized.contains("GAS") || normalized.contains("NATURAL") || normalized.contains("天然气")) {
            return FuelCategory.GAS;
        }
        return FuelCategory.OTHER;
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private double toDoubleOrZero(BigDecimal value) {
        if (value == null) {
            return 0.0;
        }
        return value.doubleValue();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        String text = value.toString().trim();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (Exception ex) {
            return null;
        }
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        String text = value.toString().trim();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Double.parseDouble(text);
        } catch (Exception ex) {
            return null;
        }
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate();
        }
        if (value instanceof java.util.Date) {
            return new java.sql.Date(((java.util.Date) value).getTime()).toLocalDate();
        }
        String text = value.toString().trim();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (Exception ex) {
            return null;
        }
    }

    private double[] ridgeFit(double[][] x, double[] y, int n, int m, double lambda) {
        double[][] xtx = new double[m][m];
        double[] xty = new double[m];
        for (int i = 0; i < n; i++) {
            double[] row = x[i];
            for (int j = 0; j < m; j++) {
                xty[j] += row[j] * y[i];
                for (int k = 0; k < m; k++) {
                    xtx[j][k] += row[j] * row[k];
                }
            }
        }
        for (int i = 0; i < m; i++) {
            xtx[i][i] += lambda;
        }
        return solveLinearSystem(xtx, xty);
    }

    private double computeResidualStd(double[][] x, double[] y, double[] beta, int n) {
        double sumSq = 0.0;
        int count = 0;
        for (int i = 0; i < n; i++) {
            double predicted = dot(beta, x[i]);
            if (!Double.isFinite(predicted)) {
                continue;
            }
            double r = y[i] - predicted;
            sumSq += r * r;
            count++;
        }
        if (count <= 2) {
            return 0.0;
        }
        return Math.sqrt(sumSq / (count - 1));
    }

    private double dot(double[] a, double[] b) {
        double sum = 0.0;
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    private double[] solveLinearSystem(double[][] a, double[] b) {
        int n = b.length;
        double[][] aug = new double[n][n + 1];
        for (int i = 0; i < n; i++) {
            System.arraycopy(a[i], 0, aug[i], 0, n);
            aug[i][n] = b[i];
        }

        for (int pivot = 0; pivot < n; pivot++) {
            int maxRow = pivot;
            double maxAbs = Math.abs(aug[pivot][pivot]);
            for (int r = pivot + 1; r < n; r++) {
                double abs = Math.abs(aug[r][pivot]);
                if (abs > maxAbs) {
                    maxAbs = abs;
                    maxRow = r;
                }
            }
            if (maxAbs < 1e-9) {
                return null;
            }
            if (maxRow != pivot) {
                double[] tmp = aug[pivot];
                aug[pivot] = aug[maxRow];
                aug[maxRow] = tmp;
            }

            double pivotValue = aug[pivot][pivot];
            for (int c = pivot; c <= n; c++) {
                aug[pivot][c] /= pivotValue;
            }

            for (int r = 0; r < n; r++) {
                if (r == pivot) {
                    continue;
                }
                double factor = aug[r][pivot];
                if (Math.abs(factor) < 1e-12) {
                    continue;
                }
                for (int c = pivot; c <= n; c++) {
                    aug[r][c] -= factor * aug[pivot][c];
                }
            }
        }

        double[] x = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = aug[i][n];
        }
        return x;
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

    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
