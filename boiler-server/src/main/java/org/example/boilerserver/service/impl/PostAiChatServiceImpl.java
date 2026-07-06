package org.example.boilerserver.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import org.example.boilerpojo.AiChatMessageDTO;
import org.example.boilerpojo.PostAiChatRequestDTO;
import org.example.boilerpojo.PostAiChatResponseVO;
import org.example.boilerpojo.PostFilterSearchDTO;
import org.example.boilerpojo.PostSearchResultVO;
import org.example.boilerpojo.PostSemanticSearchDTO;
import org.example.boilerpojo.PostSemanticSearchVO;
import org.example.constant.PostConstant;
import org.example.boilerserver.config.AzureOpenAiProperties;
import org.example.boilerserver.service.PostAiChatService;
import org.example.boilerserver.service.PostFilterSearchService;
import org.example.boilerserver.service.PostSemanticSearchService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class PostAiChatServiceImpl implements PostAiChatService {
    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 20;
    private static final int MAX_RECENT_MESSAGES = 12;
    private static final String ACTION_ASK_FOLLOW_UP = "ASK_FOLLOW_UP";
    private static final String ACTION_STRUCTURED_FILTER = "STRUCTURED_FILTER";
    private static final String ACTION_SEMANTIC_SEARCH = "SEMANTIC_SEARCH";
    private static final String SYSTEM_PROMPT = """
            You are an AI search assistant for a second-hand boiler marketplace.
            Your first task is to decide which of the following actions best fits the user's current request:
            1. ASK_FOLLOW_UP: The request is still too vague, so ask exactly one most important follow-up question to help the user clarify the need.
            2. STRUCTURED_FILTER: The user has already provided clear structured constraints such as city, boiler type, brand, or fuel type, so the request should use the filter API.
            3. SEMANTIC_SEARCH: The user describes a fuzzy, scenario-based, or natural-language need, so the request should use vector similarity search.

            Return JSON only. Do not output Markdown. Do not output explanations outside the JSON object.
            The JSON structure must be:
            {
              "action": "ASK_FOLLOW_UP | STRUCTURED_FILTER | SEMANTIC_SEARCH",
              "reply": "A user-facing reply that can be shown directly in the frontend",
              "vectorQuery": "A refined natural-language search query when action=SEMANTIC_SEARCH; otherwise empty",
              "structuredFilter": {
                "city": "City, or empty if not available",
                "boilerType": "HOT_WATER or STEAM, or empty if not available",
                "brand": "Brand, or empty if not available",
                "fuelType": "Fuel type, or empty if not available"
              }
            }

            Decision rules:
            - If the user's need is still unclear, prefer ASK_FOLLOW_UP.
            - If the user explicitly mentions structured constraints such as city, boiler type, brand, or fuel type, prefer STRUCTURED_FILTER.
            - If the user describes fuzzy intent such as suitable scenarios, condition, budget friendliness, energy saving, urgency, or value for money, prefer SEMANTIC_SEARCH.
            - boilerType must be either HOT_WATER or STEAM.
            - If the user speaks Chinese, reply in Chinese.
            - If the user speaks English, reply in English.
            - The reply must be natural, concise, and ready to display to the user.
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AzureOpenAiProperties azureOpenAiProperties;
    private final PostSemanticSearchService postSemanticSearchService;
    private final PostFilterSearchService postFilterSearchService;

    public PostAiChatServiceImpl(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            AzureOpenAiProperties azureOpenAiProperties,
            PostSemanticSearchService postSemanticSearchService,
            PostFilterSearchService postFilterSearchService
    ) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.azureOpenAiProperties = azureOpenAiProperties;
        this.postSemanticSearchService = postSemanticSearchService;
        this.postFilterSearchService = postFilterSearchService;
    }

    @Override
    public PostAiChatResponseVO chat(PostAiChatRequestDTO dto) {
        validateRequest(dto);

        AiDecision decision = requestDecision(dto);
        return executeDecision(decision, dto);
    }

    private void validateRequest(PostAiChatRequestDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("对话请求不能为空");
        }
        if (!StringUtils.hasText(dto.getCurrentUserInput())) {
            throw new IllegalArgumentException("当前用户输入不能为空");
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

    private AiDecision requestDecision(PostAiChatRequestDTO dto) {
        ensureAiConfigReady();

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", azureOpenAiProperties.getModel());
        requestBody.put("temperature", 0.2);
        requestBody.set("messages", buildMessages(dto));

        String responseBody;
        try {
            String requestUrl = Objects.requireNonNull(buildChatCompletionsUrl());
            responseBody = restClient.post()
                    .uri(requestUrl)
                    .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                    .header("api-key", azureOpenAiProperties.getApiKey().trim())
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException("调用 Azure OpenAI 失败: " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            throw new IllegalStateException("调用 Azure OpenAI 失败: " + ex.getMessage(), ex);
        }

        return parseDecision(responseBody);
    }

    private ArrayNode buildMessages(PostAiChatRequestDTO dto) {
        ArrayNode messages = objectMapper.createArrayNode();
        messages.add(createMessage("system", SYSTEM_PROMPT));

        List<AiChatMessageDTO> recentMessages = dto.getRecentMessages() == null ? List.of() : dto.getRecentMessages();
        int start = Math.max(0, recentMessages.size() - MAX_RECENT_MESSAGES);
        for (int i = start; i < recentMessages.size(); i++) {
            AiChatMessageDTO message = recentMessages.get(i);
            if (message == null || !StringUtils.hasText(message.getContent())) {
                continue;
            }
            messages.add(createMessage(normalizeRole(message.getRole()), message.getContent().trim()));
        }
        messages.add(createMessage("user", dto.getCurrentUserInput().trim()));
        return messages;
    }

    private ObjectNode createMessage(String role, String content) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            return "user";
        }
        String normalized = role.trim().toLowerCase(Locale.ROOT);
        if ("assistant".equals(normalized) || "system".equals(normalized) || "user".equals(normalized)) {
            return normalized;
        }
        return "user";
    }

    private void ensureAiConfigReady() {
        if (!StringUtils.hasText(azureOpenAiProperties.getEndpoint())
                || !StringUtils.hasText(azureOpenAiProperties.getApiKey())
                || !StringUtils.hasText(azureOpenAiProperties.getApiVersion())
                || !StringUtils.hasText(azureOpenAiProperties.getModel())) {
            throw new IllegalStateException("Azure OpenAI 配置不完整，请先配置 app.ai.azure.endpoint/api-key/api-version/model");
        }
    }

    private String buildChatCompletionsUrl() {
        String endpoint = sanitizeEndpoint(azureOpenAiProperties.getEndpoint());
        if (endpoint.contains("/openai")) {
            return endpoint + "/deployments/" + azureOpenAiProperties.getModel().trim()
                    + "/chat/completions?api-version=" + azureOpenAiProperties.getApiVersion().trim();
        }
        return endpoint + "/openai/deployments/" + azureOpenAiProperties.getModel().trim()
                + "/chat/completions?api-version=" + azureOpenAiProperties.getApiVersion().trim();
    }

    private String sanitizeEndpoint(String endpoint) {
        String sanitized = endpoint.trim().replace("`", "");
        if ((sanitized.startsWith("\"") && sanitized.endsWith("\""))
                || (sanitized.startsWith("'") && sanitized.endsWith("'"))) {
            sanitized = sanitized.substring(1, sanitized.length() - 1);
        }
        while (sanitized.endsWith("/")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }
        return sanitized.trim();
    }

    private AiDecision parseDecision(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices").path(0).path("message").path("content").asText(null);
            if (!StringUtils.hasText(content)) {
                throw new IllegalStateException("AI模型未返回有效内容");
            }
            return objectMapper.readValue(extractJson(content), AiDecision.class);
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("AI返回格式无法解析，请检查模型输出是否为 JSON", ex);
        }
    }

    private String extractJson(String content) {
        String cleaned = content.trim();
        if (cleaned.startsWith("```")) {
            int firstNewLine = cleaned.indexOf('\n');
            int lastFence = cleaned.lastIndexOf("```");
            if (firstNewLine > -1 && lastFence > firstNewLine) {
                cleaned = cleaned.substring(firstNewLine + 1, lastFence).trim();
            }
        }
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new IllegalStateException("AI未返回 JSON 对象");
        }
        return cleaned.substring(start, end + 1);
    }

    private PostAiChatResponseVO executeDecision(AiDecision decision, PostAiChatRequestDTO dto) {
        String action = normalizeAction(decision == null ? null : decision.getAction());
        PostFilterSearchDTO structuredFilter = normalizeStructuredFilter(
                decision == null ? null : decision.getStructuredFilter(),
                dto.getLimit()
        );

        PostAiChatResponseVO response = new PostAiChatResponseVO();
        response.setAction(action);
        response.setAssistantReply(resolveReply(action, decision));
        response.setStructuredFilter(structuredFilter);
        response.setVectorQuery(trimToNull(decision == null ? null : decision.getVectorQuery()));

        List<PostSearchResultVO> results = new ArrayList<>();
        if (ACTION_STRUCTURED_FILTER.equals(action)) {
            if (structuredFilter == null) {
                response.setAction(ACTION_ASK_FOLLOW_UP);
                response.setAssistantReply("我还需要再确认一个条件，比如城市或锅炉类别，才能帮你精准筛选。");
            } else {
                results = searchStructuredFilter(structuredFilter);
            }
        } else if (ACTION_SEMANTIC_SEARCH.equals(action)) {
            results = semanticSearch(dto, response.getVectorQuery(), structuredFilter);
        }

        response.setResults(results);
        response.setResultCount(results.size());
        if ((ACTION_STRUCTURED_FILTER.equals(response.getAction()) || ACTION_SEMANTIC_SEARCH.equals(response.getAction()))
                && results.isEmpty()
                && !StringUtils.hasText(response.getAssistantReply())) {
            response.setAssistantReply("我先帮你查过了，暂时没有找到特别匹配的帖子，你可以再补充一下预算、城市或锅炉类别。");
        }
        return response;
    }

    private String normalizeAction(String action) {
        if (!StringUtils.hasText(action)) {
            return ACTION_ASK_FOLLOW_UP;
        }
        String normalized = action.trim().toUpperCase(Locale.ROOT);
        if (ACTION_STRUCTURED_FILTER.equals(normalized) || ACTION_SEMANTIC_SEARCH.equals(normalized)) {
            return normalized;
        }
        return ACTION_ASK_FOLLOW_UP;
    }

    private String resolveReply(String action, AiDecision decision) {
        String reply = trimToNull(decision == null ? null : decision.getReply());
        if (reply != null) {
            return reply;
        }
        if (ACTION_STRUCTURED_FILTER.equals(action) || ACTION_SEMANTIC_SEARCH.equals(action)) {
            return "我先根据你刚才的需求帮你查一批匹配的帖子。";
        }
        return "为了帮你找得更准，我还想再确认一个最关键的条件。";
    }

    private PostFilterSearchDTO normalizeStructuredFilter(PostFilterSearchDTO filter, Integer limit) {
        if (filter == null) {
            return null;
        }
        PostFilterSearchDTO normalized = new PostFilterSearchDTO();
        normalized.setCity(normalizeCity(filter.getCity()));
        normalized.setBrand(trimToNull(filter.getBrand()));
        normalized.setFuelType(trimToNull(filter.getFuelType()));
        normalized.setBoilerType(normalizeBoilerType(filter.getBoilerType()));
        normalized.setLimit(resolveLimit(limit));
        if (!StringUtils.hasText(normalized.getCity())
                && !StringUtils.hasText(normalized.getBoilerType())
                && !StringUtils.hasText(normalized.getBrand())
                && !StringUtils.hasText(normalized.getFuelType())) {
            return null;
        }
        return normalized;
    }

    private String normalizeBoilerType(String boilerType) {
        if (!StringUtils.hasText(boilerType)) {
            return null;
        }
        String normalized = boilerType.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        if ("HOT_WATER_BOILER".equals(normalized)) {
            return PostConstant.BOILER_TYPE_HOT_WATER;
        }
        if ("STEAM_BOILER".equals(normalized)) {
            return PostConstant.BOILER_TYPE_STEAM;
        }
        if (PostConstant.BOILER_TYPE_HOT_WATER.equals(normalized)
                || PostConstant.BOILER_TYPE_STEAM.equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private int resolveLimit(Integer limit) {
        return limit == null ? DEFAULT_LIMIT : limit;
    }

    private List<PostSearchResultVO> searchStructuredFilter(PostFilterSearchDTO structuredFilter) {
        if (!StringUtils.hasText(structuredFilter.getCity())) {
            return postFilterSearchService.search(structuredFilter);
        }

        List<String> cityKeywords = buildCitySearchKeywords(structuredFilter.getCity());
        if (cityKeywords.isEmpty()) {
            return postFilterSearchService.search(structuredFilter);
        }

        Map<String, PostSearchResultVO> mergedResults = new LinkedHashMap<>();
        for (String cityKeyword : cityKeywords) {
            PostFilterSearchDTO searchDTO = copyStructuredFilter(structuredFilter);
            searchDTO.setCity(cityKeyword);
            List<PostSearchResultVO> partialResults = postFilterSearchService.search(searchDTO);
            for (PostSearchResultVO result : partialResults) {
                if (result == null || !StringUtils.hasText(result.getPostId())) {
                    continue;
                }
                mergedResults.putIfAbsent(result.getPostId(), result);
                if (mergedResults.size() >= resolveLimit(structuredFilter.getLimit())) {
                    return new ArrayList<>(mergedResults.values());
                }
            }
        }
        return new ArrayList<>(mergedResults.values());
    }

    private PostFilterSearchDTO copyStructuredFilter(PostFilterSearchDTO source) {
        PostFilterSearchDTO target = new PostFilterSearchDTO();
        target.setCity(source.getCity());
        target.setBoilerType(source.getBoilerType());
        target.setBrand(source.getBrand());
        target.setFuelType(source.getFuelType());
        target.setLimit(source.getLimit());
        return target;
    }

    private List<String> buildCitySearchKeywords(String city) {
        String rawCity = trimToNull(city);
        if (rawCity == null) {
            return List.of();
        }

        CityAlias cityAlias = resolveCityAlias(rawCity);
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        if (StringUtils.hasText(cityAlias.getEnglishUpper())) {
            keywords.add(cityAlias.getEnglishUpper());
        }
        if (StringUtils.hasText(cityAlias.getSimplifiedChinese())) {
            keywords.add(cityAlias.getSimplifiedChinese());
        }
        if (keywords.isEmpty()) {
            keywords.add(normalizeCity(rawCity));
        }
        return new ArrayList<>(keywords);
    }

    private CityAlias resolveCityAlias(String city) {
        CityAlias cityAlias = new CityAlias();
        if (containsEnglishLetter(city)) {
            cityAlias.setEnglishUpper(normalizeCity(city));
        }
        if (containsChineseCharacter(city)) {
            cityAlias.setSimplifiedChinese(trimToNull(city));
        }

        CityAlias aiAlias = requestCityAlias(city);
        if (!StringUtils.hasText(cityAlias.getEnglishUpper())) {
            cityAlias.setEnglishUpper(trimToNull(aiAlias.getEnglishUpper()));
        }
        if (!StringUtils.hasText(cityAlias.getSimplifiedChinese())) {
            cityAlias.setSimplifiedChinese(trimToNull(aiAlias.getSimplifiedChinese()));
        }
        return cityAlias;
    }

    private CityAlias requestCityAlias(String city) {
        CityAlias emptyAlias = new CityAlias();
        if (!StringUtils.hasText(city)) {
            return emptyAlias;
        }

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", azureOpenAiProperties.getModel());
        requestBody.put("temperature", 0);

        ArrayNode messages = objectMapper.createArrayNode();
        messages.add(createMessage("system", """
                You normalize one city name for search alias expansion.
                Return JSON only with this exact structure:
                {
                  "englishUpper": "STANDARDIZED ENGLISH CITY NAME IN UPPERCASE, or empty string if unknown",
                  "simplifiedChinese": "SIMPLIFIED CHINESE CITY NAME, or empty string if unknown"
                }
                Rules:
                - If the input is in English, keep the English city in uppercase and provide simplified Chinese when confident.
                - If the input is in Chinese, provide the standardized English uppercase city name and the simplified Chinese form.
                - Do not include province, district, country, explanations, or Markdown.
                - If uncertain, leave the field empty.
                """));
        messages.add(createMessage("user", city.trim()));
        requestBody.set("messages", messages);

        try {
            String requestUrl = Objects.requireNonNull(buildChatCompletionsUrl());
            String responseBody = restClient.post()
                    .uri(requestUrl)
                    .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                    .header("api-key", azureOpenAiProperties.getApiKey().trim())
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
            return objectMapper.readValue(extractJsonFromChatResponse(responseBody), CityAlias.class);
        } catch (Exception ex) {
            return emptyAlias;
        }
    }

    private String extractJsonFromChatResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        String content = root.path("choices").path(0).path("message").path("content").asText(null);
        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("AI模型未返回有效内容");
        }
        return extractJson(content);
    }

    private List<PostSearchResultVO> semanticSearch(
            PostAiChatRequestDTO dto,
            String vectorQuery,
            PostFilterSearchDTO structuredFilter
    ) {
        PostSemanticSearchDTO semanticSearchDTO = new PostSemanticSearchDTO();
        semanticSearchDTO.setQuery(StringUtils.hasText(vectorQuery) ? vectorQuery : dto.getCurrentUserInput().trim());
        semanticSearchDTO.setLimit(resolveLimit(dto.getLimit()));
        semanticSearchDTO.setMinScore(dto.getMinScore());

        List<PostSemanticSearchVO> semanticResults = postSemanticSearchService.search(semanticSearchDTO);
        List<PostSearchResultVO> adaptedResults = new ArrayList<>();
        for (PostSemanticSearchVO semanticResult : semanticResults) {
            if (structuredFilter != null && !matchesFilter(semanticResult, structuredFilter)) {
                continue;
            }
            PostSearchResultVO result = new PostSearchResultVO();
            result.setPostId(semanticResult.getPostId());
            result.setScore(semanticResult.getScore());
            result.setPost(semanticResult.getPost());
            adaptedResults.add(result);
            if (adaptedResults.size() >= resolveLimit(dto.getLimit())) {
                break;
            }
        }
        return adaptedResults;
    }

    private boolean matchesFilter(PostSemanticSearchVO result, PostFilterSearchDTO filter) {
        if (result == null || result.getPost() == null) {
            return false;
        }
        if (StringUtils.hasText(filter.getCity())) {
            String city = normalizeCity(result.getPost().getCity());
            if (!StringUtils.hasText(city) || !city.contains(filter.getCity())) {
                return false;
            }
        }
        if (result.getPost().getBoilerDetail() == null) {
            return !StringUtils.hasText(filter.getBoilerType())
                    && !StringUtils.hasText(filter.getBrand())
                    && !StringUtils.hasText(filter.getFuelType());
        }
        if (StringUtils.hasText(filter.getBoilerType())) {
            String boilerType = result.getPost().getBoilerDetail().getBoilerType();
            if (!StringUtils.hasText(boilerType) || !filter.getBoilerType().equalsIgnoreCase(boilerType)) {
                return false;
            }
        }
        if (StringUtils.hasText(filter.getBrand())) {
            String brand = result.getPost().getBoilerDetail().getBrand();
            if (!StringUtils.hasText(brand) || !brand.contains(filter.getBrand())) {
                return false;
            }
        }
        if (StringUtils.hasText(filter.getFuelType())) {
            String fuelType = result.getPost().getBoilerDetail().getFuelType();
            if (!StringUtils.hasText(fuelType) || !fuelType.contains(filter.getFuelType())) {
                return false;
            }
        }
        return true;
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

    private boolean containsEnglishLetter(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if ((text.charAt(i) >= 'a' && text.charAt(i) <= 'z')
                    || (text.charAt(i) >= 'A' && text.charAt(i) <= 'Z')) {
                return true;
            }
        }
        return false;
    }

    private boolean containsChineseCharacter(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            Character.UnicodeBlock unicodeBlock = Character.UnicodeBlock.of(text.charAt(i));
            if (Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS.equals(unicodeBlock)
                    || Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A.equals(unicodeBlock)
                    || Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B.equals(unicodeBlock)
                    || Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS.equals(unicodeBlock)) {
                return true;
            }
        }
        return false;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AiDecision {
        private String action;
        private String reply;
        private String vectorQuery;
        private PostFilterSearchDTO structuredFilter;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CityAlias {
        private String englishUpper;
        private String simplifiedChinese;
    }
}
