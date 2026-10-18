package st.evening.mc.rotnsaws.compat.jei;

import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public enum SerrJeiCompat {
    ;

    @Nullable
    private static Consumer<String> recipeGuiHandler;

    static void setRecipeGuiHandler(final Consumer<String> handler) {
        recipeGuiHandler = handler;
    }

    public static boolean hasRecipeGui() {
        return recipeGuiHandler != null;
    }

    public static void openRecipeGui(final String categoryUid) {
        if (recipeGuiHandler != null) {
            recipeGuiHandler.accept(categoryUid);
        }
    }
}
