package hicc_project.RottenToday.dto;

import java.util.List;

public class ImageToIngredientResponse {
    List<String> ingredient;

    public ImageToIngredientResponse(List<String> ingredient) {this.ingredient = ingredient;}

    public List<String> getIngredient() {return ingredient;}
}
