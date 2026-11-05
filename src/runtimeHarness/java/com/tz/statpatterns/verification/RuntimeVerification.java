package com.tz.statpatterns.verification;

import ae2.api.config.Actionable;
import ae2.api.crafting.PatternDetailsHelper;
import ae2.api.networking.GridHelper;
import ae2.api.networking.crafting.CalculationStrategy;
import ae2.api.networking.crafting.ICraftingPlan;
import ae2.api.networking.crafting.ICraftingSimulationRequester;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.GenericStack;
import ae2.core.definitions.AEBlocks;
import ae2.core.definitions.AEItems;
import ae2.tile.crafting.TileCraftingUnit;
import ae2.tile.crafting.TilePatternProvider;
import ae2.tile.networking.TileCableBus;
import ae2.tile.networking.TileCreativeEnergyCell;
import ae2.tile.storage.TileDrive;
import com.google.gson.GsonBuilder;
import com.tz.statpatterns.StatPatternsMod;
import com.tz.statpatterns.crafting.StatPatternsDetails;
import com.tz.statpatterns.math.StatPatternsSizing;
import com.tz.statpatterns.part.StatPatternsTerminalPart;
import com.tz.statpatterns.terminal.StatisticalContainer;
import com.tz.statpatterns.terminal.StatisticalSettings;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** An external SRG addon: release code is exercised through the installed AE2S grid and CPU. */
@Mod(modid = "statpatterns_verification", name = "Probability Pattern Verification", version = "1",
    dependencies = "required-after:ae2;required-after:ae2s-probability-pattern-legacy")
public final class RuntimeVerification {
    public static RuntimeVerification INSTANCE;
    public static volatile boolean gameplayDone;
    public static volatile boolean failed;
    public static volatile StatPatternsTerminalPart terminal;
    public static final BlockPos ORIGIN = new BlockPos(0, 80, 0), TERMINAL = ORIGIN.south(4);
    public final List<String> checks = new ArrayList<>();
    private MinecraftServer server;
    private WorldServer world;
    private ForgeChunkManager.Ticket fixtureTicket;
    private TileCreativeEnergyCell power;
    private TileCraftingUnit cpu;
    private TileDrive drive;
    private ae2.tile.networking.TileWirelessAccessPoint wirelessAccess;
    private TilePatternProvider first, second;
    private Future<ICraftingPlan> calculation;
    private int phase, ticks, ironAttempts, goldAttempts;
    private long expectedIron, expectedGold;
    private boolean finished;

