package io.github.pyke.blockdetector.client.hud;

import io.github.pyke.blockdetector.config.ConfigAccess;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class HudRenderer {
    public static void register() {
        HudRenderCallback.EVENT.register((graphics, tickDelta) -> render(graphics));
    }

    private static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (!HudState.shouldRender(mc)) { return; }

        String text = HudState.currentText(mc);
        if (null == text || text.isBlank()) { return; }

        var font = mc.font;
        int x = graphics.guiWidth() / 2 + ConfigAccess.CONFIG.holdHud().hudPosX;
        int y = graphics.guiHeight() - ConfigAccess.CONFIG.holdHud().hudPosY;
        graphics.drawCenteredString(font, text, x, y, 0xFFFFFF);
    }
}
