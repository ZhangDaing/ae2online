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
 * 实现思路参考 icon-exporter：把物品渲染到屏幕上 → 截图 → 裁剪 → 扣透明 → 保存。
 *
 * 关键时序（踩过的坑）：
 *   Screenshot.takeScreenshot 取的是「上一帧」已完成的 framebuffer。
 *   所以必须是：帧 N 渲染物品 → 帧 N+1 截图。
 *   如果在 render() 同一帧里既画又截，截到的是上一帧的内容（错位一格）。
 *   本类用 capturePreviousFrame 标志实现这个两帧流水。
 *
 * 文件名格式与 icon-exporter 严格一致，客户端的图标查找逻辑无需改动：
 *   无 NBT：modid__itemid.png
 *   有 NBT：modid__itemid__{nbt}.png
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
    private static Path outputDir;
    /** 单个图标的边长（屏幕像素）。必须与渲染区实际占用像素一致。 */
    private static int iconSize = 64;
    /** 上一帧已经把「当前索引」的物品画到屏幕上了，本帧可以截图 */
    private static boolean frameReady = false;
    /** 导出界面传进来的渲染原点 */
    private static int renderX = 8;
    private static int renderY = 8;
    private static int backgroundColor = 0xFF1A1A1A;

    public static Progress progress() {
        return PROGRESS;
    }

    public static boolean isRunning() {
        return PROGRESS.state == State.RUNNING;
    }

    public static int iconSize() {
        return iconSize;
    }

    /** 计算 scale 参数对应的渲染倍数与最终像素尺寸 */
    public static int pixelScaleFor(int requestedSize) {
        // 图标基准是 16x16，放大倍数必须是整数，最终尺寸 = 16 * 倍数
        int mult = Math.max(1, Math.round(requestedSize / 16.0f));
        return mult;
    }

    /**
     * 收集要导出的物品。
     *
     * @param modFilter 只导出该命名空间；空表示全部
     */
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

    /**
     * 启动导出（客户端主线程）。
     *
     * @param requestedSize 期望的图标边长（像素），会向下取整到 16 的倍数
     * @param modFilter 模组命名空间过滤
     */
    public static boolean start(int requestedSize, String modFilter) {
        if (isRunning()) return false;
        try {
            String configured = com.zhangdaing.ae2online.Config.iconPath;
            if (configured == null || configured.isBlank()) configured = "icon-exports-x64";

            outputDir = Paths.get("." + java.io.File.separator + configured).toAbsolutePath().normalize();
            Files.createDirectories(outputDir);

            int mult = pixelScaleFor(requestedSize);
            iconSize = 16 * mult;

            pending = collectItems(modFilter);
            frameReady = false;

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
            frameReady = false;
        }
    }

    /** 导出界面每帧调用：告知渲染原点与背景色 */
    public static void setRenderContext(int x, int y, int bgColor) {
        renderX = x;
        renderY = y;
        backgroundColor = bgColor;
    }

    /** 当前应该渲染的物品；没有则返回 null */
    public static ItemStack currentItem() {
        if (PROGRESS.state != State.RUNNING) return null;
        if (PROGRESS.done >= pending.size()) return null;
        return pending.get(PROGRESS.done);
    }

    /**
     * 由导出界面在渲染完当前物品后调用（帧 N），
     * 标记下一帧可以截图。
     */
    public static void markFrameRendered() {
        if (PROGRESS.state == State.RUNNING) {
            frameReady = true;
        }
    }

    /**
     * 由导出界面在下一帧开头调用（帧 N+1）：截上图并写盘。
     * 这样截到的一定是上一帧已经渲染好的物品。
     */
    public static void captureAndSave() {
        if (PROGRESS.state != State.RUNNING || !frameReady) return;
        frameReady = false;

        if (PROGRESS.done >= pending.size()) {
            finish();
            return;
        }

        ItemStack stack = pending.get(PROGRESS.done);
        try {
            NativeImage full = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget());
            if (full != null) {
                try {
                    NativeImage sub = crop(full, renderX, renderY, iconSize, iconSize);
                    if (sub != null) {
                        try {
                            makeBackgroundTransparent(sub, backgroundColor);
                            String name = fileNameFor(stack);
                            Path out = outputDir.resolve(name);
                            if (Files.exists(out)) {
                                PROGRESS.skipped++;
                            } else {
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

    private static void finish() {
        PROGRESS.state = State.DONE;
        PROGRESS.message = String.format("完成：导出 %d 个，跳过 %d 个",
                PROGRESS.written, PROGRESS.skipped);
        LOGGER.info("图标导出完成: 写入 {} 个, 跳过 {} 个, 输出 {}", PROGRESS.written, PROGRESS.skipped, outputDir);
        pending = new ArrayList<>();
        frameReady = false;
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

    private static void makeBackgroundTransparent(NativeImage img, int backgroundColor) {
        int bgRgb = backgroundColor & 0x00FFFFFF;
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

    /** Windows 非法文件名字符兜底（icon-exporter 没做，NBT 含这些字符时会写失败） */
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
