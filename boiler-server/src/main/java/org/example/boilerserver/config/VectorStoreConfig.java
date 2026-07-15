package org.example.boilerserver.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@Configuration
@EnableConfigurationProperties(VectorStoreProperties.class)
public class VectorStoreConfig {
    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    @Bean
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    @Bean(name = "vectorJdbcTemplate")
    public JdbcTemplate vectorJdbcTemplate(
            VectorStoreProperties properties,
            EmbeddingModel embeddingModel
    ) {
        String tableName = properties.getTable();
        if (!TABLE_NAME_PATTERN.matcher(tableName).matches()) {
            throw new IllegalStateException("app.vector.table 配置非法");
        }

        DriverManagerDataSource vectorDataSource = buildVectorDataSource(properties);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(vectorDataSource);
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s (
                    post_id VARCHAR(64) PRIMARY KEY,
                    content TEXT NOT NULL,
                    embedding VECTOR(%s) NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(tableName, embeddingModel.dimension()));
        return jdbcTemplate;
    }

    private DriverManagerDataSource buildVectorDataSource(VectorStoreProperties properties) {
        VectorStoreProperties.Datasource datasource = properties.getDatasource();
        if (!StringUtils.hasText(datasource.getUrl())) {
            throw new IllegalStateException("app.vector.datasource.url 未配置");
        }
        if (!StringUtils.hasText(datasource.getUsername())) {
            throw new IllegalStateException("app.vector.datasource.username 未配置");
        }

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(datasource.getDriverClassName());
        dataSource.setUrl(datasource.getUrl());
        dataSource.setUsername(datasource.getUsername());
        dataSource.setPassword(datasource.getPassword());
        return dataSource;
    }
}
