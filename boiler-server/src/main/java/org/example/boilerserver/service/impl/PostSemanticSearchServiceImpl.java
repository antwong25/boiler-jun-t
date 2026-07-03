package org.example.boilerserver.service.impl;

import com.pgvector.PGvector;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.example.boilerpojo.BoilerDetailVO;
import org.example.boilerpojo.BoilerEntity;
import org.example.boilerpojo.PostEntity;
import org.example.boilerpojo.PostSemanticSearchDTO;
import org.example.boilerpojo.PostSemanticSearchVO;
import org.example.boilerpojo.PostVO;
import org.example.boilerserver.config.VectorStoreProperties;
import org.example.boilerserver.mapper.BoilerMapper;
import org.example.boilerserver.mapper.PostMapper;
import org.example.boilerserver.service.PostSemanticSearchService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class PostSemanticSearchServiceImpl implements PostSemanticSearchService {
    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 20;

    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate vectorJdbcTemplate;
    private final VectorStoreProperties vectorStoreProperties;
    private final PostMapper postMapper;
    private final BoilerMapper boilerMapper;

    public PostSemanticSearchServiceImpl(
            EmbeddingModel embeddingModel,
            @Qualifier("vectorJdbcTemplate") JdbcTemplate vectorJdbcTemplate,
            VectorStoreProperties vectorStoreProperties,
            PostMapper postMapper,
            BoilerMapper boilerMapper
    ) {
        this.embeddingModel = embeddingModel;
        this.vectorJdbcTemplate = vectorJdbcTemplate;
        this.vectorStoreProperties = vectorStoreProperties;
        this.postMapper = postMapper;
        this.boilerMapper = boilerMapper;
    }

    @Override
    public List<PostSemanticSearchVO> search(PostSemanticSearchDTO dto) {
        validateRequest(dto);

        Embedding queryEmbedding = embeddingModel.embed(dto.getQuery().trim()).content();
        List<VectorSearchRow> rows = searchVectors(queryEmbedding.vector(), resolveLimit(dto.getLimit()), dto.getMinScore());

        List<PostSemanticSearchVO> results = new ArrayList<>();
        for (VectorSearchRow row : rows) {
            PostEntity postEntity = postMapper.getByPostId(row.postId());
            if (postEntity == null) {
                continue;
            }
            BoilerEntity boilerEntity = boilerMapper.getByBoilerId(postEntity.getBoilerId());
            if (boilerEntity == null) {
                continue;
            }

            PostSemanticSearchVO result = new PostSemanticSearchVO();
            result.setPostId(postEntity.getPostId());
            result.setScore(row.score());
            result.setPost(buildPostVO(postEntity, boilerEntity));
            results.add(result);
        }
        return results;
    }

    private void validateRequest(PostSemanticSearchDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("搜索条件不能为空");
        }
        if (!StringUtils.hasText(dto.getQuery())) {
            throw new IllegalArgumentException("搜索文本不能为空");
        }
        if (dto.getLimit() != null && dto.getLimit() <= 0) {
            throw new IllegalArgumentException("limit 必须大于 0");
        }
        if (dto.getLimit() != null && dto.getLimit() > MAX_LIMIT) {
            throw new IllegalArgumentException("limit 不能超过 " + MAX_LIMIT);
        }
        if (dto.getMinScore() != null && (dto.getMinScore() < -1 || dto.getMinScore() > 1)) {
            throw new IllegalArgumentException("minScore 必须在 -1 到 1 之间");
        }
    }

    private int resolveLimit(Integer limit) {
        return limit == null ? DEFAULT_LIMIT : limit;
    }

    private List<VectorSearchRow> searchVectors(float[] queryVector, int limit, Double minScore) {
        StringBuilder sql = new StringBuilder("""
                SELECT post_id, 1 - (embedding <=> ?) AS score
                FROM %s
                """.formatted(vectorStoreProperties.getTable()));
        List<Object> params = new ArrayList<>();
        params.add(new PGvector(queryVector));

        if (minScore != null) {
            sql.append(" WHERE 1 - (embedding <=> ?) >= ?");
            params.add(new PGvector(queryVector));
            params.add(minScore);
        }

        sql.append(" ORDER BY embedding <=> ? LIMIT ?");
        params.add(new PGvector(queryVector));
        params.add(limit);

        return vectorJdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> new VectorSearchRow(
                        rs.getString("post_id"),
                        rs.getDouble("score")
                ),
                params.toArray()
        );
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

    private record VectorSearchRow(String postId, Double score) {
    }
}
