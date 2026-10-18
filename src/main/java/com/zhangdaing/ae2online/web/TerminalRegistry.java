package com.zhangdaing.ae2online.web;

import com.zhangdaing.ae2online.blockentity.OnlineTerminalBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TerminalRegistry {
    // 用于存储所有注册在线的终端实体
    // Web 服务线程池（8 线程）会并发读取，游戏线程会注册/注销，必须用并发容器
    private static final Map<BlockEntity, OnlineTerminalBlockEntity> terminals = new ConcurrentHashMap<>();

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
    // 按用户名获取终端
    public static OnlineTerminalBlockEntity findByUsername(String username) {
    return terminals.values().stream()
        .filter(t -> username != null && username.equals(t.getUsername()))
        .findFirst()
        .orElse(null);
    }
}
