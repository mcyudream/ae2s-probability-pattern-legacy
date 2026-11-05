package com.tz.statpatterns.compat.ponder;

import com.tz.statpatterns.StatPatternsMod;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.api.registration.SharedTextRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.catnip.math.Pointing;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

/** Loaded only by guarded client IMC. No common class references this optional API. */
public final class ProbabilityPonderPlugin implements PonderPlugin {
    @Override public String getModId() { return StatPatternsMod.NAMESPACE; }
    @Override public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        for (String item : new String[] { "probability_pattern", "probability_pattern_terminal", "wireless_probability_pattern_terminal" })
            helper.addStoryBoard(StatPatternsMod.id(item), StatPatternsMod.id("probability_processing"),
                ProbabilityPonderPlugin::processing, StatPatternsMod.id("probability"))
                .identifiedBy(StatPatternsMod.id("probability_processing"));
    }
    @Override public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.registerTag(StatPatternsMod.id("probability")).title("Probability Processing")
            .description("Encode random machine recipes and plan enough attempts.")
            .item(new ItemStack(StatPatternsMod.PATTERN), true, true).addToIndex().register();
        for (String item : new String[] { "probability_pattern", "probability_pattern_terminal", "wireless_probability_pattern_terminal" })
            helper.addTagToComponent(StatPatternsMod.id(item), StatPatternsMod.id("probability"));
    }
    @Override public void registerSharedText(SharedTextRegistrationHelper helper) {
        helper.registerSharedText("network", "Fuel the vibration chamber. Connect storage, a crafting CPU and a pattern provider to the powered ME network.");
        helper.registerSharedText("encode", "Attach the probability terminal. In Processing mode, set one attempt's inputs and its primary output.");
        // Ponder's default text is already literal; .lang translations go through String.format and escape % as %%.
        helper.registerSharedText("confidence", "Enter the machine's actual success chance and choose 95% or 99% confidence. Encode using an AE2S blank pattern.");
        helper.registerSharedText("install", "Put the pattern in a provider facing your probability machine's input buffer (the chest represents it here). Return actual products to the same ME network.");
        helper.registerSharedText("request", "Request the product. Each probability stage sizes its attempts after subtracting available intermediate stock.");
        helper.registerSharedText("random", "Confidence is statistical. Bad luck can still leave an order waiting; supply missing output or cancel and retry.");
        helper.registerSharedText("wireless", "Charge and link the wireless probability terminal using AE2S. Its probability settings also work inside an AE2S universal terminal.");
    }
    private static void processing(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("probability_processing", "Planning Probability Processing");
        scene.configureBasePlate(0, 0, 7); scene.showBasePlate(); scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), EnumFacing.DOWN);
        int[][] positions = { {1,1,2}, {3,1,3}, {3,1,3}, {4,1,3}, {3,1,2}, {5,1,3}, {3,1,3} };
        var icons = new ItemStack[] { new ItemStack(net.minecraft.init.Items.COAL), new ItemStack(StatPatternsMod.TERMINAL),
            ae2.core.definitions.AEItems.BLANK_PATTERN.stack(), new ItemStack(StatPatternsMod.PATTERN),
            new ItemStack(net.minecraft.init.Items.GOLD_INGOT), new ItemStack(net.minecraft.init.Items.GOLD_INGOT),
            new ItemStack(StatPatternsMod.WIRELESS) };
        int step = 0;
        for (String text : new String[] { "network", "encode", "confidence", "install", "request", "random", "wireless" }) {
            int[] p = positions[step];
            scene.overlay().showOutline(PonderPalette.WHITE, "step-" + step, util.select().position(p[0], p[1], p[2]), 120);
            scene.overlay().showControls(util.vector().topOf(p[0], p[1], p[2]), Pointing.DOWN, 70)
                .rightClick().withItem(icons[step++]);
            scene.overlay().showText(120).sharedText(text).independent(45).attachKeyFrame();
            scene.idle(130);
        }
        scene.markAsFinished();
    }
}
