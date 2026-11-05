/* Probability Pattern for AE2, Copyright (C) 2026 TaoLe-si; LGPL-3.0-or-later.
 * Cleanroom / AE2S port Copyright (C) 2026 yudream. */
package com.tz.statpatterns.crafting;

import ae2.api.crafting.IPatternDetails;
import ae2.api.crafting.PatternDetailsTooltip;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.GenericStack;
import ae2.crafting.pattern.AEProcessingPattern;
import com.tz.statpatterns.StatPatternsMod;
import com.tz.statpatterns.math.StatPatternsSizing;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/** Immutable per-request patterns have distinct identities and retain sizing in CPU NBT. */
public final class StatPatternsDetails implements IPatternDetails {
    public static final String TAG = "statistical_pattern";
    private static final String REQUEST = "requestedOutput";
    private final AEItemKey definition;
    private final AEItemKey baseDefinition;
    private final List<GenericStack> sparseInputs;
    private final GenericStack output;
    private final IInput[] inputs;
    private final List<GenericStack> outputs;
    private final double probability, alpha;
    private final long requested, attempts;

    private StatPatternsDetails(AEItemKey definition) {
        this.definition = definition;
        var raw = definition.toStack();
        var tag = raw.getTagCompound().getCompoundTag(TAG);
        probability = tag.getDouble("successProbability");
        alpha = tag.hasKey("alpha", 6) ? tag.getDouble("alpha") : .05;
        StatPatternsSizing.validate(1, probability, alpha);
        requested = tag.hasKey(REQUEST, 4) ? tag.getLong(REQUEST) : 0;
        if (requested < 0) throw new IllegalArgumentException("Negative planned output");
        var processing = new AEProcessingPattern(definition);
        sparseInputs = processing.getSparseInputs();
        output = processing.getSparseOutputs().getFirst();
        if (processing.getSparseOutputs().stream().filter(java.util.Objects::nonNull).count() != 1)
            throw new IllegalArgumentException("Probability patterns require one primary output");
        long successes = requested == 0 ? 1 : divideCeil(requested, output.amount());
        attempts = requested == 0 ? 1 : StatPatternsSizing.planAttempts(successes, probability, alpha, 30).attempts();
        Map<AEKey, Long> condensed = new LinkedHashMap<>();
        for (var input : sparseInputs) {
            if (input != null) condensed.merge(input.what(), Math.multiplyExact(input.amount(), attempts), Math::addExact);
        }
        inputs = condensed.entrySet().stream().map(e -> new Input(e.getKey(), e.getValue())).toArray(IInput[]::new);
        outputs = List.of(new GenericStack(output.what(), requested == 0 ? output.amount() : requested));
        tag.removeTag(REQUEST);
        baseDefinition = AEItemKey.of(raw);
    }

    public static StatPatternsDetails decode(AEItemKey what, World world) {
        if (what == null || what.getItem() != StatPatternsMod.PATTERN) return null;
        var stack = what.getReadOnlyStack();
        if (!stack.hasTagCompound() || !stack.getTagCompound().hasKey(TAG, 10)) return null;
        try { return new StatPatternsDetails(what); }
        catch (IllegalArgumentException | ArithmeticException invalid) { return null; }
    }

    public static ItemStack encode(List<GenericStack> inputs, List<GenericStack> outputs, double p, double alpha) {
        StatPatternsSizing.validate(1, p, alpha);
        var result = new ItemStack(StatPatternsMod.PATTERN);
        AEProcessingPattern.encode(result, inputs, outputs);
        var tag = new NBTTagCompound();
        tag.setDouble("successProbability", p);
        tag.setDouble("alpha", alpha);
        result.setTagInfo(TAG, tag);
        // Validate slot counts, positive amounts and overflow before consuming a blank pattern.
        new StatPatternsDetails(AEItemKey.of(result));
        return result;
    }

    public StatPatternsDetails forRequest(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("Requested output must be positive");
        var stack = baseDefinition.toStack();
        stack.getTagCompound().getCompoundTag(TAG).setLong(REQUEST, amount);
        return new StatPatternsDetails(AEItemKey.of(stack));
    }
    public StatPatternsDetails basePattern() { return requested == 0 ? this : new StatPatternsDetails(baseDefinition); }
    public boolean isPlanned() { return requested > 0; }
    public long requestedOutput() { return requested; }
    public long attempts() { return attempts; }
    public double probability() { return probability; }
    public double alpha() { return alpha; }
    public List<GenericStack> sparseInputs() { return sparseInputs; }
    public GenericStack outputPerSuccess() { return output; }
    public static long divideCeil(long value, long divisor) { return value / divisor + (value % divisor == 0 ? 0 : 1); }
    public static IPatternDetails unplanned(IPatternDetails pattern) {
        return pattern instanceof StatPatternsDetails statistical ? statistical.basePattern() : pattern;
    }
    @Override public AEItemKey getDefinition() { return definition; }
    @Override public IInput[] getInputs() { return inputs.clone(); }
    @Override public List<GenericStack> getOutputs() { return outputs; }
    @Override public int hashCode() { return definition.hashCode(); }
    @Override public boolean equals(Object value) {
        return value instanceof StatPatternsDetails other && definition.equals(other.definition);
    }
    @Override public PatternDetailsTooltip getTooltip(World world, ITooltipFlag flags) {
        var tooltip = new PatternDetailsTooltip(PatternDetailsTooltip.OUTPUT_TEXT_PRODUCES);
        sparseInputs.stream().filter(java.util.Objects::nonNull).forEach(tooltip::addInput);
        tooltip.addOutput(output);
        tooltip.addProperty(new TextComponentTranslation("statpatterns.probability"),
            new TextComponentString(String.format(java.util.Locale.ROOT, "%.4g%%", probability * 100)));
        tooltip.addProperty(new TextComponentTranslation("statpatterns.confidence"),
            new TextComponentString(String.format(java.util.Locale.ROOT, "%.0f%%", (1 - alpha) * 100)));
        return tooltip;
    }
    private record Input(AEKey key, long count) implements IInput {
        @Override public GenericStack[] possibleInputs() { return new GenericStack[] { new GenericStack(key, 1) }; }
        @Override public long getMultiplier() { return count; }
        @Override public boolean isValid(AEKey candidate, World world) { return key.equals(candidate); }
        @Override public AEKey getRemainingKey(AEKey template) { return null; }
    }
}
