package com.tz.statpatterns;

import ae2.api.crafting.IPatternDetailsDecoder;
import ae2.api.crafting.PatternDetailsHelper;
import ae2.api.parts.PartModels;
import ae2.api.stacks.AEItemKey;
import ae2.items.parts.PartItem;
import com.tz.statpatterns.crafting.StatPatternsDetails;
import com.tz.statpatterns.crafting.StatPatternsItem;
import com.tz.statpatterns.item.StatPatternsWirelessItem;
import com.tz.statpatterns.part.StatPatternsTerminalPart;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = StatPatternsMod.ID, name = "AE2S Probability Pattern Legacy", version = "1.0.2",
    acceptedMinecraftVersions = "[1.12.2]",
    dependencies = "required-after:cleanroom@[0.4.0,);required-after:ae2@[1.1.0,1.2.0);after:ponder-legacy")
@Mod.EventBusSubscriber(modid = StatPatternsMod.ID)
public final class StatPatternsMod {
    public static final String ID = "ae2s-probability-pattern-legacy";
    public static final String NAMESPACE = "statpatterns";
    public static StatPatternsItem PATTERN;
    public static PartItem<StatPatternsTerminalPart> TERMINAL;
    public static StatPatternsWirelessItem WIRELESS;
    public static final CreativeTabs TAB = new CreativeTabs(ID) {
        @Override public ItemStack createIcon() { return new ItemStack(PATTERN); }
    };
    @SidedProxy(clientSide = "com.tz.statpatterns.client.ClientProxy",
        serverSide = "com.tz.statpatterns.CommonProxy")
    public static CommonProxy proxy;

    public static ResourceLocation id(String path) { return new ResourceLocation(NAMESPACE, path); }

    @SubscribeEvent public static void items(RegistryEvent.Register<Item> event) {
        PATTERN = register(new StatPatternsItem(), "probability_pattern");
        TERMINAL = register(new PartItem<>(StatPatternsTerminalPart.class, StatPatternsTerminalPart::new),
            "probability_pattern_terminal");
        WIRELESS = register(new StatPatternsWirelessItem(), "wireless_probability_pattern_terminal");
        event.getRegistry().registerAll(PATTERN, TERMINAL, WIRELESS);
        PatternDetailsHelper.registerDecoder(new IPatternDetailsDecoder() {
            @Override public boolean isEncodedPattern(ItemStack stack) {
                return stack.getItem() == PATTERN && stack.hasTagCompound()
                    && stack.getTagCompound().hasKey(StatPatternsDetails.TAG, 10);
            }
            @Override public StatPatternsDetails decodePattern(AEItemKey what, World world) {
                return StatPatternsDetails.decode(what, world);
            }
        });
    }

    private static <T extends Item> T register(T item, String path) {
        item.setRegistryName(id(path));
        item.setTranslationKey(NAMESPACE + "." + path);
        item.setCreativeTab(TAB);
        return item;
    }

    @Mod.EventHandler public void preInit(FMLPreInitializationEvent event) {
        PartModels.registerModels(StatPatternsTerminalPart.OFF, StatPatternsTerminalPart.ON);
        proxy.preInit();
    }
    @Mod.EventHandler public void init(FMLInitializationEvent event) { proxy.init(); }
}

