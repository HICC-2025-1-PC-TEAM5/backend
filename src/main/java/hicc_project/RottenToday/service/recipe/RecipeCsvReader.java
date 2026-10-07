package hicc_project.RottenToday.service.recipe;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import hicc_project.RottenToday.dto.RecipeDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 * 식품안전나라 COOKRCP01 CSV 스냅샷(D-020)을 RecipeDto 목록으로 읽는다. 열 이름은 API 응답 키와 같다.
 * .gz로 끝나면 압축을 풀어 읽는다. 외부 API를 호출하지 않는다 (레시피 계획 Phase 3).
 */
@Slf4j
@Component
public class RecipeCsvReader {

    static final List<String> REQUIRED_COLUMNS = List.of("RCP_SEQ", "RCP_NM", "RCP_PARTS_DTLS");

    /** 읽은 레시피와, 일련번호가 없거나 중복이거나 값 형식이 잘못돼 건너뛴 행 */
    public record Result(List<RecipeDto> recipes, List<Skipped> skipped) {}

    /** recordNumber는 헤더를 뺀 1부터의 행 번호 (따옴표 안 줄바꿈이 있어 파일 줄 번호와 다를 수 있다) */
    public record Skipped(long recordNumber, String rcpSeq, String reason) {}

    private final ObjectMapper objectMapper;

    public RecipeCsvReader(ObjectMapper objectMapper) {
        // CSV에는 RecipeDto에 없는 열(RCP_WAY2, HASH_TAG …)도 있다
        this.objectMapper = objectMapper.copy().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public Result read(Path path) {
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("레시피 CSV 파일이 없습니다: " + path.getFileName());
        }
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build();
        try (Reader in = open(path); CSVParser parser = format.parse(in)) {
            List<String> missing = REQUIRED_COLUMNS.stream().filter(c -> !parser.getHeaderNames().contains(c)).toList();
            if (!missing.isEmpty()) {
                throw new IllegalArgumentException("레시피 CSV에 필요한 열이 없습니다: " + String.join(", ", missing));
            }

            List<RecipeDto> recipes = new ArrayList<>();
            List<Skipped> skipped = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (CSVRecord record : parser) {
                Map<String, String> row = record.toMap();
                String rcpSeq = row.get("RCP_SEQ") == null ? "" : row.get("RCP_SEQ").trim();
                if (rcpSeq.isEmpty()) {
                    skipped.add(new Skipped(record.getRecordNumber(), rcpSeq, "일련번호 없음"));
                    continue;
                }
                if (!seen.add(rcpSeq)) {
                    skipped.add(new Skipped(record.getRecordNumber(), rcpSeq, "일련번호 중복"));
                    continue;
                }
                try {
                    recipes.add(objectMapper.convertValue(row, RecipeDto.class));
                } catch (IllegalArgumentException e) {
                    // 숫자 칸(INFO_*)에 숫자가 아닌 값 등. 그 행만 건너뛰고 나머지는 읽는다
                    skipped.add(new Skipped(record.getRecordNumber(), rcpSeq, "값 형식 오류"));
                }
            }
            if (!skipped.isEmpty()) {
                log.warn("레시피 CSV에서 건너뛴 행 {}개: {}", skipped.size(), skipped);
            }
            return new Result(recipes, skipped);
        } catch (IOException e) {
            throw new UncheckedIOException("레시피 CSV를 읽지 못했습니다: " + path.getFileName(), e);
        }
    }

    private static Reader open(Path path) throws IOException {
        InputStream in = Files.newInputStream(path);
        if (path.getFileName().toString().endsWith(".gz")) {
            in = new GZIPInputStream(in);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        // 엑셀 등에서 저장한 UTF-8 BOM이 있으면 첫 열 이름이 "﻿RCP_SEQ"가 되므로 건너뛴다
        reader.mark(1);
        if (reader.read() != '﻿') {
            reader.reset();
        }
        return reader;
    }
}
