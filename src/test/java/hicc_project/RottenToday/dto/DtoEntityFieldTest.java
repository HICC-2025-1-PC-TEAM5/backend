package hicc_project.RottenToday.dto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

// D-033: 응답은 엔티티를 그대로 담지 않고 DTO로 준다. dto 패키지의 필드에 엔티티 타입(목록 포함)이 있으면 실패한다
// 엔티티에 필드·연관을 추가했을 때 API 응답이 모르게 바뀌는 것을 막는다 (D-031의 원인)
class DtoEntityFieldTest {

    private static final String ENTITY_PACKAGE = "hicc_project.RottenToday.entity.";

    @Test
    void DTO_필드에_엔티티_타입이_없다() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));

        List<String> violations = new ArrayList<>();
        int scanned = 0;
        for (BeanDefinition bd : scanner.findCandidateComponents("hicc_project.RottenToday.dto")) {
            Class<?> type = Class.forName(bd.getBeanClassName());
            scanned++;
            for (Field field : type.getDeclaredFields()) {
                if (refersToEntity(field.getGenericType())) {
                    violations.add(type.getSimpleName() + "." + field.getName() + " : " + field.getGenericType().getTypeName());
                }
            }
        }

        assertThat(scanned).isGreaterThan(10);
        assertThat(violations).isEmpty();
    }

    private static boolean refersToEntity(Type type) {
        if (type instanceof Class<?> c) {
            // 한글 enum(Category, StorageCondition, Appetite)은 값이라 허용한다
            return c.getName().startsWith(ENTITY_PACKAGE) && !c.isEnum();
        }
        if (type instanceof ParameterizedType p) {
            for (Type arg : p.getActualTypeArguments()) {
                if (refersToEntity(arg)) return true;
            }
            return refersToEntity(p.getRawType());
        }
        return false;
    }
}
