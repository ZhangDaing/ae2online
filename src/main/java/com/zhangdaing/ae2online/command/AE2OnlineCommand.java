package com.zhangdaing.ae2online.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * /ae2online 命令树。
 *
 * 结构：
 *   /ae2online export [scale] [mod]
 *
 * 注意：导出必须由客户端执行（依赖截图 API）。在专用服务器上执行会给出提示而不是崩溃，
 * 客户端侧逻辑通过 DistExecutor 隔离，专用服务器永远不会加载那些类。
 */
public class AE2OnlineCommand {

    /** 模组命名空间补全 */
    private static final SuggestionProvider<CommandSourceStack> MOD_SUGGESTIONS = (ctx, builder) -> {
        Set<String> namespaces = new LinkedHashSet<>();
        for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
            namespaces.add(id.getNamespace());
        }
        return SharedSuggestionProvider.suggest(namespaces, builder);
    };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("ae2online")
                .then(Commands.literal("export")
                        .executes(ctx -> runExport(ctx, 64, ""))
                        .then(Commands.argument("scale", IntegerArgumentType.integer(16, 512))
                                .executes(ctx -> runExport(ctx,
                                        IntegerArgumentType.getInteger(ctx, "scale"), ""))
                                .then(Commands.argument("mod", StringArgumentType.word())
                                        .suggests(MOD_SUGGESTIONS)
                                        .executes(ctx -> runExport(ctx,
                                                IntegerArgumentType.getInteger(ctx, "scale"),
                                                StringArgumentType.getString(ctx, "mod")))))));
    }

    private static int runExport(CommandContext<CommandSourceStack> ctx, int scale, String modFilter) {
        CommandSourceStack source = ctx.getSource();

        // 客户端侧执行；专用服务器上 unsafeCallWhenOn 返回 null
        Boolean started = DistExecutor.unsafeCallWhenOn(Dist.CLIENT,
                () -> () -> ClientExportBridge.startExport(scale, modFilter));

        if (started == null) {
            // 专用服务器：不尝试加载任何客户端类
            source.sendFailure(Component.literal(
                    "[AE2 Online] 图标导出只能在客户端执行（需要截图 API）。"));
            return 0;
        }

        if (started) {
            source.sendSuccess(() -> Component.literal(
                    "[AE2 Online] 开始导出图标（scale=" + scale
                            + (modFilter == null || modFilter.isBlank() ? "，全部模组" : "，模组 " + modFilter)
                            + "）").withStyle(ChatFormatting.GREEN), false);
            return 1;
        } else {
            source.sendFailure(Component.literal(
                    "[AE2 Online] 导出未能启动（可能已有任务在运行，详见日志）。"));
            return 0;
        }
    }
}
