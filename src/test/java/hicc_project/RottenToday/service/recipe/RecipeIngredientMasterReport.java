package hicc_project.RottenToday.service.recipe;

import hicc_project.RottenToday.service.recipe.RecipeIngredientParser.ParsedIngredient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * 동의어 사전을 만들고 고칠 때 쓰는 리포트 (D-021). 평소 테스트에서는 돌지 않는다.
 * 실행: RECIPE_INGREDIENT_REPORT=1 ./gradlew test --tests "*RecipeIngredientMasterReport"
 * 결과: build/reports/recipe-ingredients/names.tsv (이름, 레시피 수, 마스터에 있는지, 원문 예시)
 */
@EnabledIfEnvironmentVariable(named = "RECIPE_INGREDIENT_REPORT", matches = "1")
class RecipeIngredientMasterReport {

    private static final Path CSV = Path.of("scripts/seed/COOKRCP01.csv.gz");
    private static final Path MASTER_SQL = Path.of("scripts/seed/ingredient_master.sql");
    private static final Path OUT = Path.of("build/reports/recipe-ingredients");

    @Test
    void 레시피_재료_이름과_재료_마스터를_비교한다() throws IOException {
        RecipeIngredientParser parser = new RecipeIngredientParser();
        Set<String> master = readMasterNames(parser);
        List<Map<String, String>> recipes = readCsv();

        Map<String, Integer> count = new HashMap<>();
        Map<String, String> example = new HashMap<>();
        int totalPieces = 0;
        int matchedPieces = 0;
        for (Map<String, String> r : recipes) {
            for (ParsedIngredient p : parser.parse(r.get("RCP_PARTS_DTLS"))) {
                count.merge(p.name(), 1, Integer::sum);
                example.putIfAbsent(p.name(), p.rawText());
                totalPieces++;
                if (master.contains(p.name())) matchedPieces++;
            }
        }

        Files.createDirectories(OUT);
        List<String> lines = new ArrayList<>();
        lines.add("name\trecipes\tin_master\texample");
        count.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .forEach(e -> lines.add(String.join("\t", e.getKey(), String.valueOf(e.getValue()),
                        master.contains(e.getKey()) ? "Y" : "N", example.get(e.getKey()).replace('\t', ' '))));
        Files.write(OUT.resolve("names.tsv"), lines, StandardCharsets.UTF_8);
        Files.write(OUT.resolve("master.txt"), master.stream().sorted().toList(), StandardCharsets.UTF_8);

        long distinct = count.size();
        long distinctMatched = count.keySet().stream().filter(master::contains).count();
        String summary = String.format(
                "recipes=%d pieces=%d matchedPieces=%d (%.1f%%) distinctNames=%d distinctMatched=%d (%.1f%%) master=%d",
                recipes.size(), totalPieces, matchedPieces, 100.0 * matchedPieces / totalPieces,
                distinct, distinctMatched, 100.0 * distinctMatched / distinct, master.size());
        Files.writeString(OUT.resolve("summary.txt"), summary + "\n", StandardCharsets.UTF_8);
        System.out.println(summary);
    }

    // seed SQL의 "  ('이름', 카테고리, ..." 줄에서 이름만 읽는다. 마스터 이름도 같은 규칙으로 정규화한다
    private static Set<String> readMasterNames(RecipeIngredientParser parser) throws IOException {
        Pattern row = Pattern.compile("^\\s+\\('((?:[^']|'')*)',");
        Set<String> names = new HashSet<>();
        for (String line : Files.readAllLines(MASTER_SQL, StandardCharsets.UTF_8)) {
            Matcher m = row.matcher(line);
            if (m.find()) names.add(parser.normalize(m.group(1).replace("''", "'")));
        }
        return names;
    }

    private static List<Map<String, String>> readCsv() throws IOException {
        String text;
        try (InputStream in = new GZIPInputStream(Files.newInputStream(CSV))) {
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("﻿", "");
        }
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder f = new StringBuilder();
        boolean q = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (q) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') { f.append('"'); i++; } else q = false;
                } else f.append(c);
            } else if (c == '"') q = true;
            else if (c == ',') { row.add(f.toString()); f.setLength(0); }
            else if (c == '\n') { row.add(f.toString().replaceAll("\r$", "")); rows.add(row); row = new ArrayList<>(); f.setLength(0); }
            else f.append(c);
        }
        if (f.length() > 0 || !row.isEmpty()) { row.add(f.toString()); rows.add(row); }
        List<String> header = rows.get(0);
        return rows.subList(1, rows.size()).stream()
                .filter(r -> r.size() == header.size())
                .map(r -> {
                    Map<String, String> m = new HashMap<>();
                    for (int i = 0; i < header.size(); i++) m.put(header.get(i), r.get(i));
                    return m;
                })
                .collect(Collectors.toList());
    }
}
