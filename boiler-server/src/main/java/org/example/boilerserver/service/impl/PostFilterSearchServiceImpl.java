package org.example.boilerserver.service.impl;

import org.example.boilerpojo.BoilerDetailVO;
import org.example.boilerpojo.BoilerEntity;
import org.example.boilerpojo.PostEntity;
import org.example.boilerpojo.PostFilterSearchDTO;
import org.example.boilerpojo.PostSearchResultVO;
import org.example.boilerpojo.PostVO;
import org.example.constant.PostConstant;
import org.example.boilerserver.mapper.BoilerMapper;
import org.example.boilerserver.mapper.PostMapper;
import org.example.boilerserver.service.PostFilterSearchService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class PostFilterSearchServiceImpl implements PostFilterSearchService {
    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 20;

    private final PostMapper postMapper;
    private final BoilerMapper boilerMapper;

    public PostFilterSearchServiceImpl(PostMapper postMapper, BoilerMapper boilerMapper) {
        this.postMapper = postMapper;
        this.boilerMapper = boilerMapper;
    }

    @Override
    public List<PostSearchResultVO> search(PostFilterSearchDTO dto) {
        validateRequest(dto);
        normalizeRequest(dto);

        List<PostEntity> posts = postMapper.listByFilter(dto);
        List<PostSearchResultVO> results = new ArrayList<>();
        for (PostEntity postEntity : posts) {
            BoilerEntity boilerEntity = boilerMapper.getByBoilerId(postEntity.getBoilerId());
            if (boilerEntity == null) {
                continue;
            }
            PostSearchResultVO result = new PostSearchResultVO();
            result.setPostId(postEntity.getPostId());
            result.setPost(buildPostVO(postEntity, boilerEntity));
            results.add(result);
        }
        return results;
    }

    private void validateRequest(PostFilterSearchDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("筛选条件不能为空");
        }
        if (!StringUtils.hasText(dto.getCity())
                && !StringUtils.hasText(dto.getBoilerType())
                && !StringUtils.hasText(dto.getBrand())
                && !StringUtils.hasText(dto.getFuelType())) {
            throw new IllegalArgumentException("至少需要提供一个筛选条件");
        }
        if (dto.getLimit() != null && dto.getLimit() <= 0) {
            throw new IllegalArgumentException("limit 必须大于 0");
        }
        if (dto.getLimit() != null && dto.getLimit() > MAX_LIMIT) {
            throw new IllegalArgumentException("limit 不能超过 " + MAX_LIMIT);
        }
    }

    private void normalizeRequest(PostFilterSearchDTO dto) {
        dto.setCity(normalizeCity(dto.getCity()));
        dto.setBrand(trimToNull(dto.getBrand()));
        dto.setFuelType(trimToNull(dto.getFuelType()));
        dto.setLimit(dto.getLimit() == null ? DEFAULT_LIMIT : dto.getLimit());
        if (StringUtils.hasText(dto.getBoilerType())) {
            dto.setBoilerType(normalizeBoilerType(dto.getBoilerType()));
        }
    }

    private String normalizeBoilerType(String boilerType) {
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
        boilerDetailVO.setManufactureDate(boilerEntity.getManufactureYear());
        boilerDetailVO.setEvaporationCapacity(boilerEntity.getEvaporationCapacity());
        boilerDetailVO.setRatedThermalPower(boilerEntity.getRatedThermalPower());
        boilerDetailVO.setThermalEfficiency(boilerEntity.getThermalEfficiency());
        boilerDetailVO.setEquipmentCondition(boilerEntity.getEquipmentCondition());
        boilerDetailVO.setUsageHours(boilerEntity.getUsageHours());
        boilerDetailVO.setTestReport(boilerEntity.getTestReport());
        boilerDetailVO.setRatedOutletWaterTemperature(boilerEntity.getRatedOutletWaterTemperature());
        return boilerDetailVO;
    }
}
