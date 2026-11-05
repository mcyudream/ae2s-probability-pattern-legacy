package com.tz.statpatterns.mixin;

import ae2.api.crafting.IPatternDetails;
import ae2.helpers.patternprovider.PatternProviderLogic;
import com.tz.statpatterns.crafting.StatPatternsDetails;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PatternProviderLogic.class, remap = false)
public abstract class PatternProviderMixin {
    @Redirect(method = "pushPattern", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/ObjectList;contains(Ljava/lang/Object;)Z"))
    private boolean statpatterns$accept(ObjectList<?> patterns, Object pattern) {
        return patterns.contains(pattern instanceof IPatternDetails details ? StatPatternsDetails.unplanned(details) : pattern);
    }
    @Inject(method = "canMergePatternPush", at = @At("HEAD"), cancellable = true)
    private void statpatterns$single(IPatternDetails pattern, CallbackInfoReturnable<Boolean> cir) {
        if (pattern instanceof StatPatternsDetails) cir.setReturnValue(false);
    }
}
