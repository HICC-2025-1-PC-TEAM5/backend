package hicc_project.RottenToday.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hicc_project.RottenToday.config.OpenAiProperties;
import hicc_project.RottenToday.dto.*;
import hicc_project.RottenToday.entity.Category;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class OpenAiService {

    private final OpenAiProperties openAiProperties;
    private final WebClient webClient;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public OpenAiService(OpenAiProperties openAiProperties, WebClient.Builder webClientBuilder) {
        this.openAiProperties = openAiProperties;
        this.webClient = webClientBuilder.baseUrl("https://api.openai.com").build();
    }

    public List<IngredientDto> getimagetoingredient(String message) {
        String systemPrompt = "아래 이미지 URL을 참고해, 이미지에 보이는 식재료를 모두 식별해 주세요.  \n" +
                "식재료 중 실제 식재료가 아닌 항목은 제외하고, 남은 식재료는 다음 규칙에 따라 분류해 JSON 형식으로 분류하세요.\n" +
                "\n" +
                "- 형식: [{\"name\": \"깐마늘\", \"category\": \"채소류\", \"subcategory\": \"깐마늘\"}, ...]\n" +
                "- name, category, subcategory 모두 한글로 표기 (영어 인식 시 한글로 변환)  \n" +
                "- category(대분류) 중 하나만 선택:  \n" +
                "  채소류, 과일류, 곡류/전분류, 육류, 어패류, 달걀/난류, 유제품, 두류/콩류, 기름/지방류, 조미료/향신료, 가공식품, 음료류, 기타  \n" +
                "- 육류는 subcategory에 돼지고기, 소고기, 닭고기 등 중분류까지만 표기  \n" +
                "- 육류 외 식재료는 subcategory에 식재료명 그대로 표기  \n" +
                "- 식재료가 아니면 제외  \n" +
                "- 분류 어려우면 category와 subcategory 모두 \"기타\"  \n" +
                "- name은 이미지 속 식재료명 그대로 표기 (영어는 한글 변환)" +
                "- 색깔로 출력하지 말고 형태도 같이 고려해서 알려줘" +
                "- 잘모르겠으면 null값으로 줘도 돼" +
                "- 주의 사항은 필요없음, 식재료 배열 json만 출력";

        return requestIngredients(systemPrompt, message);
    }

    public List<IngredientDto> getChatCompletion(String message) {
        String systemPrompt = "당신은 영수증에 명시된 글자들을 보고 식재료를 분류하는 AI입니다.\n" +
                "입력된 식재료에 대해 아래 JSON 형식으로 분류하세요.\n" +
                "\n" +
                "- 형식: [{\"name\": \"깐마늘\", \"category\": \"채소류\", \"subcategory\": \"깐마늘\"}, ...]\n" +
                "- 식재료명이 아니면 제외\n" +
                "- category(대분류): 채소류, 과일류, 곡류/전분류, 육류, 어패류, 달걀/난류, 유제품, 두류/콩류, 기름/지방류, 조미료/향신료, 가공식품, 음료류, 기타 중 선택\n" +
                "- 육류는 subcategory에 돼지고기, 소고기 등 중분류까지만\n" +
                "- 나머지는 subcategory에 일반화된 명칭 대신 식재료명 그대로 사용\n" +
                "- 분류 불가능하면 category = \"기타\", subcategory = \"기타\"\n" +
                "\n" +
                "예시 입력:\n" +
                "[\"깐마늘\", \"딸기\", \"돼지고기\"]\n" +
                "\n" +
                "예시 출력:\n" +
                "[\n" +
                "  {\"name\": \"깐마늘\", \"category\": \"채소류\", \"subcategory\": \"깐마늘\"},\n" +
                "  {\"name\": \"딸기\", \"category\": \"과일류\", \"subcategory\": \"딸기\"},\n" +
                "  {\"name\": \"돼지고기\", \"category\": \"육류\", \"subcategory\": \"돼지고기\"}\n" +
                "]";

        return requestIngredients(systemPrompt, message);
    }

    public List<IngredientDto> getpicturetoingredient(List<String> labels) {
        String message = labels.toString();
        String systemPrompt = "당신은 google vision api에서 이미지를 보고 반환된 글자들을 보고 식재료를 분류하는 AI입니다.\n" +
                "입력된 식재료에 대해 아래 JSON 형식으로 분류하세요.\n" +
                "\n" +
                "- 형식: [{\"name\": \"깐마늘\", \"category\": \"채소류\", \"subcategory\": \"깐마늘\"}, ...]\n" +
                "- 식재료명이 아니면 제외\n" +
                "- category(대분류): 채소류, 과일류, 곡류/전분류, 육류, 어패류, 달걀/난류, 유제품, 두류/콩류, 기름/지방류, 조미료/향신료, 가공식품, 음료류, 기타 중 선택\n" +
                "- 육류는 subcategory에 돼지고기, 소고기 등 중분류까지만\n" +
                "- 나머지는 subcategory에 일반화된 명칭 대신 식재료명 그대로 사용\n" +
                "- 분류 불가능하면 category = \"기타\", subcategory = \"기타\"\n" +
                "- 한글로 적어줘야해\n" +
                "\n" +
                "예시 입력:\n" +
                "[\"깐마늘\", \"딸기\", \"돼지고기\"]\n" +
                "\n" +
                "예시 출력:\n" +
                "[\n" +
                "  {\"name\": \"깐마늘\", \"category\": \"채소류\", \"subcategory\": \"깐마늘\"},\n" +
                "  {\"name\": \"딸기\", \"category\": \"과일류\", \"subcategory\": \"딸기\"},\n" +
                "  {\"name\": \"돼지고기\", \"category\": \"육류\", \"subcategory\": \"돼지고기\"}\n" +
                "]";

        return requestIngredients(systemPrompt, message);
    }

    /**
     * 시스템 프롬프트 + 사용자 메시지로 Chat API를 한 번 호출하고 응답을 재료 목록으로 바꾼다 (B10: 세 메서드의 같은 코드를 모음).
     * 가공식품·음료류는 name을, 나머지는 subcategory를 재료 이름으로 쓴다
     */
    private List<IngredientDto> requestIngredients(String systemPrompt, String message) {
        ChatRequest request = ChatRequest.builder()
                .model("gpt-4.1")
                .messages(List.of(
                        ChatMessage.builder()
                                .role("system")
                                .content(systemPrompt)
                                .build(),
                        ChatMessage.builder()
                                .role("user")
                                .content(message)
                                .build()
                ))
                .maxTokens(2000)
                .temperature(0.3)
                .build();


        try {
            log.info("GPT API 호출 시작 - URL: {}", openAiProperties.getUrl());

            ChatResponse response = webClient.post()
                    .uri("/v1/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiProperties.getKey().trim())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(request)
                    .retrieve()
                    // 오류 본문은 로그·예외에 넣지 않고 상태 코드와 오류 종류 코드만 남긴다 (B29, D-041, AGENTS 8장)
                    .onStatus(status -> status.isError(), errorResponse -> errorResponse.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .flatMap(errorBody -> {
                                String code = errorCodeOf(errorBody);
                                log.error("GPT API 오류 - 상태코드: {}, 오류코드: {}", errorResponse.statusCode().value(), code);
                                return Mono.error(new OpenAiCallException("GPT API 오류 ("
                                        + errorResponse.statusCode().value() + ", " + code + ")"));
                            }))
                    .bodyToMono(ChatResponse.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            String content = response.getChoices().get(0).getMessage().getContent();
            List<IngredientDto> ingredients;
            try {
                ingredients = MAPPER.readValue(content, new TypeReference<List<IngredientDto>>() {});
            } catch (JsonProcessingException e) {
                // 파서 메시지에는 응답 일부(인식 결과)가 들어 있어 남기지 않는다
                log.error("GPT 응답 JSON 파싱 실패 - 응답 길이: {}", content == null ? 0 : content.length());
                throw new OpenAiCallException("GPT 응답 JSON 파싱 실패");
            }

            List<IngredientDto> result = new ArrayList<>();
            for (IngredientDto ingredient : ingredients) {
                IngredientDto dto = toIngredient(ingredient);
                if (dto != null) result.add(dto);
            }
            log.info("GPT API 호출 성공 - 응답 길이: {}, 재료 {}개", content.length(), result.size());
            return result;

        } catch (WebClientRequestException e) {
            log.error("GPT API 요청 전송 실패: {}", e.getClass().getSimpleName());
            throw new RuntimeException("GPT API 요청 전송 실패", e);
        } catch (WebClientResponseException e) {
            log.error("GPT API 응답 오류 - 상태코드: {}", e.getStatusCode().value());
            throw new RuntimeException("GPT API 응답 오류 (" + e.getStatusCode().value() + ")", e);
        } catch (OpenAiCallException e) {
            throw e; // 위에서 로그를 남기고 만든 예외
        } catch (RuntimeException e) {
            log.error("GPT API 호출 중 예상치 못한 오류: {}", e.getClass().getSimpleName());
            throw new RuntimeException("GPT API 호출 중 예상치 못한 오류 발생", e);
        }
    }

    /**
     * AI 결과 한 항목을 재료로 바꾼다. 가공식품·음료류는 name을, 나머지는 subcategory를 이름으로 쓰고 비면 다른 쪽을 쓴다.
     * category가 비면 "기타"로 둔다(사용자가 확인 화면에서 고칠 수 있다). 이름이 둘 다 비면 등록할 수 없어 뺀다 (B29, D-041)
     */
    static IngredientDto toIngredient(IngredientDto ai) {
        if (ai == null) return null;
        String category = isBlank(ai.getCategory()) ? Category.ETC.getType() : ai.getCategory().trim();
        boolean usesName = Category.PROCESSED.getType().equals(category) || Category.DRINK.getType().equals(category);
        String name = usesName ? firstNonBlank(ai.getName(), ai.getSubcategory()) : firstNonBlank(ai.getSubcategory(), ai.getName());
        if (name == null) return null;
        IngredientDto dto = new IngredientDto();
        dto.setCategory(category);
        dto.setName(name);
        return dto;
    }

    // OpenAI 오류 본문 {"error": {"code": "...", "type": "..."}}에서 종류 코드만 꺼낸다. 메시지 문장은 쓰지 않는다
    static String errorCodeOf(String errorBody) {
        try {
            JsonNode error = MAPPER.readTree(errorBody == null ? "" : errorBody).path("error");
            String code = error.path("code").asText("");
            if (code.isBlank()) code = error.path("type").asText("");
            return code.isBlank() ? "unknown" : code;
        } catch (JsonProcessingException e) {
            return "unknown";
        }
    }

    /** OpenAI 호출·응답 처리 실패. 메시지에는 상태 코드·오류 종류 코드만 넣는다 (GlobalExceptionHandler에서 500) */
    static class OpenAiCallException extends RuntimeException {
        OpenAiCallException(String message) {
            super(message);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String firstNonBlank(String a, String b) {
        if (!isBlank(a)) return a.trim();
        if (!isBlank(b)) return b.trim();
        return null;
    }
}
