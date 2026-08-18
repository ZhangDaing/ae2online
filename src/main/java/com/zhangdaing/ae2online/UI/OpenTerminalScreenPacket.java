package com.zhangdaing.ae2online.UI;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenTerminalScreenPacket {
    private final BlockPos pos;
    private final String username;
    private final String password;
    private final String url;
    
    public OpenTerminalScreenPacket(BlockPos pos, String username, String password, String url) {
        this.pos = pos;
        this.username = username;
        this.password = password;
        this.url = url;
    }

    public static void encode(OpenTerminalScreenPacket msg, net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.username);
        buf.writeUtf(msg.password);
        buf.writeUtf(msg.url);
    }

    public static OpenTerminalScreenPacket decode(net.minecraft.network.FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String username = buf.readUtf();
        String password = buf.readUtf();
        String url = buf.readUtf();
        return new OpenTerminalScreenPacket(pos, username, password, url);
    }
    
    public static void handle(OpenTerminalScreenPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 打开Screen
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                Minecraft.getInstance().setScreen(
                    new OnlineTerminalScreen(msg.pos, msg.username, msg.password, msg.url)
                ));
        });
        ctx.get().setPacketHandled(true);
    }
}
