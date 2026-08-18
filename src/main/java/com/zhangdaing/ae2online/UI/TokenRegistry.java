package com.zhangdaing.ae2online.UI;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.zhangdaing.ae2online.blockentity.OnlineTerminalBlockEntity;

public class TokenRegistry {
    private static final Map<String, OnlineTerminalBlockEntity> tokens = new ConcurrentHashMap<>();

    // 生成token
    public static String createToken(OnlineTerminalBlockEntity terminal) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, terminal);
        return token;
    }

    // 根据token获取终端
    public static OnlineTerminalBlockEntity getTerminal(String token) {
        return token == null ? null : tokens.get(token);
    }

    // 清除终端token
    public static void removeToken(String token) {
        tokens.remove(token);
    }
}
