package com.tz.statpatterns.mixin;

import ae2.parts.encoding.PatternEncodingLogic;
import ae2.parts.encoding.EncodingMode;
import ae2.util.inv.AppEngInternalInventory;
import com.tz.statpatterns.crafting.StatPatternsDetails;
import com.tz.statpatterns.math.StatPatternsSizing;
import com.tz.statpatterns.terminal.StatisticalSettings;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PatternEncodingLogic.class, remap = false)
public abstract class PatternEncodingLogicMixin implements StatisticalSettings {
    @Unique private double statpatterns$p = .8;
    @Unique private double statpatterns$alpha = .05;
    @Shadow public abstract void saveChanges();
    @Override public double statpatterns$probability() { return statpatterns$p; }
    @Override public double statpatterns$alpha() { return statpatterns$alpha; }
    @Override public void statpatterns$setProbability(double p) {
        StatPatternsSizing.validate(1, p, statpatterns$alpha);
        statpatterns$p = p; saveChanges();
    }
    @Override public void statpatterns$setAlpha(double alpha) {
        StatPatternsSizing.validate(1, statpatterns$p, alpha);
        statpatterns$alpha = alpha; saveChanges();
    }
    @Inject(method = "readFromNBT", at = @At("TAIL"))
    private void statpatterns$read(NBTTagCompound tag, CallbackInfo ci) {
        if (!tag.hasKey("statpatternsProbability", 6)) return;
        try {
            double p = tag.getDouble("statpatternsProbability"), alpha = tag.getDouble("statpatternsAlpha");
            StatPatternsSizing.validate(1, p, alpha);
            statpatterns$p = p; statpatterns$alpha = alpha;
        } catch (IllegalArgumentException invalid) { statpatterns$p = .8; statpatterns$alpha = .05; }
    }
    @Inject(method = "writeToNBT", at = @At("TAIL"))
    private void statpatterns$write(NBTTagCompound tag, CallbackInfo ci) {
        tag.setDouble("statpatternsProbability", statpatterns$p);
        tag.setDouble("statpatternsAlpha", statpatterns$alpha);
    }
    @Inject(method = "onChangeInventory", at = @At("TAIL"))
    private void statpatterns$load(AppEngInternalInventory inv, int slot, CallbackInfo ci) {
        var self = (PatternEncodingLogic) (Object) this;
        if (inv != self.getEncodedPatternInv()) return;
        var details = StatPatternsDetails.decode(ae2.api.stacks.AEItemKey.of(inv.getStackInSlot(0)), null);
        if (details == null) return;
        statpatterns$p = details.probability(); statpatterns$alpha = details.alpha();
        self.setMode(EncodingMode.PROCESSING);
        var inputs = self.getEncodedInputInv();
        inputs.beginBatch();
        try {
            for (int i = 0; i < inputs.size(); i++)
                inputs.setStack(i, i < details.sparseInputs().size() ? details.sparseInputs().get(i) : null);
        } finally { inputs.endBatch(); }
        var outputs = self.getEncodedOutputInv();
        outputs.beginBatch();
        try {
            for (int i = 0; i < outputs.size(); i++) outputs.setStack(i, i == 0 ? details.outputPerSuccess() : null);
        } finally { outputs.endBatch(); }
        saveChanges();
    }
}
