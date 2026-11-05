package com.tz.statpatterns.integration.jei;

import ae2.client.gui.me.patternencode.GuiPatternEncodingTerm;
import com.tz.statpatterns.terminal.StatisticalContainer;
import java.awt.Rectangle;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.gui.IAdvancedGuiHandler;

@JEIPlugin
public final class ProbabilityJeiPlugin implements IModPlugin {
    @Override public void register(IModRegistry registry) {
        registry.addAdvancedGuiHandlers(new IAdvancedGuiHandler<GuiPatternEncodingTerm>() {
            @Override public Class<GuiPatternEncodingTerm> getGuiContainerClass() { return GuiPatternEncodingTerm.class; }
            @Override public List<Rectangle> getGuiExtraAreas(GuiPatternEncodingTerm gui) {
                return ((StatisticalContainer) gui.inventorySlots).statpatterns$enabled()
                    ? List.of(new Rectangle(Math.max(2, gui.getGuiLeft() - 156), Math.max(2, gui.getGuiTop() + 44), 108, 88))
                    : List.of();
            }
        });
    }
}
