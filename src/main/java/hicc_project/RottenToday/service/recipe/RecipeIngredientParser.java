package hicc_project.RottenToday.service.recipe;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 식품안전나라 레시피의 재료 문자열(RCP_PARTS_DTLS)을 재료 이름 목록으로 나눈다 (레시피 계획 Phase 1, D-021).
 * 예: "연두부 75g(3/4모), 다진 마늘 2g(1/2쪽), 참깨 약간" → [연두부, 마늘, 참깨]
 * <p>
 * 이름은 {@link #normalize(String)}로 정규화한다: 수식어(다진·저염 등)를 떼고 동의어 사전으로 일반적으로 쓰는 이름 하나로 맞춘다.
 * 냉장고 재료 이름도 추천할 때 같은 함수로 정규화해야 서로 맞는다.
 * 외부 상태가 없는 순수 함수다. 동의어 사전은 {@code recipe/ingredient-aliases.csv}에서 읽는다.
 */
@Component
public class RecipeIngredientParser {

    public record ParsedIngredient(String name, String rawText) {}

    private static final String ALIAS_RESOURCE = "recipe/ingredient-aliases.csv";

    // 줄 앞 기호와 "[1인분]" 같은 대괄호 머리말
    private static final Pattern LEADING_MARK = Pattern.compile("^[\\s●•·∙ㆍ\\-*※▶▷◆◇■□○◎]+");
    private static final Pattern BRACKET_HEADER = Pattern.compile("^\\s*\\[[^\\]]*\\]\\s*");
    // "양념장 :" 처럼 콜론 앞이 숫자 없는 머리말
    private static final Pattern COLON_HEADER = Pattern.compile("^[^0-9:()]{1,20}[:：]\\s*");
    // 줄 맨 앞의 "재료" 머리말 ("재료 바나나(150g)")
    private static final Pattern WORD_HEADER = Pattern.compile("^(주재료|부재료|재료|필수\\s*재료|양념|소스|고명|육수)\\s+");
    private static final Pattern PAREN = Pattern.compile("\\([^()]*\\)|\\[[^\\[\\]]*\\]");
    // 수량이 시작되는 곳: 숫자, 분수 문자
    private static final Pattern QUANTITY_START = Pattern.compile("[0-9½⅓⅔¼¾⅛]");
    private static final Pattern VAGUE_AMOUNT = Pattern.compile("\\s*(약간|적당량|적당히|조금|소량|한줌|한꼬집|넉넉히)\\s*$");
    private static final Pattern HAS_INGREDIENT_HINT = Pattern.compile("[0-9½⅓⅔¼¾,]|약간|적당량|조금|소량");
    private static final Pattern HTML_BREAK = Pattern.compile("(?i)<br\\s*/?>");
    private static final Pattern MID_BRACKET_HEADER = Pattern.compile("\\[[^\\]]*(양념|소스|소개|재료|고명|토핑|드레싱|육수)[^\\]]*\\]");
    // "조림양념 저염간장", "반죽양념 다진 파" 처럼 콜론 없이 붙은 소제목 단어
    private static final Pattern SUB_HEADER = Pattern.compile("^\\S*(양념|양념장|소스|토핑|반죽|드레싱|장식)\\s+(?=\\S)");
    private static final Pattern TRAILING_SUFFIX = Pattern.compile("\\s*(다진것|불린것|장식용|\\.)$");
    // 조각이 이 단어만 남으면 재료가 아니라 머리말이다
    private static final java.util.Set<String> HEADER_WORDS = java.util.Set.of(
            "재료", "주재료", "부재료", "양념", "양념장", "소스", "고명", "육수", "드레싱", "토핑", "반죽", "장식");
    // 동의어 사전에서 이 값으로 매핑하면 재료가 아닌 것으로 보고 버린다 (예: "멸치액적소스,-")
    static final String IGNORE = "-";

    // 이름 앞에서 떼는 수식어. "생강"처럼 이름 자체가 수식어로 시작하는 경우를 피하려고 공백이 필요한 것은 공백까지 포함한다
    private static final List<String> PREFIXES = List.of(
            "다진 ", "다진", "저염 ", "저염", "무염 ", "무염", "저당 ", "저지방 ",
            "생 ", "냉동 ", "삶은 ", "데친 ", "볶은 ", "구운 ", "불린 ", "말린 ", "채썬 ", "곱게 간 ", "간 ");

    /** 사전 한 행. common = 기준 이름과 함께 둘 다 흔히 쓰는 이름 (화면 표시·검색에 함께 쓴다) */
    public record AliasEntry(String alias, String canonical, boolean common) {}

    private final Map<String, String> aliases;
    // 기준 이름 → 그 이름으로 정규화되는 모든 별칭 (예: 달걀 → [계란, 삶은달걀, …])
    private final Map<String, Set<String>> aliasesByCanonical = new HashMap<>();
    // 기준 이름 → 둘 다 흔히 쓰는 별칭 (예: 달걀 → [계란])
    private final Map<String, List<String>> commonByCanonical = new HashMap<>();

    @Autowired // 빈으로는 리소스 사전을 읽는 기본 생성자를 쓴다
    public RecipeIngredientParser() {
        this(loadEntries());
    }

    public RecipeIngredientParser(Map<String, String> aliases) {
        this(aliases.entrySet().stream().map(e -> new AliasEntry(e.getKey(), e.getValue(), false)).toList());
    }

    public RecipeIngredientParser(List<AliasEntry> entries) {
        Map<String, String> map = new LinkedHashMap<>();
        for (AliasEntry e : entries) {
            map.put(e.alias(), e.canonical());
            if (IGNORE.equals(e.canonical())) continue;
            aliasesByCanonical.computeIfAbsent(e.canonical(), k -> new LinkedHashSet<>()).add(e.alias());
            if (e.common()) commonByCanonical.computeIfAbsent(e.canonical(), k -> new ArrayList<>()).add(e.alias());
        }
        this.aliases = map;
    }

    /**
     * 이 이름과 같은 재료로 보는 모든 이름 (기준 이름 + 별칭). 원문 문자열로 찾아야 할 때 쓴다.
     * 예: "계란" → [달걀, 계란, 삶은달걀, 달걀물, …]
     */
    public Set<String> namesOf(String name) {
        String canonical = normalize(name);
        if (canonical.isEmpty()) return Set.of();
        Set<String> names = new LinkedHashSet<>();
        names.add(canonical);
        names.addAll(aliasesByCanonical.getOrDefault(canonical, Set.of()));
        return names;
    }

    /** 기준 이름과 함께 둘 다 흔히 쓰는 별칭 (사전의 both 표시). 화면에 "달걀(계란)"처럼 보여 줄 때 쓴다 */
    public List<String> commonAliasesOf(String name) {
        return commonByCanonical.getOrDefault(normalize(name), List.of());
    }

    public List<ParsedIngredient> parse(String partsDetail) {
        if (partsDetail == null || partsDetail.isBlank()) return List.of();

        Map<String, ParsedIngredient> byName = new LinkedHashMap<>(); // 같은 레시피 안의 중복 이름은 하나로
        String text = HTML_BREAK.matcher(partsDetail).replaceAll("\n");
        for (String line : text.split("\\r?\\n")) {
            String body = stripHeaders(line);
            // 줄 중간의 "[소스소개]" 같은 대괄호 머리말은 지우고, 나머지 대괄호는 구분자로 본다
            body = MID_BRACKET_HEADER.matcher(body).replaceAll(",");
            body = body.replace('[', ',').replace(']', ',');
            if (body.isBlank() || isHeaderOnly(body)) continue;
            for (String piece : splitOutsideParens(body)) {
                String raw = piece.trim();
                String name = normalize(extractName(raw));
                if (!name.isEmpty() && !HEADER_WORDS.contains(name)) {
                    byName.putIfAbsent(name, new ParsedIngredient(name, raw));
                }
            }
        }
        return new ArrayList<>(byName.values());
    }

    /** 수식어를 떼고 공백을 정리한 뒤 동의어 사전으로 일반적인 이름 하나로 맞춘다 */
    public String normalize(String name) {
        String key = lookupKey(name);
        String canonical = aliases.getOrDefault(key, key);
        return IGNORE.equals(canonical) ? "" : canonical;
    }

    /** 사전을 찾기 전 단계: 수식어를 떼고 공백을 없앤 키 */
    static String lookupKey(String name) {
        if (name == null) return "";
        String s = name.replaceAll("\\s+", " ").trim();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String p : PREFIXES) {
                if (s.startsWith(p) && s.length() > p.length()) {
                    s = s.substring(p.length()).trim();
                    changed = true;
                }
            }
        }
        return s.replace(" ", "");
    }

    private static String stripHeaders(String line) {
        String s = LEADING_MARK.matcher(line).replaceFirst("");
        s = BRACKET_HEADER.matcher(s).replaceFirst("");
        s = COLON_HEADER.matcher(s).replaceFirst("");
        s = WORD_HEADER.matcher(s).replaceFirst("");
        return s.trim();
    }

    // "고명", 첫 줄의 요리명처럼 수량·쉼표가 없는 줄은 머리말로 본다
    private static boolean isHeaderOnly(String body) {
        return !HAS_INGREDIENT_HINT.matcher(body).find();
    }

    private static List<String> splitOutsideParens(String s) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        StringBuilder cur = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '(' || c == '[') depth++;
            if ((c == ')' || c == ']') && depth > 0) depth--;
            if ((c == ',' || c == '，') && depth == 0) {
                parts.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        parts.add(cur.toString());
        return parts;
    }

    private static String extractName(String raw) {
        String s = raw;
        String prev;
        do { // 중첩 괄호까지 지운다
            prev = s;
            s = PAREN.matcher(s).replaceAll(" ");
        } while (!s.equals(prev));
        // "토핑 > 그릭요거트", "비트양념:식초" → 구분 기호 뒤가 재료
        int cut = Math.max(Math.max(s.lastIndexOf('>'), s.lastIndexOf(':')), s.lastIndexOf('：'));
        if (cut >= 0) s = s.substring(cut + 1);
        var m = QUANTITY_START.matcher(s);
        if (m.find()) s = s.substring(0, m.start());
        s = LEADING_MARK.matcher(s).replaceFirst("");
        s = SUB_HEADER.matcher(s.trim()).replaceFirst("");
        String prev2;
        do {
            prev2 = s;
            s = VAGUE_AMOUNT.matcher(s).replaceFirst("");
            s = TRAILING_SUFFIX.matcher(s).replaceFirst("");
        } while (!s.equals(prev2));
        return s.replaceAll("[()\\[\\]:<>]", " ").trim();
    }

    /** 별칭,이름,이유 형식. '#'으로 시작하는 줄은 주석. 키는 공백을 뺀 이름 */
    static Map<String, String> loadAliases() {
        Map<String, String> map = new LinkedHashMap<>();
        for (AliasEntry e : loadEntries()) map.put(e.alias(), e.canonical());
        return map;
    }

    /** 별칭,이름,both,이유 형식. both 칸이 "both"이면 둘 다 흔히 쓰는 이름 */
    static List<AliasEntry> loadEntries() {
        List<AliasEntry> entries = new ArrayList<>();
        InputStream in = RecipeIngredientParser.class.getClassLoader().getResourceAsStream(ALIAS_RESOURCE);
        if (in == null) return entries;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] kv = line.split(",", 4); // 별칭,이름,both,이유
                if (kv.length >= 2 && !kv[0].isBlank() && !kv[1].isBlank()) {
                    boolean common = kv.length >= 3 && "both".equals(kv[2].trim());
                    entries.add(new AliasEntry(kv[0].replace(" ", ""), kv[1].trim().replace(" ", ""), common));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("동의어 사전을 읽지 못했습니다: " + ALIAS_RESOURCE, e);
        }
        return entries;
    }
}
