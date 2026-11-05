package com.tz.statpatterns.verification;

import ae2.client.gui.me.patternencode.GuiPatternEncodingTerm;
import ae2.container.me.patternencode.ContainerPatternEncodingTerm;
import ae2.core.definitions.AEItems;
import ae2.helpers.IPatternTerminalGuiHost;
import ae2.items.tools.powered.WirelessTerminalItem;
import ae2.items.tools.powered.WirelessUniversalTerminalItem;
import com.tz.statpatterns.StatPatternsMod;
import com.tz.statpatterns.crafting.StatPatternsDetails;
import com.tz.statpatterns.terminal.StatisticalContainer;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import static com.tz.statpatterns.verification.RuntimeVerification.require;

/** Runs genuine GUI-open and client-action packets against the integrated server. */
public final class ClientVerification {
    private final Minecraft mc = Minecraft.getMinecraft();
    private CompletableFuture<Void> pending;
    private int phase, ticks, waitUntil;
    private String capture;
    private boolean done;
    private boolean heiVerified;
    private volatile boolean wireApplied, wirelessApplied;
    public static void install() { MinecraftForge.EVENT_BUS.register(new ClientVerification()); }
    private void check(String text) { RuntimeVerification.INSTANCE.checked(text); }
    private net.minecraft.entity.player.EntityPlayerMP player() {
        return mc.getIntegratedServer().getPlayerList().getPlayers().getFirst();
    }
    private void server(Runnable work) {
        pending = new CompletableFuture<>();
        mc.getIntegratedServer().addScheduledTask(() -> {
            try { work.run(); pending.complete(null); }
            catch (Throwable failure) { pending.completeExceptionally(failure); }
        });
    }
    private StatisticalContainer settings() { return (StatisticalContainer) mc.player.openContainer; }
    private ContainerPatternEncodingTerm container() { return (ContainerPatternEncodingTerm) mc.player.openContainer; }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || done) return;
        ticks++;
        if (ticks < waitUntil || capture != null) return;
        try {
            if (ticks > 5000) throw new IllegalStateException("Client verification stalled in phase " + phase);
            if (pending != null) {
                if (!pending.isDone()) return;
                pending.join(); pending = null;
            }
            switch (phase) {
                case 0 -> {
                    if (!(mc.currentScreen instanceof GuiMainMenu)) return;
                    mc.gameSettings.pauseOnLostFocus = false; mc.gameSettings.renderDistanceChunks = 2; mc.gameSettings.guiScale = 2;
                    mc.gameSettings.setSoundLevel(net.minecraft.util.SoundCategory.MASTER, 0);
                    mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
                    var config = new WorldSettings(2718, GameType.CREATIVE, false, false, WorldType.FLAT);
                    config.setGeneratorOptions("3;minecraft:bedrock,2*minecraft:dirt,minecraft:grass;1;"); config.enableCommands();
                    mc.launchIntegratedServer("probability-verification", "Probability Pattern verification", config);
                    phase++;
                }
                case 1 -> {
                    if (mc.world == null || mc.player == null || !RuntimeVerification.gameplayDone) return;
                    check("Packaged client enters world and completes the real chained CPU job");
                    server(() -> {
                        player().setPositionAndUpdate(.5, 80, 5.5);
                        player().capabilities.isFlying = true; player().sendPlayerAbilities();
                    });
                    phase = 20; waitUntil = ticks + 30;
                }
                case 20 -> {
                    if (!(mc.world.getTileEntity(RuntimeVerification.TERMINAL) instanceof ae2.tile.networking.TileCableBus bus)
                        || bus.getPart(net.minecraft.util.EnumFacing.SOUTH) == null) return;
                    server(() -> {
                        ae2.core.gui.GuiOpener.openPartGui(player(), ae2.container.GuiIds.GuiKey.PATTERN_ENCODING_TERMINAL,
                            RuntimeVerification.terminal);
                    });
                    phase = 2; waitUntil = ticks + 15;
                }
                case 2 -> {
                    if (!(mc.currentScreen instanceof GuiPatternEncodingTerm)) return;
                    require(!net.minecraft.client.resources.I18n.format("statpatterns.probability").equals("statpatterns.probability"), "Probability control translation is missing");
                    require(settings().statpatterns$enabled(), "Client wired menu lacks probability controls");
                    require(settings().statpatterns$probability() == .25 && settings().statpatterns$alpha() == .01, "Initial GUI state was not synchronized");
                    settings().statpatterns$setProbability(.6); settings().statpatterns$setAlpha(.05);
                    container().encode(); waitUntil = ticks + 15; phase++;
                }
                case 3 -> {
                    server(() -> {
                        var logic = RuntimeVerification.terminal.getLogic();
                        var encoded = StatPatternsDetails.decode(ae2.api.stacks.AEItemKey.of(logic.getEncodedPatternInv().getStackInSlot(0)), player().world);
                        wireApplied = encoded != null && encoded.probability() == .6 && encoded.alpha() == .05;
                    });
                    phase = 25; waitUntil = ticks + 5;
                }
                case 25 -> {
                    if (!wireApplied) { phase = 3; return; }
                    check("Real wired GUI action packets change chance/confidence and encode on the server");
                    capture = "wired-en_us.png"; phase = 4;
                }
                case 4 -> {
                    language("zh_cn"); capture = "wired-zh_cn.png"; phase++;
                }
                case 5 -> {
                    mc.player.closeScreen(); language("en_us");
                    phase = 21; waitUntil = ticks + 20;
                }
                case 21 -> {
                    server(() -> {
                        var stack = new ItemStack(StatPatternsMod.WIRELESS);
                        StatPatternsMod.WIRELESS.injectAEPower(stack, 100000, ae2.api.config.Actionable.MODULATE);
                        WirelessTerminalItem.LINKABLE_HANDLER.link(stack, player().world, RuntimeVerification.ORIGIN.south(7));
                        player().inventory.setInventorySlotContents(0, stack); player().inventory.currentItem = 0;
                    });
                    phase = 23; waitUntil = ticks + 20;
                }
                case 23 -> {
                    if (mc.player.getHeldItemMainhand().getItem() != StatPatternsMod.WIRELESS) return;
                    server(() -> {
                        StatPatternsMod.WIRELESS.onItemRightClick(player().world, player(), EnumHand.MAIN_HAND);
                        var logic = ((IPatternTerminalGuiHost) ((ContainerPatternEncodingTerm) player().openContainer).getTarget()).getLogic();
                        logic.setMode(ae2.parts.encoding.EncodingMode.PROCESSING);
                        logic.getEncodedInputInv().setStack(0, new ae2.api.stacks.GenericStack(ae2.api.stacks.AEItemKey.of(net.minecraft.init.Items.IRON_INGOT), 1));
                        logic.getEncodedOutputInv().setStack(0, new ae2.api.stacks.GenericStack(ae2.api.stacks.AEItemKey.of(net.minecraft.init.Items.GOLD_INGOT), 1));
                        logic.getBlankPatternInv().setItemDirect(0, AEItems.BLANK_PATTERN.stack());
                    });
                    phase = 6; waitUntil = ticks + 15;
                }
                case 6 -> {
                    if (!(mc.currentScreen instanceof GuiPatternEncodingTerm)) return;
                    require(settings().statpatterns$enabled(), "Wireless GUI does not recognize the probability terminal");
                    settings().statpatterns$setProbability(.35); settings().statpatterns$setAlpha(.01);
                    container().encode(); waitUntil = ticks + 15; phase++;
                }
                case 7 -> {
                    server(() -> {
                        var logic = ((IPatternTerminalGuiHost) ((ContainerPatternEncodingTerm) player().openContainer).getTarget()).getLogic();
                        var encoded = StatPatternsDetails.decode(ae2.api.stacks.AEItemKey.of(logic.getEncodedPatternInv().getStackInSlot(0)), player().world);
                        wirelessApplied = encoded != null && encoded.probability() == .35 && encoded.alpha() == .01;
                    });
                    phase = 26; waitUntil = ticks + 5;
                }
                case 26 -> {
                    if (!wirelessApplied) { phase = 7; return; }
                    capture = "wireless-en_us.png"; phase = 8;
                }
                case 8 -> {
                    mc.player.closeScreen();
                    phase = 22; waitUntil = ticks + 20;
                }
                case 22 -> {
                    server(() -> {
                        var item = AEItems.WIRELESS_UNIVERSAL_TERMINAL.item();
                        var universal = AEItems.WIRELESS_UNIVERSAL_TERMINAL.stack();
                        ((WirelessUniversalTerminalItem) item).addTerminal(universal, player().getHeldItemMainhand(), StatPatternsMod.WIRELESS);
                        ((WirelessUniversalTerminalItem) item).injectAEPower(universal, 100000, ae2.api.config.Actionable.MODULATE);
                        player().inventory.setInventorySlotContents(0, universal);
                    });
                    phase = 24; waitUntil = ticks + 20;
                }
                case 24 -> {
                    if (mc.player.getHeldItemMainhand().getItem() != AEItems.WIRELESS_UNIVERSAL_TERMINAL.item()) return;
                    server(() -> ((WirelessUniversalTerminalItem) AEItems.WIRELESS_UNIVERSAL_TERMINAL.item())
                        .onItemRightClick(player().world, player(), EnumHand.MAIN_HAND));
                    phase = 9; waitUntil = ticks + 15;
                }
                case 9 -> {
                    if (!(mc.currentScreen instanceof GuiPatternEncodingTerm)) return;
                    require(settings().statpatterns$enabled() && settings().statpatterns$probability() == .35
                        && settings().statpatterns$alpha() == .01, "Universal terminal lost the probability terminal's identity or settings");
                    check("Charged/linked wireless terminal encodes through native packets and retains settings in AE2S universal terminal");
                    capture = "universal-en_us.png"; phase++;
                }
                case 10 -> {
                    if (!heiVerified && Loader.isModLoaded("jei")) {
                        Class.forName("com.tz.statpatterns.verification.HeiVerification").getMethod("transfer").invoke(null);
                        phase = 30; waitUntil = ticks + 20; return;
                    }
                    if (Loader.isModLoaded("ponder-legacy")) {
                        Class.forName("com.tz.statpatterns.verification.PonderVerification").getMethod("verify").invoke(null);
                        capture = "ponder-en_us.png";
                    }
                    phase++;
                }
                case 30 -> {
                    server(() -> {
                        try {
                            Class.forName("com.tz.statpatterns.verification.HeiVerification")
                                .getMethod("verifyServer", net.minecraft.entity.player.EntityPlayerMP.class).invoke(null, player());
                        } catch (ReflectiveOperationException failure) { throw new RuntimeException(failure); }
                    });
                    heiVerified = true; capture = "hei-transfer-en_us.png"; phase = 10;
                }
                case 11 -> {
                    if (Loader.isModLoaded("ponder-legacy")) {
                        language("zh_cn");
                        Class.forName("com.tz.statpatterns.verification.PonderVerification").getMethod("verify").invoke(null);
                        capture = "ponder-zh_cn.png";
                    }
                    phase++;
                }
                default -> { RuntimeVerification.INSTANCE.write("PASS", null); done = true; mc.shutdown(); }
            }
        } catch (Throwable failure) {
            failure.printStackTrace(); RuntimeVerification.INSTANCE.write("FAIL", failure); done = true; mc.shutdown();
        }
    }
    private void language(String code) {
        mc.gameSettings.language = code;
        mc.getLanguageManager().setCurrentLanguage(mc.getLanguageManager().getLanguage(code)); mc.refreshResources();
    }
    @SubscribeEvent public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || capture == null) return;
        try {
            Path output = mc.gameDir.toPath().resolve("statpatterns-verification");
            ScreenShotHelper.saveScreenshot(output.toFile(), capture, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            require(org.lwjgl.opengl.GL11.glGetError() == 0, "GUI render left an OpenGL error");
            check("Rendered " + capture + " without OpenGL errors");
        } catch (Throwable failure) { RuntimeVerification.INSTANCE.write("FAIL", failure); done = true; mc.shutdown(); }
        capture = null; waitUntil = ticks + 8;
    }
}
