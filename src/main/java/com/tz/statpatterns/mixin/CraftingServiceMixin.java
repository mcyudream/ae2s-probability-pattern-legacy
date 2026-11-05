package com.tz.statpatterns.mixin;

import ae2.api.crafting.IPatternDetails;
import ae2.me.service.CraftingService;
import com.tz.statpatterns.crafting.StatPatternsDetails;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = CraftingService.class, remap = false)
public abstract class CraftingServiceMixin {
    @ModifyVariable(method = {"getProviders", "getProvidersSnapshot"}, at = @At("HEAD"), argsOnly = true)
    private IPatternDetails statpatterns$provider(IPatternDetails pattern) { return StatPatternsDetails.unplanned(pattern); }
}
