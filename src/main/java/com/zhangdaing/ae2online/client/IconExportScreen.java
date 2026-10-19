package com.zhangdaing.ae2online.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 图标导出专用界面。
 *
 * 完全照 icon-exporter 的做法：
 *   物品必须画在屏幕 **左上角 (0,0)**，尺寸 = iconSize。
 *   截图后直接从 (0,0) 裁 iconSize × iconSize，不需要任何坐标换算
 *   （guiScale=1 时天然对齐；guiScale>1 时按倍数放大裁剪后缩回）。
 *
 * 截图时机：在同一 render() 里"画完物品后立刻截"，与 icon-exporter 一致。
 *
 * 进度 UI 画在物品区域 **下方**，避免被截进去。
 */
public class IconExportScreen extends Screen {

    /** 背景色：品红（罕见色，便于可靠抠图；避免与物品本身颜色冲突） */
    public static final int BACKGROUND_COLOR = 0xFFFF00FF;
    /** 物品渲染原点 —— 必须是 (0,0)，与截图裁剪原点对齐 */
    public static final int RENDER_X = 0;
    public static final int RENDER_Y = 0;

    private final List<ItemStack> items;
    private boolean closing = false;

    public IconExportScreen(List<ItemStack> items) {
        super(Component.literal("AE2 Online - 导出图标"));
        this.items = new ArrayList<>(items);
    }

    @Override
    protected void init() {
        ItemIconExporter.setBackgroundColor(BACKGROUND_COLOR);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var p = ItemIconExporter.progress();
        if (p.state != ItemIconExporter.State.RUNNING) {
            if (!closing) {
                closing = true;
                notifyPlayer(p.message);
                this.onClose();
            }
            graphics.fill(0, 0, this.width, this.height, 0xFF000000);
            return;
        }

        int iconSize = ItemIconExporter.iconSize();
        ItemStack current = ItemIconExporter.currentItem();

        // ---------- 1. 整屏黑色（界面观感） ----------
        graphics.fill(0, 0, this.width, this.height, 0xFF000000);

        // ---------- 2. 只把「截图区域」涂成品红 ----------
        // 品红仅用于可靠抠图，所以只覆盖 (0,0)-(iconSize,iconSize) 这一小块，
        // 其余区域留黑，避免整个界面变成刺眼的紫色。
        if (iconSize > 0) {
            graphics.fill(RENDER_X, RENDER_Y,
                    RENDER_X + iconSize, RENDER_Y + iconSize,
                    BACKGROUND_COLOR);
        }

        // ---------- 3. 物品画在 (0,0)，缩放到 iconSize ----------
        if (current != null && iconSize > 0) {
            int mult = Math.max(1, iconSize / 16);
            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.scale(mult, mult, 1.0f);
            graphics.renderItem(current, RENDER_X, RENDER_Y);
            pose.popPose();
        }

        // ---------- 4. 立刻截图（同帧，从 (0,0) 裁） ----------
        ItemIconExporter.captureAndSaveNow();

        // ---------- 5. 进度 UI（画在物品区下方，不影响截图） ----------
        int textY = Math.max(iconSize, 16) + 20;
        var font = this.font;
        var p2 = ItemIconExporter.progress();

        graphics.drawString(font, "AE2 Online 图标导出", 8, textY, 0xFFFFFF);

        String line = String.format("%d / %d    已写入 %d    跳过 %d",
                Math.min(p2.done, p2.total), p2.total, p2.written, p2.skipped);
        graphics.drawString(font, line, 8, textY + 14, 0xCCCCCC);

        int barW = Math.min(320, Math.max(120, this.width - 16));
        int barY = textY + 30;
        graphics.fill(8, barY, 8 + barW, barY + 6, 0xFF333333);
        int progress = p2.total <= 0 ? 0 : Math.min(p2.done + 1, p2.total);
        int filled = p2.total <= 0 ? 0 : (int) ((long) barW * progress / p2.total);
        graphics.fill(8, barY, 8 + filled, barY + 6, 0xFF4EC9B0);

        graphics.drawString(font, "ESC 取消", 8, barY + 14, 0x888888);
    }

    private void notifyPlayer(String msg) {
        if (this.minecraft == null || this.minecraft.player == null) return;
        if (msg == null || msg.isBlank()) return;
        this.minecraft.player.displayClientMessage(
                Component.literal("[AE2 Online] " + msg), false);
    }

    @Override
    public void onClose() {
        if (ItemIconExporter.isRunning()) {
            ItemIconExporter.cancel();
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
