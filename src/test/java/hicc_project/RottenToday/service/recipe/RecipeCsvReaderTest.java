package hicc_project.RottenToday.service.recipe;

import com.fasterxml.jackson.databind.ObjectMapper;
import hicc_project.RottenToday.dto.RecipeDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 레시피 계획 Phase 3: 식품안전나라 COOKRCP01 CSV 스냅샷(D-020)을 RecipeDto로 읽는다
class RecipeCsvReaderTest {

    private static final Path COMMITTED_CSV = Path.of("scripts/seed/COOKRCP01.csv.gz");
    private static final String HEADER = "RCP_SEQ,RCP_NM,RCP_PAT2,INFO_ENG,RCP_PARTS_DTLS,ATT_FILE_NO_MAIN,MANUAL01,MANUAL_IMG01,MANUAL02,MANUAL_IMG02,HASH_TAG\n";

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private final RecipeCsvReader reader = new RecipeCsvReader(objectMapper);

    @TempDir
    Path dir;

    private Path csv(String name, String content) throws IOException {
        Path path = dir.resolve(name);
        Files.writeString(path, content, StandardCharsets.UTF_8);
        return path;
    }

    @Test
    void 저장소의_CSV_스냅샷을_모두_읽는다() {
        RecipeCsvReader.Result result = reader.read(COMMITTED_CSV);

        assertThat(result.recipes()).hasSize(1156);
        assertThat(result.skipped()).isEmpty();
        assertThat(new HashSet<>(result.recipes().stream().map(RecipeDto::getRCP_SEQ).toList())).hasSize(1156);
        RecipeDto first = result.recipes().get(0);
        assertThat(first.getRCP_SEQ()).isEqualTo("28");
        assertThat(first.getRCP_NM()).isEqualTo("새우 두부 계란찜");
        assertThat(first.getINFO_ENG()).isNotNull();
        assertThat(first.getRecipeSteps()).isNotEmpty();
    }

    @Test
    void 따옴표_안의_줄바꿈과_쉼표를_한_칸으로_읽는다() throws IOException {
        Path path = csv("r.csv", HEADER
                + "1,두부조림,반찬,120,\"두부조림\n두부 1모, 간장 2큰술\",http://img/1.jpg,1. 두부를 썬다.,http://img/s1.jpg,\"2. 굽는다, 졸인다.\",,#두부\n");

        RecipeDto dto = reader.read(path).recipes().get(0);

        assertThat(dto.getRCP_PARTS_DTLS()).isEqualTo("두부조림\n두부 1모, 간장 2큰술");
        assertThat(dto.getINFO_ENG()).isEqualTo(120.0);
        assertThat(dto.getRecipeSteps()).extracting(step -> step.getDescription())
                .containsExactly("1. 두부를 썬다.", "2. 굽는다, 졸인다.");
    }

    @Test
    void BOM이_있어도_첫_열_이름을_인식한다() throws IOException {
        Path path = csv("bom.csv", "﻿" + HEADER + "1,두부조림,반찬,120,두부 1모,,,,,,\n");

        assertThat(reader.read(path).recipes()).extracting(RecipeDto::getRCP_SEQ).containsExactly("1");
    }

    @Test
    void gzip_압축_파일을_읽는다() throws IOException {
        Path path = dir.resolve("r.csv.gz");
        try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(path))) {
            out.write((HEADER + "1,두부조림,반찬,120,두부 1모,,,,,,\n").getBytes(StandardCharsets.UTF_8));
        }

        assertThat(reader.read(path).recipes()).hasSize(1);
    }

    @Test
    void 빈_숫자_칸은_null로_읽는다() throws IOException {
        Path path = csv("r.csv", HEADER + "1,두부조림,반찬,,두부 1모,,,,,,\n");

        assertThat(reader.read(path).recipes().get(0).getINFO_ENG()).isNull();
    }

    @Test
    void 일련번호가_없거나_중복인_행은_건너뛰고_줄_번호를_알려준다() throws IOException {
        Path path = csv("r.csv", HEADER
                + "1,두부조림,반찬,120,두부 1모,,,,,,\n"
                + ",이름만,반찬,120,두부 1모,,,,,,\n"
                + "1,두부조림 중복,반찬,120,두부 1모,,,,,,\n"
                + "2,감자볶음,반찬,90,감자 1개,,,,,,\n");

        RecipeCsvReader.Result result = reader.read(path);

        assertThat(result.recipes()).extracting(RecipeDto::getRCP_NM).containsExactly("두부조림", "감자볶음");
        assertThat(result.skipped()).extracting(RecipeCsvReader.Skipped::recordNumber).containsExactly(2L, 3L);
    }

    @Test
    void 숫자_칸에_숫자가_아닌_값이_있으면_그_행만_건너뛴다() throws IOException {
        Path path = csv("r.csv", HEADER
                + "1,두부조림,반찬,열량없음,두부 1모,,,,,,\n"
                + "2,감자볶음,반찬,90,감자 1개,,,,,,\n");

        RecipeCsvReader.Result result = reader.read(path);

        assertThat(result.recipes()).extracting(RecipeDto::getRCP_SEQ).containsExactly("2");
        assertThat(result.skipped()).extracting(RecipeCsvReader.Skipped::rcpSeq).containsExactly("1");
    }

    @Test
    void 파일이_없으면_예외() {
        assertThatThrownBy(() -> reader.read(dir.resolve("없음.csv")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("없음.csv");
    }

    @Test
    void 필수_열이_없으면_예외() throws IOException {
        Path path = csv("r.csv", "RCP_NM,RCP_PARTS_DTLS\n두부조림,두부 1모\n");

        assertThatThrownBy(() -> reader.read(path))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RCP_SEQ");
    }

    @Test
    void 빈_파일은_빈_결과() throws IOException {
        Path path = csv("r.csv", HEADER);

        assertThat(reader.read(path).recipes()).isEqualTo(List.of());
    }
}