    @Mod.EventHandler public void init(FMLInitializationEvent event) throws Exception {
        INSTANCE = this;
        ForgeChunkManager.setForcedChunkLoadingCallback(this,
            (tickets, level) -> tickets.forEach(ForgeChunkManager::releaseTicket));
        MinecraftForge.EVENT_BUS.register(this);
        if (event.getSide().isClient())
            Class.forName("com.tz.statpatterns.verification.ClientVerification").getMethod("install").invoke(null);
    }
    @Mod.EventHandler public void started(FMLServerStartedEvent event) {
        server = FMLCommonHandler.instance().getMinecraftServerInstance(); world = server.getWorld(0);
        // Superflat spawn selection can place the player thousands of blocks from the fixture.
        // Keep every machine chunk ticking independently of client join/loading timing.
        world.setSpawnPoint(ORIGIN.south(5));
        world.getGameRules().setOrCreateGameRule("spawnRadius", "0");
        fixtureTicket = ForgeChunkManager.requestTicket(this, world, ForgeChunkManager.Type.NORMAL);
        require(fixtureTicket != null, "Could not keep verification machine chunks loaded");
        for (int x = -1; x <= 0; x++) for (int z = -1; z <= 0; z++)
            ForgeChunkManager.forceChunk(fixtureTicket, new ChunkPos(x, z));
    }
    @Mod.EventHandler public void stopping(FMLServerStoppingEvent event) {
        if (fixtureTicket != null) {
            ForgeChunkManager.releaseTicket(fixtureTicket);
            fixtureTicket = null;
        }
    }
    public static void require(boolean value, String description) {
        if (!value) throw new IllegalStateException(description);
    }
    public synchronized void checked(String description) {
        checks.add(description); write("RUNNING", null);
    }
    public synchronized void write(String result, Throwable error) {
        try {
            Path out = Path.of("statpatterns-verification"); Files.createDirectories(out);
            var report = new java.util.LinkedHashMap<String, Object>();
            report.put("result", result);
            report.put("cleanroom", Loader.instance().getIndexedModList().get("cleanroom").getVersion());
            report.put("java", System.getProperty("java.version"));
            report.put("ae2s", Loader.instance().getIndexedModList().get("ae2").getVersion());
            report.put("ponder", Loader.isModLoaded("ponder-legacy"));
            report.put("checks", List.copyOf(checks));
            report.put("planned_iron", expectedIron); report.put("planned_gold", expectedGold);
            report.put("machine_iron_attempts", ironAttempts); report.put("machine_gold_attempts", goldAttempts);
            report.put("phase", phase); report.put("server_ticks", ticks);
            report.put("fixture_chunks_forced", fixtureTicket == null ? 0 : fixtureTicket.getChunkList().size());
            if (error != null) {
                var trace = new java.io.StringWriter(); error.printStackTrace(new java.io.PrintWriter(trace));
                report.put("failure", trace.toString());
            }
            Files.writeString(out.resolve("report.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
            if ("FAIL".equals(result)) failed = true;
        } catch (Exception failure) { throw new RuntimeException(failure); }
    }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || server == null || finished) return;
        try {
            if (++ticks > 2400) throw new IllegalStateException("Runtime verification timeout at phase " + phase);
            switch (phase) {
                case 0 -> create();
                case 1 -> connect();
                case 2 -> encodeAndPlan();
                case 3 -> submit();
                case 4 -> complete();
                case 5 -> stockedPlan();
                case 6 -> finishGameplay();
            }
        } catch (Throwable failure) {
            failure.printStackTrace(); finished = true; write("FAIL", failure); server.initiateShutdown();
        }
    }
    private void place(BlockPos position, net.minecraft.block.Block block) {
        world.getChunk(position); world.setBlockToAir(position); world.setBlockState(position, block.getDefaultState(), 3);
    }
    private void create() {
        require("0.4.0".equals(Loader.instance().getIndexedModList().get("cleanroom").getVersion()), "Wrong Cleanroom baseline");
        place(ORIGIN, AEBlocks.CREATIVE_ENERGY_CELL.block());
        place(ORIGIN.west(4), AEBlocks.CRAFTING_STORAGE_64K.block());
        place(ORIGIN.east(4), AEBlocks.DRIVE.block());
        place(ORIGIN.north(4), AEBlocks.PATTERN_PROVIDER.block());
        place(ORIGIN.north(4).east(), Blocks.CHEST);
        place(ORIGIN.north(8), AEBlocks.PATTERN_PROVIDER.block());
        place(ORIGIN.north(8).east(), Blocks.CHEST);
        place(TERMINAL, AEBlocks.CABLE_BUS.block());
        place(ORIGIN.south(7), AEBlocks.WIRELESS_ACCESS_POINT.block());
        power = (TileCreativeEnergyCell) world.getTileEntity(ORIGIN);
        cpu = (TileCraftingUnit) world.getTileEntity(ORIGIN.west(4));
        drive = (TileDrive) world.getTileEntity(ORIGIN.east(4));
        first = (TilePatternProvider) world.getTileEntity(ORIGIN.north(4));
        second = (TilePatternProvider) world.getTileEntity(ORIGIN.north(8));
        wirelessAccess = (ae2.tile.networking.TileWirelessAccessPoint) world.getTileEntity(ORIGIN.south(7));
        terminal = ((TileCableBus) world.getTileEntity(TERMINAL)).getCableBus()
            .addPart(StatPatternsMod.TERMINAL, EnumFacing.SOUTH, null);
        drive.getInternalInventory().setItemDirect(0, AEItems.ITEM_CELL_64K.stack());
        first.getLogic().getPatternInv().setItemDirect(0,
            StatPatternsDetails.encode(List.of(new GenericStack(AEItemKey.of(Items.IRON_INGOT), 2)),
                List.of(new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 2)), .5, .05));
        second.getLogic().getPatternInv().setItemDirect(0,
            StatPatternsDetails.encode(List.of(new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 3)),
                List.of(new GenericStack(AEItemKey.of(Items.DIAMOND), 1)), .8, .01));
        phase++;
    }
    private void connect() {
        var endpoints = List.of(cpu.getMainNode(), drive.getMainNode(), first.getMainNode(), second.getMainNode(), terminal.getMainNode(), wirelessAccess.getMainNode());
        if (!power.getMainNode().isReady() || endpoints.stream().anyMatch(n -> !n.isReady())) return;
        for (var endpoint : endpoints) GridHelper.createConnection(power.getMainNode().getNode(), endpoint.getNode());
        phase++;
    }
    private void encodeAndPlan() {
        if (!first.getMainNode().isActive() || !second.getMainNode().isActive()
            || power.getMainNode().getGrid().getCraftingService().getCpus().isEmpty()) return;
        var grid = power.getMainNode().getGrid();
        if (grid.getCraftingService().getCraftingFor(AEItemKey.of(Items.DIAMOND)).isEmpty()) return;
        checked("Cleanroom 0.4.0 packaged addon creates native powered provider/CPU/storage/terminal nodes");
        var logic = terminal.getLogic();
        var player = FakePlayerFactory.getMinecraft(world);
        var container = new ae2.container.me.patternencode.ContainerPatternEncodingTerm(player.inventory, terminal);
        var settings = (StatisticalContainer) container;
        require(settings.statpatterns$enabled(), "Wired probability terminal is not recognized");
        settings.statpatterns$setProbability(.25); settings.statpatterns$setAlpha(.01);
        settings.statpatterns$setProbability(Double.NaN);
        require(settings.statpatterns$probability() == .25, "NaN client action accepted");
        container.setMode(ae2.parts.encoding.EncodingMode.PROCESSING);
        logic.getEncodedInputInv().setStack(0, new GenericStack(AEItemKey.of(Items.IRON_INGOT), 2));
        logic.getEncodedOutputInv().setStack(0, new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 3));
        logic.getBlankPatternInv().setItemDirect(0, AEItems.BLANK_PATTERN.stack(2));
        container.encode();
        var encoded = logic.getEncodedPatternInv().getStackInSlot(0);
        var pattern = (StatPatternsDetails) PatternDetailsHelper.decodePattern(encoded, world);
        require(pattern != null && pattern.probability() == .25 && pattern.alpha() == .01, "Terminal did not encode statistical parameters");
        require(logic.getBlankPatternInv().getStackInSlot(0).getCount() == 1, "Encoding consumed an incorrect number of blank patterns");
        var batch = pattern.forRequest(10);
        require(batch.attempts() == StatPatternsSizing.planAttempts(4, .25, .01, 30).attempts(), "Per-success output count ignored");
        var restored = PatternDetailsHelper.decodePattern(AEItemKey.fromTag(batch.getDefinition().toTag()), world);
        require(batch.equals(restored) && restored.getInputs()[0].getMultiplier() == batch.getInputs()[0].getMultiplier(), "Planned pattern lost sizing through NBT");
        require(!pattern.forRequest(7).equals(pattern.forRequest(10)), "Different-sized CPU tasks collide");
        var saved = new NBTTagCompound(); logic.writeToNBT(saved);
        var loaded = new ae2.parts.encoding.PatternEncodingLogic(terminal); loaded.readFromNBT(saved);
        require(((StatisticalSettings) loaded).statpatterns$probability() == .25, "Terminal NBT lost probability");
        require(((StatisticalSettings) loaded).statpatterns$alpha() == .01, "Terminal NBT lost confidence");
        // Loading an encoded pattern restores its ghost slots and confidence, including in a normal AE2S editor.
        loaded.getEncodedPatternInv().setItemDirect(0, ItemStack.EMPTY);
        loaded.getEncodedPatternInv().setItemDirect(0, encoded.copy());
        require(loaded.getEncodedInputInv().getStack(0).amount() == 2, "Pattern editor did not restore inputs");
        checked("Wired encoding consumes one blank; probability/confidence, multi-output amount and ghost slots round-trip");
        checked("Per-request CPU task identities and scaled inputs survive actual AE2S pattern serialization");
        var inventory = grid.getStorageService().getInventory();
        require(inventory.insert(AEItemKey.of(Items.IRON_INGOT), 20000, Actionable.MODULATE, IActionSource.empty()) == 20000, "Real 64k cell rejected iron");
        expectedGold = 3 * StatPatternsSizing.planAttempts(5, .8, .01, 30).attempts();
        expectedIron = 2 * StatPatternsSizing.planAttempts(StatPatternsDetails.divideCeil(expectedGold, 2), .5, .05, 30).attempts();
        calculation = grid.getCraftingService().beginCraftingCalculation(world, requester(), AEItemKey.of(Items.DIAMOND), 5, CalculationStrategy.REPORT_MISSING_ITEMS);
        phase++;
    }
    private ICraftingSimulationRequester requester() {
        return new ICraftingSimulationRequester() {
            @Override public IActionSource getActionSource() { return IActionSource.empty(); }
            @Override public ae2.api.networking.IGridNode getGridNode() { return power.getMainNode().getNode(); }
        };
    }
    private void submit() throws Exception {
        if (!calculation.isDone()) return;
        var plan = calculation.get();
        require(!plan.simulation() && plan.missingItems().isEmpty(), "Probability chain reports missing items: " + plan.missingItems());
        require(plan.usedItems().get(AEItemKey.of(Items.IRON_INGOT)) == expectedIron,
            "Probability chain used incorrect iron: " + plan.usedItems() + " expected " + expectedIron);
        require(plan.patternTimes().size() == 2 && plan.patternTimes().values().stream().allMatch(n -> n.longValue() == 1), "Sized chain should dispatch one batch at each stage");
        for (var pattern : plan.patternTimes().keySet()) require(pattern instanceof StatPatternsDetails p && p.isPlanned(), "Unscaled pattern entered the CPU plan");
        checked("Native planner sizes both probability stages independently, including two outputs per success");
        var accepted = power.getMainNode().getGrid().getCraftingService().submitJob(plan, null, null, false, IActionSource.empty());
        require(accepted.successful(), "Native CPU rejected plan: " + accepted.errorCode());
        var saved = new NBTTagCompound(); cpu.getCluster().writeToNBT(saved);
        cpu.getCluster().readFromNBT(saved);
        var again = new NBTTagCompound(); cpu.getCluster().writeToNBT(again);
        require(saved.equals(again), "Actual CPU serialization changed pending tasks");
        checked("Actual native CPU saves and reloads the pending scaled job before machine execution");
        phase++;
    }
    private void complete() {
        process((TileEntityChest) world.getTileEntity(first.getPos().east()), true);
        process((TileEntityChest) world.getTileEntity(second.getPos().east()), false);
        if (cpu.getCluster().isBusy()) return;
        var inventory = power.getMainNode().getGrid().getStorageService().getInventory();
        long diamonds = inventory.extract(AEItemKey.of(Items.DIAMOND), Long.MAX_VALUE, Actionable.SIMULATE, IActionSource.empty());
        if (diamonds < 5) return;
        require(ironAttempts * 2L == expectedIron, "Provider dispatched incorrect iron count");
        require(goldAttempts * 3L == expectedGold, "Provider dispatched incorrect gold count");
        require(inventory.extract(AEItemKey.of(Items.IRON_INGOT), Long.MAX_VALUE, Actionable.SIMULATE, IActionSource.empty()) == 20000 - expectedIron, "CPU input accounting changed");
        checked("Real native providers feed external chest machines, return chain output, finish CPU and conserve exact inputs");
        // A second calculation must account for intermediate stock before sizing its probability batch.
        inventory.extract(AEItemKey.of(Items.GOLD_INGOT), Long.MAX_VALUE, Actionable.MODULATE, IActionSource.empty());
        inventory.insert(AEItemKey.of(Items.GOLD_INGOT), 7, Actionable.MODULATE, IActionSource.empty());
        calculation = power.getMainNode().getGrid().getCraftingService().beginCraftingCalculation(world, requester(), AEItemKey.of(Items.DIAMOND), 5, CalculationStrategy.REPORT_MISSING_ITEMS);
        phase++;
    }
    private void process(TileEntityChest chest, boolean iron) {
        var ingredient = iron ? Items.IRON_INGOT : Items.GOLD_INGOT;
        int count = 0;
        for (int slot = 0; slot < chest.getSizeInventory(); slot++) {
            var stack = chest.getStackInSlot(slot);
            if (stack.getItem() == ingredient) { count += stack.getCount(); chest.setInventorySlotContents(slot, ItemStack.EMPTY); }
        }
        if (count == 0) return;
        int input = iron ? 2 : 3;
        require(count % input == 0, "Machine received partial recipe");
        int successes = 0;
        for (int attempt = 0; attempt < count / input; attempt++) {
            if (iron ? ++ironAttempts % 2 == 0 : ++goldAttempts % 5 != 0) successes++;
        }
        long output = successes * (iron ? 2L : 1L);
        if (output > 0) require(power.getMainNode().getGrid().getStorageService().getInventory().insert(
            AEItemKey.of(iron ? Items.GOLD_INGOT : Items.DIAMOND), output, Actionable.MODULATE, IActionSource.empty()) == output, "Native network rejected machine output");
    }
    private void stockedPlan() throws Exception {
        if (!calculation.isDone()) return;
        var plan = calculation.get();
        long needed = 2 * StatPatternsSizing.planAttempts(StatPatternsDetails.divideCeil(expectedGold - 7, 2), .5, .05, 30).attempts();
        require(plan.usedItems().get(AEItemKey.of(Items.GOLD_INGOT)) == 7, "Existing intermediate stock ignored");
        require(plan.usedItems().get(AEItemKey.of(Items.IRON_INGOT)) == needed, "Stock subtraction did not precede probability sizing");
        checked("A repeated request after intermediate stock changes sizes only the missing intermediate quantity");
        phase++;
    }
    private void finishGameplay() {
        gameplayDone = true; finished = true;
        if (FMLCommonHandler.instance().getSide().isServer()) { write("PASS", null); server.initiateShutdown(); }
    }
}
