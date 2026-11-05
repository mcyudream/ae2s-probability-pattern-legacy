package com.tz.statpatterns.client;

import ae2.client.gui.me.patternencode.GuiPatternEncodingTerm;
import ae2.client.gui.style.GuiStyleManager;
import ae2.container.AEBaseContainer;
import ae2.container.me.patternencode.ContainerPatternEncodingTerm;
import com.tz.statpatterns.CommonProxy;
import com.tz.statpatterns.StatPatternsMod;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = StatPatternsMod.ID, value = Side.CLIENT)
public final class ClientProxy extends CommonProxy {
    @Override public void preInit() { MinecraftForge.EVENT_BUS.register(new ProbabilityControls()); }
    @SubscribeEvent public static void models(ModelRegistryEvent event) {
        for (var item : new net.minecraft.item.Item[] { StatPatternsMod.PATTERN, StatPatternsMod.TERMINAL, StatPatternsMod.WIRELESS })
            ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }
    @SubscribeEvent public static void transfer(ae2.api.client.PatternRecipeTransferEvent event) {
        if (event.getMode() != ae2.parts.encoding.EncodingMode.PROCESSING
            || !((com.tz.statpatterns.terminal.StatisticalContainer) event.getContainer()).statpatterns$enabled()) return;
        // Each pattern has exactly one random output; other recipe byproducts are not promised to the CPU.
        for (int i = 1; i < event.getOutputSlots().size(); i++) event.getOutputSlots().get(i).clear();
    }
    @Override public void init() {
        if (net.minecraftforge.fml.common.Loader.isModLoaded("ponder-legacy"))
            net.minecraftforge.fml.common.event.FMLInterModComms.sendMessage("ponder-legacy", "register_ponder_plugin",
                "com.tz.statpatterns.compat.ponder.ProbabilityPonderPlugin");
    }
    @Override public Object wirelessScreen(AEBaseContainer container, InventoryPlayer inventory) {
        return container instanceof ContainerPatternEncodingTerm pattern
            ? new GuiPatternEncodingTerm(pattern, inventory, null,
                GuiStyleManager.loadStyleDoc("/screens/terminals/pattern_encoding_terminal.json")) : null;
    }
}
