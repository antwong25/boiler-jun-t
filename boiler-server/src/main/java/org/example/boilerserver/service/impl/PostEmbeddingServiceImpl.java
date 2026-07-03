package org.example.boilerserver.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgvector.PGvector;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.example.boilerpojo.BoilerEntity;
import org.example.boilerpojo.PostEntity;
import org.example.boilerserver.config.VectorStoreProperties;
import org.example.boilerserver.mapper.BoilerMapper;
import org.example.boilerserver.mapper.PostMapper;
import org.example.boilerserver.service.PostEmbeddingService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PostEmbeddingServiceImpl implements PostEmbeddingService {

    private final PostMapper postMapper;
    private final BoilerMapper boilerMapper;
    private final ObjectMapper objectMapper;
    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate vectorJdbcTemplate;
    private final VectorStoreProperties vectorStoreProperties;

    public PostEmbeddingServiceImpl(
            PostMapper postMapper,
            BoilerMapper boilerMapper,
            ObjectMapper objectMapper,
            EmbeddingModel embeddingModel,
            @Qualifier("vectorJdbcTemplate") JdbcTemplate vectorJdbcTemplate,
            VectorStoreProperties vectorStoreProperties
    ) {
        this.postMapper = postMapper;
        this.boilerMapper = boilerMapper;
        this.objectMapper = objectMapper;
        this.embeddingModel = embeddingModel;
        this.vectorJdbcTemplate = vectorJdbcTemplate;
        this.vectorStoreProperties = vectorStoreProperties;
    }

    @Override
    public void vectorizePost(String postId) {
        PostEntity postEntity = getExistingPost(postId);
        BoilerEntity boilerEntity = getExistingBoiler(postEntity.getBoilerId());
        String content = buildVectorContent(postEntity, boilerEntity);

        Embedding embedding = embeddingModel.embed(content).content();
        upsertEmbedding(postEntity.getPostId(), content, embedding.vector());
    }

    @Override
    public void deletePostVector(String postId) {
        if (!StringUtils.hasText(postId)) {
            throw new IllegalArgumentException("帖子ID不能为空");
        }
        String sql = "DELETE FROM %s WHERE post_id = ?".formatted(vectorStoreProperties.getTable());
        vectorJdbcTemplate.update(sql, postId.trim());
    }

    private void upsertEmbedding(String postId, String content, float[] vector) {
        String sql = """
                INSERT INTO %s (post_id, content, embedding, created_at, updated_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (post_id) DO UPDATE
                SET content = EXCLUDED.content,
                    embedding = EXCLUDED.embedding,
                    updated_at = CURRENT_TIMESTAMP
                """.formatted(vectorStoreProperties.getTable());
        vectorJdbcTemplate.update(sql, postId, content, new PGvector(vector));
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

    private String buildVectorContent(PostEntity postEntity, BoilerEntity boilerEntity) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("postId", postEntity.getPostId());
        payload.put("sellerId", postEntity.getSellerId());
        payload.put("title", postEntity.getTitle());
        payload.put("price", postEntity.getPrice());
        payload.put("description", postEntity.getDescription());
        payload.put("status", postEntity.getStatus());
        payload.put("publishTime", postEntity.getPublishTime());
        payload.put("updateTime", postEntity.getUpdateTime());
        payload.put("viewCount", postEntity.getViewCount());
        payload.put("mediaFiles", postEntity.getMediaFiles());
        payload.put("aiValuationRange", postEntity.getAiValuationRange());
        payload.put("city", postEntity.getCity());

        Map<String, Object> boilerDetail = new LinkedHashMap<>();
        boilerDetail.put("boilerId", boilerEntity.getBoilerId());
        boilerDetail.put("model", boilerEntity.getModel());
        boilerDetail.put("brand", boilerEntity.getBrand());
        boilerDetail.put("boilerType", boilerEntity.getBoilerType());
        boilerDetail.put("tonnage", boilerEntity.getTonnage());
        boilerDetail.put("fuelType", boilerEntity.getFuelType());
        boilerDetail.put("workingPressure", boilerEntity.getWorkingPressure());
        boilerDetail.put("noxEmissions", boilerEntity.getNoxEmissions());
        boilerDetail.put("footprintArea", boilerEntity.getFootprintArea());
        boilerDetail.put("manufactureDate", boilerEntity.getManufactureYear());
        boilerDetail.put("evaporationCapacity", boilerEntity.getEvaporationCapacity());
        boilerDetail.put("ratedThermalPower", boilerEntity.getRatedThermalPower());
        boilerDetail.put("thermalEfficiency", boilerEntity.getThermalEfficiency());
        boilerDetail.put("equipmentCondition", boilerEntity.getEquipmentCondition());
        boilerDetail.put("usageHours", boilerEntity.getUsageHours());
        boilerDetail.put("testReport", boilerEntity.getTestReport());
        boilerDetail.put("ratedOutletWaterTemperature", boilerEntity.getRatedOutletWaterTemperature());
        payload.put("boilerDetail", boilerDetail);

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("帖子内容序列化失败", ex);
        }
    }
}
