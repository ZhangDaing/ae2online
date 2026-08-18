package com.zhangdaing.ae2online.UI;

import com.zhangdaing.ae2online.blockentity.OnlineTerminalBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import net.minecraftforge.network.NetworkEvent;

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
            // 保存设定到方块
            var level = ctx.get().getSender().level();
            if(level.getBlockEntity(msg.pos) instanceof OnlineTerminalBlockEntity terminal){
                terminal.setCredentials(msg.username, msg.password, msg.url);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
