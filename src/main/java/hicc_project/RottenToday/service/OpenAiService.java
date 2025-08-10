package hicc_project.RottenToday.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import hicc_project.RottenToday.config.OpenAiProperties;
import hicc_project.RottenToday.dto.*;
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
import java.util.List;

@Slf4j
@Service
public class OpenAiService {

    private final OpenAiProperties openAiProperties;
    private final WebClient webClient;

    public OpenAiService(OpenAiProperties openAiProperties, WebClient.Builder webClientBuilder) {
        this.openAiProperties = openAiProperties;
        this.webClient = webClientBuilder.baseUrl("https://api.openai.com").build();
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
            log.debug("요청 데이터: {}", request);

            ChatResponse response = webClient.post()
                    .uri("/v1/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiProperties.getKey().trim())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), clientResponse -> {
                        log.error("GPT API 클라이언트 오류 발생: {}", clientResponse.statusCode());
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("오류 응답 본문: {}", errorBody);
                                    return Mono.error(new RuntimeException("GPT API 클라이언트 오류 (" +
                                            clientResponse.statusCode() + "): " + errorBody));
                                });
                    })
                    .onStatus(status -> status.is5xxServerError(), serverResponse -> {
                        log.error("GPT API 서버 오류 발생: {}", serverResponse.statusCode());
                        return serverResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("서버 오류 응답 본문: {}", errorBody);
                                    return Mono.error(new RuntimeException("GPT API 서버 오류 (" +
                                            serverResponse.statusCode() + "): " + errorBody));
                                });
                    })
                    .bodyToMono(ChatResponse.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            log.info("GPT API 응답 수신 완료 = {}", response);



            String content = response.getChoices().get(0).getMessage().getContent();
            ObjectMapper mapper = new ObjectMapper();
            List<IngredientDto> ingredients =
                    mapper.readValue(content, new TypeReference<List<IngredientDto>>() {});
            log.info("GPT API 호출 성공 - 응답 길이: {}", content.length());
            return ingredients;

        } catch (WebClientRequestException e) {
            log.error("GPT API 요청 전송 실패", e);
            throw new RuntimeException("GPT API 요청 전송 실패: " + e.getMessage(), e);
        } catch (WebClientResponseException e) {
            log.error("GPT API 응답 오류 - 상태코드: {}, 응답본문: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("GPT API 응답 오류 (" + e.getStatusCode() + "): " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("GPT API 호출 중 예상치 못한 오류", e);
            throw new RuntimeException("GPT API 호출 중 예상치 못한 오류 발생: " + e.getMessage(), e);
        }
    }
}