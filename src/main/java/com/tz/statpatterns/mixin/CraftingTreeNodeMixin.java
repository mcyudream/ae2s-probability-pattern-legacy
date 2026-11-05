package com.tz.statpatterns.mixin;

import ae2.api.crafting.IPatternDetails;
import ae2.api.stacks.AEKey;
import ae2.crafting.CraftingCalculation;
import ae2.crafting.CraftingTreeNode;
import ae2.crafting.CraftingTreeProcess;
import com.llamalad7.mixinextras.sugar.Local;
import com.tz.statpatterns.crafting.StatPatternsDetails;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CraftingTreeNode.class, remap = false)
public abstract class CraftingTreeNodeMixin {
    @Shadow @Final private long amount;
    @Shadow private ObjectArrayList<CraftingTreeProcess> nodes;
    @Unique private long statpatterns$requested;
    @Unique private boolean statpatterns$hasProbability;

    @Inject(method = "requestUncached", at = @At(value = "INVOKE", target = "Lae2/crafting/CraftingTreeNode;buildChildPatterns()V"))
    private void statpatterns$request(CallbackInfo ci, @Local(argsOnly = true) long requestedAmount) {
        statpatterns$resize(requestedAmount);
    }
    @Inject(method = "extractAvailableForCraftingInner", at = @At(value = "INVOKE", target = "Lae2/crafting/CraftingTreeNode;buildChildPatterns()V"))
    private void statpatterns$available(CallbackInfoReturnable<Long> cir, @Local(argsOnly = true) long maxAmount,
            @Local(name = "available") long available) {
        statpatterns$resize(maxAmount - available);
    }
    @Unique private void statpatterns$resize(long units) {
        long total = Math.multiplyExact(units, amount);
        if (total != statpatterns$requested && statpatterns$hasProbability) nodes = null;
        statpatterns$requested = total;
    }
    @Redirect(method = "buildChildPatterns", at = @At(value = "INVOKE",
        target = "Lae2/crafting/CraftingCalculation;getCraftingFor(Lae2/api/stacks/AEKey;)Lit/unimi/dsi/fastutil/objects/ObjectList;"))
    private ObjectList<IPatternDetails> statpatterns$patterns(CraftingCalculation job, AEKey key) {
        var original = job.getCraftingFor(key);
        if (statpatterns$requested <= 0) return original;
        var result = new ObjectArrayList<IPatternDetails>(original.size());
        for (var pattern : original) {
            if (pattern instanceof StatPatternsDetails probability) {
                statpatterns$hasProbability = true;
                result.add(probability.forRequest(statpatterns$requested));
            } else result.add(pattern);
        }
        return result;
    }
}
