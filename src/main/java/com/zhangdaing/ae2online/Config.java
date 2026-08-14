package com.zhangdaing.ae2online;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = ae2online.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    // 配置项
    public static final ForgeConfigSpec.ConfigValue<String> ICON_PATH = BUILDER
            .comment("IconExporter 导出的文件夹",
                        "可以使用其他工具导出，只需要确保格式相同",
                        "例: icon-exports-x64, icon-exports-x128")
            .define("iconPath", "icon-exports-x64");
    static final ForgeConfigSpec SPEC = BUILDER.build();
    // 静态字段
    public static String iconPath;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {

        // 同步图标路径
        iconPath = ICON_PATH.get();
    }
}
