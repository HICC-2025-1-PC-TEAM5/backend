package hicc_project.RottenToday.dto;

// 레시피 재료 대신 쓸 수 있는 냉장고 재료 (D-040). 예: { ingredient: "닭고기살", from: "닭고기" }
public record SubstituteDto(String ingredient, String from) {}
