package com.tz.statpatterns.mixin;

import ae2.container.AEBaseContainer;
import ae2.container.GuiIds;
import ae2.container.guisync.GuiSync;
import ae2.container.me.patternencode.ContainerPatternEncodingTerm;
import ae2.helpers.IPatternTerminalGuiHost;
import ae2.helpers.WirelessPatternEncodingTerminalGuiHost;
import ae2.parts.encoding.PatternEncodingLogic;
import com.tz.statpatterns.crafting.StatPatternsDetails;
import com.tz.statpatterns.item.StatPatternsWirelessItem;
import com.tz.statpatterns.part.StatPatternsTerminalPart;
import com.tz.statpatterns.terminal.StatisticalContainer;
import com.tz.statpatterns.terminal.StatisticalSettings;
import java.util.ArrayList;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ContainerPatternEncodingTerm.class, remap = false)
public abstract class PatternEncodingContainerMixin extends AEBaseContainer implements StatisticalContainer {
    @Shadow @Final private PatternEncodingLogic encodingLogic;
    @Unique private boolean statpatterns$enabled;
    @Unique @GuiSync(610) public double statpatterns$probability = .8;
    @Unique @GuiSync(611) public double statpatterns$alpha = .05;
    protected PatternEncodingContainerMixin(InventoryPlayer inventory, Object host) { super(inventory, host); }

    @Inject(method = "<init>(Lae2/container/GuiIds$GuiKey;Lnet/minecraft/entity/player/InventoryPlayer;Lae2/helpers/IPatternTerminalGuiHost;ZZ)V", at = @At("RETURN"))
    private void statpatterns$init(GuiIds.GuiKey key, InventoryPlayer inventory, IPatternTerminalGuiHost host,
            boolean bind, boolean providerActions, CallbackInfo ci) {
        statpatterns$enabled = host instanceof StatPatternsTerminalPart
            || host instanceof WirelessPatternEncodingTerminalGuiHost wireless
                && wireless.getTerminalItem() instanceof StatPatternsWirelessItem;
        if (!statpatterns$enabled) return;
        statpatterns$restore();
        registerClientAction("statpatternsProbability", Double.class, 32, this::statpatterns$setProbability);
        registerClientAction("statpatternsAlpha", Double.class, 32, this::statpatterns$setAlpha);
    }
    @Unique private void statpatterns$restore() {
        var settings = (StatisticalSettings) encodingLogic;
        statpatterns$probability = settings.statpatterns$probability();
        statpatterns$alpha = settings.statpatterns$alpha();
    }
    @Override public boolean statpatterns$enabled() { return statpatterns$enabled; }
    @Override public double statpatterns$probability() { return statpatterns$probability; }
    @Override public double statpatterns$alpha() { return statpatterns$alpha; }
    @Override public void statpatterns$setProbability(double value) {
        if (!statpatterns$enabled || !Double.isFinite(value) || value <= 0 || value > 1) return;
        statpatterns$probability = value;
        if (isClientSide()) sendClientAction("statpatternsProbability", value);
        else ((StatisticalSettings) encodingLogic).statpatterns$setProbability(value);
    }
    @Override public void statpatterns$setAlpha(double value) {
        if (!statpatterns$enabled || (value != .05 && value != .01)) return;
        statpatterns$alpha = value;
        if (isClientSide()) sendClientAction("statpatternsAlpha", value);
        else ((StatisticalSettings) encodingLogic).statpatterns$setAlpha(value);
    }
    @Inject(method = "onSlotChange", at = @At("TAIL"))
    private void statpatterns$slot(Slot slot, CallbackInfo ci) {
        if (statpatterns$enabled && !isClientSide()) statpatterns$restore();
    }
    @Inject(method = "encodeProcessingPattern", at = @At("HEAD"), cancellable = true)
    private void statpatterns$encode(CallbackInfoReturnable<ItemStack> cir) {
        if (!statpatterns$enabled) return;
        var inputInv = encodingLogic.getEncodedInputInv();
        var inputs = new ArrayList<ae2.api.stacks.GenericStack>();
        for (int i = 0; i < inputInv.size(); i++) inputs.add(inputInv.getStack(i));
        var primary = encodingLogic.getEncodedOutputInv().getStack(0);
        try {
            if (primary == null) cir.setReturnValue(null);
            else cir.setReturnValue(StatPatternsDetails.encode(inputs, java.util.List.of(primary),
                statpatterns$probability, statpatterns$alpha));
        } catch (IllegalArgumentException | ArithmeticException invalid) { cir.setReturnValue(null); }
    }
}
