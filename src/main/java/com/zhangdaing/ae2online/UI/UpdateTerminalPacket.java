package com.zhangdaing.ae2online.UI;

import com.zhangdaing.ae2online.blockentity.OnlineTerminalBlockEntity;
import com.zhangdaing.ae2online.web.NetworkAddress;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;


public class UpdateTerminalPacket {
    private final BlockPos pos;
    private final String username;
    private final String password;
    private final String url;

    public UpdateTerminalPacket(BlockPos pos, String username, String password, String url) {
        this.pos = pos;
        this.username = username;
        this.password = password;
        this.url = url;
    }
    
    public static void encode(UpdateTerminalPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.username);
        buf.writeUtf(msg.password);
        buf.writeUtf(msg.url);
    }

    public static UpdateTerminalPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String username = buf.readUtf();
        String password = buf.readUtf();
        String url = buf.readUtf();
        return new UpdateTerminalPacket(pos, username, password, url);
    }

    public static void handle(UpdateTerminalPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var sender = ctx.get().getSender();
            if (sender == null) return;
            var level = sender.level();
            if (level.getBlockEntity(msg.pos) instanceof OnlineTerminalBlockEntity terminal) {
                terminal.setCredentials(msg.username, msg.password, msg.url);
                // 保存后把「客户端该填的地址」直接发到聊天栏，省得玩家自己查 IP
                sendAddressHint(sender, terminal);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * 组装并发送连接地址提示。
     * 根据 bindAddress 决定显示哪些地址：
     *   - 0.0.0.0：列出本机所有局域网 IP
     *   - 具体 IP：只显示那个
     *   - 127.0.0.1：提示当前仅本机可访问
     */
    private static void sendAddressHint(net.minecraft.server.level.ServerPlayer sender,
                                        OnlineTerminalBlockEntity terminal) {
        int port = terminal.parsePortFromUrl();
        if (port <= 0) {
            sender.sendSystemMessage(Component.literal("[AE2 Online] ")
                    .withStyle(ChatFormatting.AQUA)
                    .append(Component.literal("端口未正确配置，Web 服务不会启动。")
                            .withStyle(ChatFormatting.RED)));
            return;
        }

        String bind = com.zhangdaing.ae2online.Config.bindAddress;
        if (bind == null) bind = "";
        bind = bind.trim();

        sender.sendSystemMessage(Component.literal("[AE2 Online] ")
                .withStyle(ChatFormatting.AQUA)
                .append(Component.literal("配置已保存，端口 " + port).withStyle(ChatFormatting.GREEN)));

        if (bind.equals("127.0.0.1") || bind.equals("localhost")) {
            sender.sendSystemMessage(Component.literal("  当前仅本机可访问（bindAddress = " + bind + "）。")
                    .withStyle(ChatFormatting.GRAY));
            sender.sendSystemMessage(Component.literal("  客户端填：http://127.0.0.1:" + port)
                    .withStyle(ChatFormatting.WHITE));
            sender.sendSystemMessage(Component.literal("  如需手机/其他电脑访问，把 config 里 bindAddress 改成 0.0.0.0")
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }

        // 0.0.0.0 或具体网卡 IP
        List<String> ips;
        if (bind.equals("0.0.0.0")) {
            ips = NetworkAddress.localIpv4Addresses();
        } else {
            ips = List.of(bind);
        }

        if (ips.isEmpty()) {
            sender.sendSystemMessage(Component.literal("  未能探测到局域网地址，请手动用 ipconfig 查看本机 IP。")
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }

        sender.sendSystemMessage(Component.literal("  客户端（手机/其他电脑）可填以下地址：")
                .withStyle(ChatFormatting.GRAY));
        int shown = 0;
        for (String ip : ips) {
            if (shown >= 4) break; // 避免刷屏
            sender.sendSystemMessage(Component.literal("    http://" + ip + ":" + port)
                    .withStyle(ChatFormatting.WHITE));
            shown++;
        }
        sender.sendSystemMessage(Component.literal("  本机自测可用：http://127.0.0.1:" + port)
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
