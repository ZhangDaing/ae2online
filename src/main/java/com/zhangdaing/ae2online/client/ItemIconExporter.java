package com.zhangdaing.ae2online.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 物品图标导出器（客户端侧）。
 *
 * 实现完全参照 icon-exporter 模组的做法：
 *   1. 在 render() 内，把物品画在 **屏幕左上角 (0,0)**，尺寸 = size × size
 *   2. 同一帧内调用 Screenshot.takeScreenshot(RenderTarget)
 *   3. 从截图的 **左上角 (0,0)** 直接裁 size × size
 *   4. 背景色像素扣成透明后保存
 *
 * 关键点（来自对 icon-exporter 字节码的分析）：
 *   - 它不做 guiScale 换算，也不处理 flipY
 *   - 因为它把物品画在 (0,0)，而 Screenshot 拿到的图像原点是左上角，
 *     只要 guiScale=1 两者就天然对齐
 *   - 若 guiScale>1，需按倍数换算（本实现已处理）
 *
 * 文件名格式与 icon-exporter 严格一致。
 */
public final class ItemIconExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger("AE2Online IconExport");

    private ItemIconExporter() {}

    public enum State { IDLE, RUNNING, DONE, CANCELLED, ERROR }

    public static final class Progress {
        public State state = State.IDLE;
        public int done = 0;
        public int total = 0;
        public int written = 0;
        public int skipped = 0;
        public String message = "";
        public String error = "";

        public double ratio() {
            return total <= 0 ? 0.0 : Math.min(1.0, (double) done / total);
        }
    }

    private static final Progress PROGRESS = new Progress();

    private static List<ItemStack> pending = new ArrayList<>();
    /** 本次导出已写过的文件名，用于去重（磁盘已有文件仍会覆盖） */
    private static final java.util.Set<String> writtenNames = new java.util.HashSet<>();
    private static Path outputDir;
    /** 图标边长（最终 PNG 尺寸，像素） */
    private static int iconSize = 64;
    /** 背景色（用于扣透明） */
    private static int backgroundColor = 0x00FF00FF;

    public static Progress progress() {
        return PROGRESS;
    }

    public static boolean isRunning() {
        return PROGRESS.state == State.RUNNING;
    }

    public static int iconSize() {
        return iconSize;
    }

    /** 收集要导出的物品 */
    public static List<ItemStack> collectItems(String modFilter) {
        List<ItemStack> result = new ArrayList<>();
        String filter = (modFilter == null || modFilter.isBlank()) ? null : modFilter.trim().toLowerCase(Locale.ROOT);

        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null) continue;
            if (filter != null && !id.getNamespace().equalsIgnoreCase(filter)) continue;

            ItemStack plain = new ItemStack(item);
            if (plain.isEmpty()) continue;
            result.add(plain);

            CompoundTag defaultTag = plain.getTag();
            if (defaultTag != null && !defaultTag.isEmpty()) {
                ItemStack withTag = new ItemStack(item);
                withTag.setTag(defaultTag.copy());
                result.add(withTag);
            }
        }
        return result;
    }

    /** 启动导出 */
    public static boolean start(int requestedSize, String modFilter) {
        if (isRunning()) return false;
        try {
            String configured = com.zhangdaing.ae2online.Config.iconPath;
            if (configured == null || configured.isBlank()) configured = "icon-exports-x64";

            outputDir = Paths.get("." + java.io.File.separator + configured).toAbsolutePath().normalize();
            Files.createDirectories(outputDir);

            // 16 的整数倍，与物品基准尺寸对齐
            int mult = Math.max(1, Math.round(requestedSize / 16.0f));
            iconSize = 16 * mult;

            pending = collectItems(modFilter);
            writtenNames.clear();

            PROGRESS.state = State.RUNNING;
            PROGRESS.done = 0;
            PROGRESS.total = pending.size();
            PROGRESS.written = 0;
            PROGRESS.skipped = 0;
            PROGRESS.error = "";
            PROGRESS.message = "准备导出 " + pending.size() + " 个物品";

            LOGGER.info("开始导出物品图标: {} 个, 尺寸={}px, 输出={}", pending.size(), iconSize, outputDir);
            return true;
        } catch (Exception e) {
            PROGRESS.state = State.ERROR;
            PROGRESS.error = String.valueOf(e.getMessage());
            LOGGER.error("启动图标导出失败", e);
            return false;
        }
    }

    public static void cancel() {
        if (PROGRESS.state == State.RUNNING) {
            PROGRESS.state = State.CANCELLED;
            PROGRESS.message = "已取消（已导出 " + PROGRESS.written + " 个）";
            LOGGER.info("图标导出已取消，已导出 {} 个", PROGRESS.written);
        }
    }

    public static void reset() {
        if (PROGRESS.state != State.RUNNING) {
            PROGRESS.state = State.IDLE;
            PROGRESS.done = 0;
            PROGRESS.total = 0;
            PROGRESS.written = 0;
            PROGRESS.skipped = 0;
            PROGRESS.error = "";
            PROGRESS.message = "";
        }
    }

    /** 设置背景色（由导出界面传入） */
    public static void setBackgroundColor(int color) {
        backgroundColor = color;
    }

    /** 当前应该渲染的物品 */
    public static ItemStack currentItem() {
        if (PROGRESS.state != State.RUNNING) return null;
        if (PROGRESS.done >= pending.size()) return null;
        return pending.get(PROGRESS.done);
    }

    /**
     * 当前 GUI 缩放倍数。
     * RenderTarget 是物理像素，GuiGraphics 是逻辑坐标，相差此倍数。
     */
    private static int guiScale() {
        try {
            var mc = Minecraft.getInstance();
            if (mc != null && mc.getWindow() != null) {
                return Math.max(1, (int) Math.round(mc.getWindow().getGuiScale()));
            }
        } catch (Exception ignored) {
        }
        return 1;
    }

    /**
     * 在 render() 内、画完物品后立刻调用：截图 → 从左上角裁剪 → 扣透明 → 保存。
     *
     * 照 icon-exporter 的做法：物品画在 (0,0)，截图也从 (0,0) 裁。
     * guiScale>1 时按倍数换算，并缩回目标尺寸。
     */
    public static void captureAndSaveNow() {
        if (PROGRESS.state != State.RUNNING) return;
        if (PROGRESS.done >= pending.size()) {
            finish();
            return;
        }

        ItemStack stack = pending.get(PROGRESS.done);
        try {
            NativeImage full = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget());
            if (full != null) {
                try {
                    int gs = guiScale();
                    int cropSize = iconSize * gs;
                    // 物品画在 GUI (0,0) → 物理 (0,0)
                    // Screenshot 内部 flipY() 后，图像原点仍为左上角，
                    // 所以从 (0,0) 裁即可，无需处理 Y 翻转
                    int px = 0;
                    int py = 0;

                    NativeImage sub = crop(full, px, py, cropSize, cropSize);
                    if (sub != null) {
                        try {
                            if (gs != 1 && sub.getWidth() != iconSize) {
                                NativeImage scaled = downscale(sub, iconSize);
                                if (scaled != sub) {
                                    // downscale 内部已关闭 sub，这里换引用
                                    sub = scaled;
                                }
                            }
                            makeBackgroundTransparent(sub, backgroundColor);
                            String name = fileNameFor(stack);
                            // 同一物品在一次导出中可能重复出现（例如带不同 NBT 生成同名文件），
                            // 用集合去重；但**磁盘上已存在的文件一律覆盖**，
                            // 这样重新导出可以修复历史版本留下的坏图，而不是被跳过。
                            if (!writtenNames.add(name)) {
                                PROGRESS.skipped++;
                            } else {
                                Path out = outputDir.resolve(name);
                                sub.writeToFile(out);
                                PROGRESS.written++;
                            }
                        } finally {
                            sub.close();
                        }
                    }
                } finally {
                    full.close();
                }
            }
        } catch (Exception e) {
            PROGRESS.skipped++;
            LOGGER.warn("导出 {} 失败: {}", stack.getItem(), e.toString());
        }

        PROGRESS.done++;
        if ((PROGRESS.done & 31) == 0) {
            PROGRESS.message = String.format("已导出 %d / %d", PROGRESS.done, PROGRESS.total);
        }
        if (PROGRESS.done >= pending.size()) {
            finish();
        }
    }

    /** 兼容旧调用名 */
    @Deprecated
    public static void captureAndSave() {
        captureAndSaveNow();
    }

    private static void finish() {
        PROGRESS.state = State.DONE;
        PROGRESS.message = String.format("完成：导出 %d 个，跳过 %d 个",
                PROGRESS.written, PROGRESS.skipped);
        LOGGER.info("图标导出完成: 写入 {} 个, 跳过 {} 个, 输出 {}",
                PROGRESS.written, PROGRESS.skipped, outputDir);
        pending = new ArrayList<>();
    }

    private static NativeImage crop(NativeImage src, int x, int y, int w, int h) {
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x + w > src.getWidth()) w = src.getWidth() - x;
        if (y + h > src.getHeight()) h = src.getHeight() - y;
        if (w <= 0 || h <= 0) return null;

        NativeImage out = new NativeImage(w, h, false);
        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                out.setPixelRGBA(dx, dy, src.getPixelRGBA(x + dx, y + dy));
            }
        }
        return out;
    }

    /** 缩小到目标边长（最近邻）。会关闭 src。 */
    private static NativeImage downscale(NativeImage src, int targetSize) {
        int sw = src.getWidth();
        int sh = src.getHeight();
        if (sw <= 0 || sh <= 0) return src;
        if (sw == targetSize && sh == targetSize) return src;

        NativeImage out = new NativeImage(targetSize, targetSize, false);
        for (int y = 0; y < targetSize; y++) {
            int sy = Math.min(sh - 1, y * sh / targetSize);
            for (int x = 0; x < targetSize; x++) {
                int sx = Math.min(sw - 1, x * sw / targetSize);
                out.setPixelRGBA(x, y, src.getPixelRGBA(sx, sy));
            }
        }
        try {
            src.close();
        } catch (Exception ignored) {
        }
        return out;
    }

    private static void makeBackgroundTransparent(NativeImage img, int bgColor) {
        int bgRgb = bgColor & 0x00FFFFFF;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int px = img.getPixelRGBA(x, y);
                if ((px & 0x00FFFFFF) == bgRgb) {
                    img.setPixelRGBA(x, y, 0);
                }
            }
        }
    }

    // ---------------- 文件名（与 icon-exporter 一致） ----------------

    static String escapeKey(String s) {
        return s.replace(":", "__").replace("\"", "'").replace("/", "___");
    }

    public static String fileNameFor(ItemStack stack) {
        StringBuilder sb = new StringBuilder(64);
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        sb.append(escapeKey(String.valueOf(id)));

        CompoundTag tag = stack.getTag();
        if (tag != null && !tag.isEmpty()) {
            sb.append("__").append(escapeKey(tag.toString()));
        }
        sb.append(".png");
        return sanitizeFileName(sb.toString());
    }

    private static String sanitizeFileName(String name) {
        String out = name.replace('\\', '_')
                .replace('*', '_')
                .replace('?', '_')
                .replace('<', '_')
                .replace('>', '_')
                .replace('|', '_');
        if (out.length() > 200) {
            String hash = Integer.toHexString(out.hashCode());
            out = out.substring(0, 180) + "_" + hash + ".png";
        }
        return out;
    }

    public static Path outputDir() {
        return outputDir;
    }

    public static List<ItemStack> pendingItemsSnapshot() {
        return new ArrayList<>(pending);
    }
}
