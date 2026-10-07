package hicc_project.RottenToday.config;

import hicc_project.RottenToday.service.recipe.RecipeCsvReader;
import hicc_project.RottenToday.service.recipe.RecipeImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * recipe-import 프로파일일 때만 레시피 CSV를 DB에 적재하고 종료한다 (레시피 계획 Phase 4, D-020).
 * 종료 코드: 건너뛴 행·실패가 없으면 0, 있으면 1. 같은 CSV로 다시 실행해도 결과가 같다.
 */
@Slf4j
@Component
@Profile("recipe-import")
@RequiredArgsConstructor
public class RecipeImportRunner implements ApplicationRunner {

    private final RecipeCsvReader csvReader;
    private final RecipeImportService importService;
    private final ConfigurableApplicationContext context;

    @Value("${recipe.import.csv-path:scripts/seed/COOKRCP01.csv.gz}")
    private String csvPath;

    @Override
    public void run(ApplicationArguments args) {
        long start = System.nanoTime();
        RecipeCsvReader.Result csv = csvReader.read(Path.of(csvPath));
        RecipeImportService.Summary summary = importService.importRecipes(csv.recipes());
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        log.info("recipe.import total={} created={} updated={} skippedRows={} failed={} elapsedMs={}",
                summary.total(), summary.created(), summary.updated(), csv.skipped().size(), summary.failed().size(), elapsedMs);
        log.info("recipe.import ingredients={} linked={} linkedRate={}%",
                summary.ingredientCount(), summary.linkedCount(), String.format("%.1f", summary.linkedRate() * 100));
        log.info("recipe.import 마스터 미연결 상위 {}개: {}", summary.topUnlinked().size(), summary.topUnlinked());
        if (!summary.failed().isEmpty()) {
            log.warn("recipe.import 실패 RCP_SEQ: {}", summary.failed());
        }

        int exitCode = summary.failed().isEmpty() && csv.skipped().isEmpty() ? 0 : 1;
        System.exit(SpringApplication.exit(context, () -> exitCode));
    }
}
