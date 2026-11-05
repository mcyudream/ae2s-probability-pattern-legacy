package com.tz.statpatterns.part;

import ae2.api.parts.IPartItem;
import ae2.api.parts.IPartModel;
import ae2.parts.PartModel;
import ae2.parts.encoding.PatternEncodingTerminalPart;
import com.tz.statpatterns.StatPatternsMod;
import net.minecraft.util.ResourceLocation;

public final class StatPatternsTerminalPart extends PatternEncodingTerminalPart {
    public static final ResourceLocation OFF = StatPatternsMod.id("part/probability_pattern_terminal_off");
    public static final ResourceLocation ON = StatPatternsMod.id("part/probability_pattern_terminal_on");
    private static final IPartModel OFF_MODELS = new PartModel(MODEL_BASE, OFF, MODEL_STATUS_OFF);
    private static final IPartModel ON_MODELS = new PartModel(MODEL_BASE, ON, MODEL_STATUS_ON);
    private static final IPartModel CHANNEL_MODELS = new PartModel(MODEL_BASE, ON, MODEL_STATUS_HAS_CHANNEL);

    public StatPatternsTerminalPart(IPartItem<?> item) { super(item); }
    @Override public IPartModel getStaticModels() { return selectModel(OFF_MODELS, ON_MODELS, CHANNEL_MODELS); }
}

