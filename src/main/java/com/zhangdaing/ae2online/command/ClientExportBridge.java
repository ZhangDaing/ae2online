package com.zhangdaing.ae2online.command;

import net.minecraft.client.Minecraft;

import com.zhangdaing.ae2online.client.IconExportScreen;
import com.zhangdaing.ae2online.client.ItemIconExporter;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 客户端侧桥接：把命令/GUI 的调用转成客户端操作。
 *
 * 这个类只会在客户端被加载（通过 DistExecutor 调用），
 * 保证专用服务器不会因为引用客户端类而崩溃。
 */
@OnlyIn(Dist.CLIENT)
public final class ClientExportBridge {

    private ClientExportBridge() {}

    /**
     * 启动导出并打开导出界面。
     *
     * @return 是否成功启动
     */
    public static boolean startExport(int scale, String modFilter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return false;

        if (ItemIconExporter.isRunning()) {
            return false;
        }

        if (!ItemIconExporter.start(scale, modFilter)) {
            return false;
        }

        // 切到导出界面（必须在主线程）
        mc.execute(() -> {
            var items = ItemIconExporter.pendingItemsSnapshot();
            mc.setScreen(new IconExportScreen(items));
        });
        return true;
    }
}
