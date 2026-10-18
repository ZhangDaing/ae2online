package com.zhangdaing.ae2online.blockentity;

import com.zhangdaing.ae2online.ae2online;
import com.zhangdaing.ae2online.UI.TokenRegistry;
import com.zhangdaing.ae2online.web.TerminalRegistry;
import com.zhangdaing.ae2online.web.WebServerManager;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.blockentity.grid.AENetworkBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.net.URI;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class OnlineTerminalBlockEntity extends AENetworkBlockEntity {
    private static final Logger LOGGER = LoggerFactory.getLogger("AE2Online Block");

    private String username = "";
    private String password = "";
    private String url = "";

    public OnlineTerminalBlockEntity(BlockPos pos, BlockState state) {
        this(ae2online.ONLINE_TERMINAL_BLOCK_ENTITY.get(), pos, state);
    }

    public OnlineTerminalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        //设置待机耗电
        getMainNode().setIdlePowerUsage(16.0);
        //需要频道
        getMainNode().setFlags(GridFlags.REQUIRE_CHANNEL);
        //ME控制器显示状态
        getMainNode().setVisualRepresentation(ae2online.ONLINE_TERMINAL_ITEM.get().getDefaultInstance());
    }

    public IGrid getGrid() {
        return getMainNode().getGrid();
    }

    public boolean isOnline() {
        return getMainNode().isOnline();
    }

    public void RegisterOnStateChange(boolean isOnline){
        if (isOnline) {
            // 注册在线终端实体，并按需启动 Web 服务
            TerminalRegistry.registerTerminal(this);
            startWebServerIfConfigured();
        } else {
            // 注销在线终端实体，清理其 token，并尝试回收 Web 服务
            TerminalRegistry.unregisterTerminal(this);
            int cleared = TokenRegistry.removeTokensFor(this);
            if (cleared > 0) {
                LOGGER.info("Terminal at {} went offline, cleared {} token(s)", worldPosition, cleared);
            }
            stopWebServerIfUnused();
        }
    }

    /**
     * 仅在终端已接入网络（有频道、已启动）时才启动 Web 服务。
     * 未接入网络时对外提供服务只会让客户端拿到「认证失败」这类误导性结果。
     */
    private void startWebServerIfConfigured() {
        if (level == null || level.isClientSide) return;
        int port = parsePortFromUrl();
        if (port <= 0) return;
        WebServerManager.startServer(port);
    }

    /**
     * 终端离线时回收 Web 服务。
     * 只有在没有其他在线终端使用同一端口时才真正停止，避免误停他人服务。
     */
    private void stopWebServerIfUnused() {
        if (level == null || level.isClientSide) return;
        int port = parsePortFromUrl();
        if (port <= 0) return;
        // 该端口是否仍被其他在线终端占用
        boolean stillUsed = TerminalRegistry.getAllTerminals().values().stream()
                .anyMatch(t -> t != this && t.parsePortFromUrl() == port);
        if (!stillUsed) {
            WebServerManager.stopServer(port);
        }
    }

    @Override
    public void onMainNodeStateChanged(appeng.api.networking.IGridNodeListener.State reason) {
        super.onMainNodeStateChanged(reason);
        // 供电/频道状态变化时同步注册表与 Web 服务
        RegisterOnStateChange(isOnline());
    }

    /**
     * 网格节点创建完成后主动同步一次状态。
     *
     * 必要性：AE2 的 ManagedGridNode.create() -> markReady() -> updateState() 只建立连接，
     * 并不触发 onStateChanged 回调；该回调仅在供电（POWER）或频道（CHANNEL）变化时才发生。
     * 若只依赖回调，服务器重启或区块重载后，一个供电稳定的在线终端将永远不会启动 Web 服务。
     */
    @Override
    public void onReady() {
        super.onReady();
        RegisterOnStateChange(isOnline());
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // 注意：区块加载早于网格节点创建，此处 isOnline() 必然为 false。
        // 因此不在这里启动 Web 服务，等服务真正上线时由 onMainNodeStateChanged 启动。
    }

    @Override
    public void setRemoved() {
        // 从注册表移除，并清理该终端名下的全部 token
        TerminalRegistry.unregisterTerminal(this);
        int cleared = TokenRegistry.removeTokensFor(this);
        if (cleared > 0) {
            LOGGER.info("Terminal at {} removed, cleared {} token(s)", worldPosition, cleared);
        }
        stopWebServerIfUnused();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        TerminalRegistry.unregisterTerminal(this);
        // 区块卸载时方块可能只是暂时离开视野，但仍要清理 token：
        // 重新加载后终端会重新注册，用户需重新登录（安全优先）
        TokenRegistry.removeTokensFor(this);
        super.onChunkUnloaded();
    }

    /** 解析 URL 中的端口；供同包/注册表判定端口占用情况使用。 */
    int parsePortFromUrl() {
        if (url == null || url.isBlank()) return -1;
        try {
            URI uri = new URI(url);
            int port = uri.getPort();
            if (port == -1) {
                String scheme = uri.getScheme();
                port = "https".equalsIgnoreCase(scheme) ? 443 : 80;
            }
            return port;
        } catch (Exception e) {
            LOGGER.warn("Failed to parse port from URL '{}': {}", url, e.getMessage());
            return -1;
        }
    }

    public void setCredentials(String username, String password, String url) {
        int oldPort = parsePortFromUrl();
        this.username = username;
        this.password = password;
        this.url = url;
        setChanged();
        if (level == null || level.isClientSide) return;
        int newPort = parsePortFromUrl();
        // 端口变了：回收旧端口的服务（若无人使用），再按新配置启动
        if (oldPort > 0 && oldPort != newPort) {
            stopWebServerIfUnused();
        }
        // 仅在终端已接入网络时才启动服务，避免离线终端对外表现为「认证失败」
        if (isOnline()) {
            startWebServerIfConfigured();
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("username", username);
        tag.putString("password", password);
        tag.putString("url", url);
    }

    @Override
    public void loadTag(CompoundTag tag) {
        super.loadTag(tag);
        username = tag.getString("username");
        password = tag.getString("password");
        url = tag.getString("url");
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getUrl() { return url; }
}