package com.zhangdaing.ae2online.web;

import com.zhangdaing.ae2online.blockentity.OnlineTerminalBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;

public class TerminalRegistry {
    // 用于存储所有注册在线的终端实体
    private static final Map<BlockEntity, OnlineTerminalBlockEntity> terminals = new HashMap<>();

    // 注册一个在线终端实体
    public static void registerTerminal(OnlineTerminalBlockEntity onlineTerminal) {
        terminals.put(onlineTerminal.getBlockEntity(), onlineTerminal);
    }
    // 注销一个在线终端实体
    public static void unregisterTerminal(OnlineTerminalBlockEntity onlineTerminal) {
        terminals.remove(onlineTerminal.getBlockEntity());
    }

    // 获取对应的终端实体
    public static OnlineTerminalBlockEntity getTerminal(BlockEntity blockEntity) {
        return terminals.get(blockEntity);
    }
    // 获取所有终端实体
    public static Map<BlockEntity, OnlineTerminalBlockEntity> getAllTerminals() {
        return terminals;
    }
}
