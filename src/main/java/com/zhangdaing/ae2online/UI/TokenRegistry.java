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

    /**
     * 清除某个终端名下的全部 token。
     * 终端离线、方块被移除或区块卸载时调用，避免 token 永久累积（内存泄漏）
     * 以及旧 token 继续指向已失效的方块实体。
     *
     * @return 被清除的 token 数量
     */
    public static int removeTokensFor(OnlineTerminalBlockEntity terminal) {
        if (terminal == null) return 0;
        int removed = 0;
        for (var it = tokens.entrySet().iterator(); it.hasNext(); ) {
            if (it.next().getValue() == terminal) {
                it.remove();
                removed++;
            }
        }
        return removed;
    }

    // 当前存活 token 数（诊断用）
    public static int size() {
        return tokens.size();
    }
}
