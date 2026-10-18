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
                        "0.0.0.0 = 监听所有网卡，局域网内其他设备（手机等）可访问（默认）",
                        "127.0.0.1 = 仅本机可访问。若只在本机使用，建议改成这个更安全",
                        "也可填本机某个网卡的具体 IP，只在该网卡上监听")
            .define("bindAddress", "0.0.0.0");

    public static final ForgeConfigSpec.BooleanValue ENABLE_HTTPS = BUILDER
            .comment("是否启用 HTTPS（自签名证书，默认开启）",
                        "浏览器只把 https:// 和 localhost 视为「安全上下文」，",
                        "而 Godot Web 版（浏览器里运行的界面）必须在安全上下文中才能启动。",
                        "开启后首次启动会自动生成自签名证书到 config/ae2online-cert.p12。",
                        "浏览器首次访问会提示「连接不是私密连接」，点「高级 → 继续访问」即可。",
                        "关闭则使用 HTTP：桌面客户端/Android 客户端仍可用，但浏览器中的 Web 版无法运行。")
            .define("enableHttps", true);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ALLOWED_ORIGINS = BUILDER
            .comment("额外允许的浏览器跨域来源（CORS）",
                        "默认只允许 localhost / 127.0.0.1",
                        "需要从别的地址用浏览器打开页面时，把来源加进来",
                        "例: [\"https://192.168.1.10:8080\"]",
                        "Godot 桌面客户端不发送 Origin 头，不受此项影响")
            .defineListAllowEmpty("allowedOrigins", List.<String>of(), o -> o instanceof String);

    static final ForgeConfigSpec SPEC = BUILDER.build();
    // 静态字段
    public static String iconPath;
    public static String bindAddress;
    public static boolean enableHttps;
    public static List<String> allowedOrigins;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {

        // 同步图标路径
        iconPath = ICON_PATH.get();
        // 同步监听地址与跨域白名单
        bindAddress = BIND_ADDRESS.get();
        enableHttps = ENABLE_HTTPS.get();
        allowedOrigins = List.copyOf(ALLOWED_ORIGINS.get());
    }
}
