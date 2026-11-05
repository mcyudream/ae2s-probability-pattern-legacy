package com.tz.statpatterns.mixin;

import ae2.container.me.patternencode.ContainerPatternEncodingTerm;
import ae2.mixins.hei.AccessorRecipeLayout;
import com.tz.statpatterns.terminal.StatisticalContainer;
import com.tz.statpatterns.util.StatPatternsExtractor;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "ae2.integration.modules.hei.PatternEncodingRecipeTransferHandler", remap = false)
public abstract class RecipeTransferMixin {
    @Inject(method = "transferRecipe(Lae2/container/me/patternencode/ContainerPatternEncodingTerm;Lmezz/jei/api/gui/IRecipeLayout;Lnet/minecraft/entity/player/EntityPlayer;ZZ)Lmezz/jei/api/recipe/transfer/IRecipeTransferError;", at = @At("HEAD"))
    private void statpatterns$chance(ContainerPatternEncodingTerm container, IRecipeLayout layout,
            EntityPlayer player, boolean maximum, boolean transfer, CallbackInfoReturnable<IRecipeTransferError> cir) {
        var settings = (StatisticalContainer) container;
        if (transfer && settings.statpatterns$enabled() && layout instanceof AccessorRecipeLayout accessor) {
            StatPatternsExtractor.extract(accessor.ae2$getRecipeWrapper()).ifPresent(settings::statpatterns$setProbability);
        }
    }
}
