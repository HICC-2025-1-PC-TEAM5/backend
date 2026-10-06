package hicc_project.RottenToday.service.recipe;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

/**
 * 동의어 사전 저장 방식 비교 측정 (메모리 HashMap vs MySQL 인덱스). 평소 테스트에서는 돌지 않는다.
 * 추천 1회 = 냉장고 재료 20개 정규화. 같은 전처리(lookupKey) 뒤 사전 조회 방식만 다르게 한다.
 * 실행: backend/scripts/bench/alias-bench.sh (D-009 리소스 제한 컨테이너에서 compose MySQL에 접속)
 * 결과: build/reports/alias-bench/result.txt
 */
@EnabledIfEnvironmentVariable(named = "ALIAS_BENCH", matches = "1")
class AliasLookupBenchmark {

    private static final String TABLE = "bench_ingredient_alias";
    private static final Path OUT = Path.of("build/reports/alias-bench");

    // 냉장고 재료 예시 20개: 별칭(계란, 쇠고기 …)과 기준 이름(양파, 마늘 …), 사전에 없는 이름(두부 …)을 섞었다
    private static final List<String> FRIDGE = List.of(
            "계란", "쇠고기", "후춧가루", "양파", "마늘", "대파", "브로컬리", "두부", "다진 마늘", "감자",
            "풋고추", "샐러리", "우유", "돼지고기", "표고", "애호박", "새송이", "버터", "당근", "고추가루");

    private final int warmup = intEnv("BENCH_WARMUP", 300);
    private final int iterations = intEnv("BENCH_N", 2000);

    @Test
    void 사전_저장_방식별_조회_시간을_잰다() throws Exception {
        Map<String, String> aliases = RecipeIngredientParser.loadAliases();
        RecipeIngredientParser parser = new RecipeIngredientParser(aliases);
        List<String> report = new ArrayList<>();
        report.add(String.format("aliases=%d fridgeNames=%d warmup=%d iterations=%d cpus=%d maxMemMB=%d",
                aliases.size(), FRIDGE.size(), warmup, iterations,
                Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().maxMemory() / 1024 / 1024));

        // 1) 메모리 HashMap (실제 코드 경로 normalize)
        List<String> expected = FRIDGE.stream().map(parser::normalize).toList();
        report.add(measure("memory-hashmap", () -> {
            List<String> out = new ArrayList<>(FRIDGE.size());
            for (String n : FRIDGE) out.add(parser.normalize(n));
            return out;
        }, expected));

        try (Connection con = DriverManager.getConnection(env("BENCH_DB_URL"), env("BENCH_DB_USER"), env("BENCH_DB_PASSWORD"))) {
            setUpTable(con, aliases);
            try {
                // 2) DB 인덱스 조회, 재료마다 1번 (커넥션 하나를 계속 씀 = 커넥션 풀에서 이미 빌린 상태, DB에 가장 유리한 조건)
                try (PreparedStatement ps = con.prepareStatement("SELECT canonical FROM " + TABLE + " WHERE alias = ?")) {
                    report.add(measure("mysql-index-per-name", () -> {
                        List<String> out = new ArrayList<>(FRIDGE.size());
                        for (String n : FRIDGE) {
                            String key = RecipeIngredientParser.lookupKey(n);
                            ps.setString(1, key);
                            try (ResultSet rs = ps.executeQuery()) {
                                out.add(canonicalOrKey(rs.next() ? rs.getString(1) : null, key));
                            }
                        }
                        return out;
                    }, expected));
                }

                // 3) DB 인덱스 조회, IN 쿼리 1번으로 묶음
                String in = String.join(",", Collections.nCopies(FRIDGE.size(), "?"));
                try (PreparedStatement ps = con.prepareStatement("SELECT alias, canonical FROM " + TABLE + " WHERE alias IN (" + in + ")")) {
                    report.add(measure("mysql-index-in-query", () -> {
                        List<String> keys = FRIDGE.stream().map(RecipeIngredientParser::lookupKey).toList();
                        for (int i = 0; i < keys.size(); i++) ps.setString(i + 1, keys.get(i));
                        Map<String, String> found = new HashMap<>();
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) found.put(rs.getString(1), rs.getString(2));
                        }
                        List<String> out = new ArrayList<>(keys.size());
                        for (String k : keys) out.add(canonicalOrKey(found.get(k), k));
                        return out;
                    }, expected));
                }

                // 참고: 인덱스가 실제로 쓰이는지
                try (Statement st = con.createStatement();
                     ResultSet rs = st.executeQuery("EXPLAIN SELECT canonical FROM " + TABLE + " WHERE alias = '계란'")) {
                    rs.next();
                    report.add("explain type=" + rs.getString("type") + " key=" + rs.getString("key"));
                }
            } finally {
                try (Statement st = con.createStatement()) {
                    st.execute("DROP TABLE IF EXISTS " + TABLE);
                }
            }
        }

        Files.createDirectories(OUT);
        Files.write(OUT.resolve("result.txt"), report, StandardCharsets.UTF_8);
        report.forEach(System.out::println);
    }

    private static String canonicalOrKey(String canonical, String key) {
        String c = canonical != null ? canonical : key;
        return RecipeIngredientParser.IGNORE.equals(c) ? "" : c;
    }

    private static void setUpTable(Connection con, Map<String, String> aliases) throws Exception {
        try (Statement st = con.createStatement()) {
            st.execute("DROP TABLE IF EXISTS " + TABLE);
            st.execute("CREATE TABLE " + TABLE + " (alias VARCHAR(100) NOT NULL PRIMARY KEY, canonical VARCHAR(100) NOT NULL)"
                    + " DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
        }
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO " + TABLE + " (alias, canonical) VALUES (?, ?)")) {
            for (var e : aliases.entrySet()) {
                ps.setString(1, e.getKey());
                ps.setString(2, e.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    @FunctionalInterface
    interface Lookup {
        List<String> run() throws Exception;
    }

    private String measure(String label, Lookup lookup, List<String> expected) throws Exception {
        for (int i = 0; i < warmup; i++) lookup.run();
        long[] ns = new long[iterations];
        for (int i = 0; i < iterations; i++) {
            long t = System.nanoTime();
            List<String> out = lookup.run();
            ns[i] = System.nanoTime() - t;
            if (!out.equals(expected)) throw new AssertionError(label + " 결과가 메모리 조회와 다름: " + out);
        }
        Arrays.sort(ns);
        double avgUs = Arrays.stream(ns).average().orElse(0) / 1000.0;
        return String.format("%-22s per-recommendation(20 names): avg=%.1fus p50=%.1fus p95=%.1fus p99=%.1fus max=%.1fus",
                label, avgUs, ns[iterations / 2] / 1000.0, ns[(int) (iterations * 0.95)] / 1000.0,
                ns[(int) (iterations * 0.99)] / 1000.0, ns[iterations - 1] / 1000.0);
    }

    private static String env(String name) {
        String v = System.getenv(name);
        if (v == null || v.isBlank()) throw new IllegalStateException(name + " 환경변수가 필요합니다");
        return v;
    }

    private static int intEnv(String name, int def) {
        String v = System.getenv(name);
        return v == null || v.isBlank() ? def : Integer.parseInt(v);
    }
}
