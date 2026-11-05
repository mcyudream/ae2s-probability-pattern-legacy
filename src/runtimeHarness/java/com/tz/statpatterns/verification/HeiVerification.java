package com.tz.statpatterns.verification;

import ae2.container.me.patternencode.ContainerPatternEncodingTerm;
import ae2.helpers.IPatternTerminalGuiHost;
import ae2.integration.modules.hei.PatternEncodingRecipeTransferHandler;
import ae2.mixins.hei.AccessorRecipeLayout;
import com.tz.statpatterns.terminal.StatisticalContainer;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import static com.tz.statpatterns.verification.RuntimeVerification.require;

/** Synthetic recipe data, actual installed HEI/AE2S transfer handler and real server packets. */
public final class HeiVerification {
    public static final class ChanceRecipe implements IRecipeWrapper {
        public double getChance() { return 42; }
        @Override public void getIngredients(IIngredients ingredients) {}
    }
    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(HeiVerification.class.getClassLoader(), new Class<?>[] { type }, handler);
    }
    private static IGuiIngredient<ItemStack> ingredient(boolean input, ItemStack stack) {
        return proxy(IGuiIngredient.class, (p, method, args) -> switch (method.getName()) {
            case "isInput" -> input;
            case "getAllIngredients" -> List.of(stack);
            case "getDisplayedIngredient" -> stack;
            default -> null;
        });
    }
    public static void transfer() {
        var mc = Minecraft.getMinecraft();
        var container = (ContainerPatternEncodingTerm) mc.player.openContainer;
        Map<Integer, IGuiIngredient<ItemStack>> slots = new LinkedHashMap<>();
        slots.put(0, ingredient(false, new ItemStack(Items.GOLD_INGOT, 4)));
        slots.put(1, ingredient(true, new ItemStack(Items.IRON_INGOT, 2)));
        slots.put(2, ingredient(false, new ItemStack(Items.REDSTONE, 1)));
        var items = proxy(IGuiItemStackGroup.class, (p, method, args) -> method.getName().equals("getGuiIngredients") ? slots : null);
        var category = proxy(IRecipeCategory.class, (p, method, args) -> method.getName().equals("getUid") ? "statpatterns.verification" : "Probability fixture");
        var wrapper = new ChanceRecipe();
        var layout = (IRecipeLayout) Proxy.newProxyInstance(HeiVerification.class.getClassLoader(),
            new Class<?>[] { IRecipeLayout.class, AccessorRecipeLayout.class }, (p, method, args) -> switch (method.getName()) {
                case "ae2$getRecipeWrapper" -> wrapper;
                case "getRecipeCategory" -> category;
                case "getItemStacks" -> items;
                case "getIngredientsGroup" -> args[0] == VanillaTypes.ITEM || args[0] == ItemStack.class ? items : null;
                default -> null;
            });
        new PatternEncodingRecipeTransferHandler<>(ContainerPatternEncodingTerm.class)
            .transferRecipe(container, layout, mc.player, false, true);
        require(((StatisticalContainer) container).statpatterns$probability() == .42, "HEI chance extraction mixin did not execute");
    }
    public static void verifyServer(EntityPlayerMP player) {
        var container = (ContainerPatternEncodingTerm) player.openContainer;
        var logic = ((IPatternTerminalGuiHost) container.getTarget()).getLogic();
        require(((StatisticalContainer) container).statpatterns$probability() == .42, "HEI chance packet did not reach server");
        require(logic.getEncodedInputInv().getStack(0).amount() == 2, "HEI input transfer changed recipe amount");
        require(logic.getEncodedOutputInv().getStack(0).amount() == 4 && logic.getEncodedOutputInv().getStack(1) == null,
            "HEI transfer did not preserve primary output and clear secondary byproduct");
        RuntimeVerification.INSTANCE.checked("Actual HEI transfer handler imports ingredients, extracts 42% chance and sends primary-only recipe to server");
    }
}
