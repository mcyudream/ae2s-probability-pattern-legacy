package com.tz.statpatterns.item;

import ae2.items.tools.powered.WirelessTerminalItem;
import ae2.helpers.WirelessPatternEncodingTerminalGuiHost;
import ae2.container.me.patternencode.ContainerPatternEncodingTerm;
import ae2.container.GuiIds;
import com.tz.statpatterns.StatPatternsMod;
import net.minecraft.item.ItemStack;

/** AE2S supplies charging, linking, upgrades and universal-terminal composition. */
public final class StatPatternsWirelessItem extends WirelessTerminalItem {
    public StatPatternsWirelessItem() {
        super(1_600_000, "statpatterns_probability", ItemStack::new,
            WirelessPatternEncodingTerminalGuiHost::new,
            (definition, inventory, host) -> host instanceof WirelessPatternEncodingTerminalGuiHost patternHost
                ? new ContainerPatternEncodingTerm(GuiIds.GuiKey.WIRELESS_TERMINAL_DYNAMIC, inventory, patternHost, true) : null,
            (definition, container, inventory) -> StatPatternsMod.proxy.wirelessScreen(container, inventory),
            "statpatterns_probability", 2);
    }
}

