package com.zhangdaing.ae2online.UI;

import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;



public class Networking {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation("ae2online", "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    public static void register() {
        // 客户端到服务端
        CHANNEL.registerMessage(0, UpdateTerminalPacket.class,
            UpdateTerminalPacket::encode,
            UpdateTerminalPacket::decode,
            UpdateTerminalPacket::handle
        );
        // 服务端到客户端
        CHANNEL.registerMessage(1, OpenTerminalScreenPacket.class,
            OpenTerminalScreenPacket::encode,
            OpenTerminalScreenPacket::decode,
            OpenTerminalScreenPacket::handle
        );
    }
}
