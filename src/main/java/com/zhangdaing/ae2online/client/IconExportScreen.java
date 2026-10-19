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
 * 布局：物品渲染在左上角固定区域，进度信息全部画在它下方，绝不重叠
 *      （否则截出来的图会带上进度条像素）。
 *
 * 两帧流水（关键，否则会截到上一帧的错误内容）：
 *   帧 N   ：render() 把当前物品画进画面，随后 markFrameRendered()
 *   帧 N+1 ：render() 开头先 captureAndSave()（此时 framebuffer 里已是帧 N 的画面），
 *            再画下一个物品
 */
public class IconExportScreen extends Screen {

    /** 背景色；导出时整屏填充，之后按此色扣成透明 */
    public static final int BACKGROUND_COLOR = 0xFF1A1A1A;
    /** 渲染区左上角 */
    public static final int RENDER_X = 8;
    public static final int RENDER_Y = 8;

    private final List<ItemStack> items;
    /** 是否已经画过至少一帧（第一帧只画不截） */
    private boolean firstFrameDrawn = false;

    public IconExportScreen(List<ItemStack> items) {
        super(Component.literal("AE2 Online - 导出图标"));
        this.items = new ArrayList<>(items);
    }

    @Override
    protected void init() {
        // 无子控件，全部自绘；告知导出器渲染上下文
        ItemIconExporter.setRenderContext(RENDER_X, RENDER_Y, BACKGROUND_COLOR);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var p = ItemIconExporter.progress();

        // ---- 帧 N+1 开头：先把上一帧的画面截下来 ----
        if (firstFrameDrawn) {
            ItemIconExporter.captureAndSave();
            p = ItemIconExporter.progress();
            if (p.state != ItemIconExporter.State.RUNNING) {
                notifyPlayer(p.message);
                this.onClose();
                return;
            }
        }

        // ---- 整屏纯色背景 ----
        graphics.fill(0, 0, this.width, this.height, BACKGROUND_COLOR);

        // ---- 画当前物品（渲染区 = RENDER_X/Y 起 16 逻辑像素 × 倍数） ----
        ItemStack current = ItemIconExporter.currentItem();
        int iconSize = ItemIconExporter.iconSize();
        if (current != null) {
            int mult = iconSize / 16;
            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(RENDER_X, RENDER_Y, 0);
            pose.scale(mult, mult, 1.0f);
            graphics.renderItem(current, 0, 0);
            pose.popPose();
        }

        // ---- 进度信息（渲染区下方，避免被截图） ----
        int textY = RENDER_Y + iconSize + 10;
        var font = this.font;
        graphics.drawString(font, "AE2 Online 图标导出", 8, textY, 0xFFFFFF);

        String line = String.format("%d / %d    已写入 %d    跳过 %d",
                Math.min(p.done, p.total), p.total, p.written, p.skipped);
        graphics.drawString(font, line, 8, textY + 14, 0xCCCCCC);

        int barW = Math.min(320, Math.max(120, this.width - 16));
        int barY = textY + 30;
        graphics.fill(8, barY, 8 + barW, barY + 6, 0xFF333333);
        int filled = p.total <= 0 ? 0 : (int) ((long) barW * Math.min(p.done + 1, p.total) / p.total);
        graphics.fill(8, barY, 8 + filled, barY + 6, 0xFF4EC9B0);

        graphics.drawString(font, "ESC 取消", 8, barY + 14, 0x888888);

        // ---- 标记本帧已渲染，下一帧可以截图 ----
        firstFrameDrawn = true;
        ItemIconExporter.markFrameRendered();
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
