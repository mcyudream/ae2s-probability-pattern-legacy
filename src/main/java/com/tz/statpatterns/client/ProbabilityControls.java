package com.tz.statpatterns.client;

import com.tz.statpatterns.terminal.StatisticalContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Client-only controls use AE2S's authenticated, window-bound client action channel. */
public final class ProbabilityControls {
    private GuiScreen owner;
    private StatisticalContainer container;
    private GuiTextField probability;
    private int x, y;
    private static final int WIDTH = 108, HEIGHT = 88;

    @SubscribeEvent public void init(GuiScreenEvent.InitGuiEvent.Post event) {
        owner = null; container = null; probability = null;
        if (!(event.getGui() instanceof GuiContainer gui)
            || !(gui.inventorySlots instanceof StatisticalContainer settings) || !settings.statpatterns$enabled()) return;
        owner = gui; container = settings;
        x = Math.max(2, gui.getGuiLeft() - WIDTH - 48);
        y = Math.max(2, gui.getGuiTop() + 44);
        probability = new GuiTextField(734, Minecraft.getMinecraft().fontRenderer, x + 8, y + 23, WIDTH - 16, 18);
        probability.setMaxStringLength(24);
        probability.setText(Double.toString(container.statpatterns$probability()));
    }
    @SubscribeEvent public void draw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (event.getGui() != owner || probability == null) return;
        var font = Minecraft.getMinecraft().fontRenderer;
        Gui.drawRect(x, y, x + WIDTH, y + HEIGHT, 0xee20242d);
        Gui.drawRect(x, y, x + WIDTH, y + 2, 0xffb477dd);
        font.drawString(I18n.format("statpatterns.probability"), x + 8, y + 8, 0xffffff);
        if (!probability.isFocused()) probability.setText(Double.toString(container.statpatterns$probability()));
        probability.drawTextBox();
        String confidence = String.format(java.util.Locale.ROOT, "%.0f%%", 100 * (1 - container.statpatterns$alpha()));
        Gui.drawRect(x + 7, y + 47, x + WIDTH - 7, y + 65, 0xff4c3c61);
        font.drawString(I18n.format("statpatterns.confidence") + " " + confidence, x + 11, y + 52, 0xffffff);
        font.drawString(I18n.format("statpatterns.primary_only"), x + 7, y + 73, 0xbac2d1);
    }
    @SubscribeEvent public void mouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (event.getGui() != owner || probability == null || !Mouse.getEventButtonState() || Mouse.getEventButton() != 0) return;
        var mc = Minecraft.getMinecraft();
        int mx = Mouse.getEventX() * owner.width / mc.displayWidth;
        int my = owner.height - Mouse.getEventY() * owner.height / mc.displayHeight - 1;
        probability.mouseClicked(mx, my, 0);
        if (mx < x || mx >= x + WIDTH || my < y || my >= y + HEIGHT) return;
        if (my >= y + 47 && my < y + 65) container.statpatterns$setAlpha(container.statpatterns$alpha() == .05 ? .01 : .05);
        event.setCanceled(true);
    }
    @SubscribeEvent public void keyboard(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (event.getGui() != owner || probability == null || !probability.isFocused() || !Keyboard.getEventKeyState()) return;
        int key = Keyboard.getEventKey();
        if (key == Keyboard.KEY_ESCAPE || key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            probability.setFocused(false);
        } else {
            probability.textboxKeyTyped(Keyboard.getEventCharacter(), key);
            try {
                String input = probability.getText().trim();
                double value = input.endsWith("%") ? Double.parseDouble(input.substring(0, input.length() - 1)) / 100 : Double.parseDouble(input);
                if (!Double.isFinite(value) || value <= 0 || value > 1) throw new NumberFormatException();
                container.statpatterns$setProbability(value);
                probability.setTextColor(0xffffff);
            } catch (NumberFormatException invalid) { probability.setTextColor(0xff7777); }
        }
        event.setCanceled(true);
    }
}
