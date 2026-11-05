package com.tz.statpatterns.mixin;

import ae2.crafting.CraftingCalculation;
import ae2.crafting.CraftingPlan;
import ae2.crafting.graph.CraftingGraph;
import com.tz.statpatterns.crafting.StatPatternsDetails;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** AE2S's linear graph assumes constant inputs; its native tree supports request-specific patterns. */
@Mixin(value = CraftingCalculation.class, remap = false)
public abstract class CraftingCalculationMixin {
    @Inject(method = "planGraph", at = @At("HEAD"), cancellable = true)
    private void statpatterns$tree(CraftingGraph graph, long amount, long started,
            CallbackInfoReturnable<CraftingPlan> cir) {
        for (var node : graph.getAllNodes()) {
            if (node.getPatternCandidates().stream().anyMatch(StatPatternsDetails.class::isInstance)) {
                cir.setReturnValue(null);
                return;
            }
        }
    }
}
