package com.zhangdaing.ae2online;

import java.util.List;

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

    public static final ForgeConfigSpec.ConfigValue<String> BIND_ADDRESS = BUILDER
            .comment("Web 服务监听地址",
                        "127.0.0.1 = 仅本机可访问（默认，最安全）",
                        "0.0.0.0 = 监听所有网卡，局域网内其他设备可访问",
                        "也可填本机某个网卡的具体 IP，只在该网卡上监听",
                        "注意：局域网访问走明文 HTTP，请自行评估网络环境")
            .define("bindAddress", "127.0.0.1");

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ALLOWED_ORIGINS = BUILDER
            .comment("额外允许的浏览器跨域来源（CORS）",
                        "默认只允许 localhost / 127.0.0.1",
                        "需要从别的地址用浏览器打开页面时，把来源加进来",
                        "例: [\"http://192.168.1.10:8080\"]",
                        "Godot 桌面客户端不发送 Origin 头，不受此项影响")
            .defineListAllowEmpty("allowedOrigins", List.<String>of(), o -> o instanceof String);

    static final ForgeConfigSpec SPEC = BUILDER.build();
    // 静态字段
    public static String iconPath;
    public static String bindAddress;
    public static List<String> allowedOrigins;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {

        // 同步图标路径
        iconPath = ICON_PATH.get();
        // 同步监听地址与跨域白名单
        bindAddress = BIND_ADDRESS.get();
        allowedOrigins = List.copyOf(ALLOWED_ORIGINS.get());
    }
}
