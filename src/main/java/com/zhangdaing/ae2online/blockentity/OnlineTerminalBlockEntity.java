package com.zhangdaing.ae2online.blockentity;

import com.zhangdaing.ae2online.ae2online;
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
            // 注册在线终端实体
            TerminalRegistry.registerTerminal(this);
        } else {
            // 注销在线终端实体
            TerminalRegistry.unregisterTerminal(this);
        }
    }

    @Override
    public void onMainNodeStateChanged(appeng.api.networking.IGridNodeListener.State reason) {
        super.onMainNodeStateChanged(reason);
        // 上线时注册到 Web 终端列表，离线时注销
        RegisterOnStateChange(isOnline());
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!level.isClientSide) {
            int port = parsePortFromUrl();
            if (port > 0) {
                WebServerManager.startServer(port);
            }
        }
    }

    @Override
    public void setRemoved() {
        // 从注册表移除
        TerminalRegistry.unregisterTerminal(this);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        TerminalRegistry.unregisterTerminal(this);
        super.onChunkUnloaded();
    }

    private int parsePortFromUrl() {
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
        this.username = username;
        this.password = password;
        this.url = url;
        setChanged();
        if (!level.isClientSide) {
            int port = parsePortFromUrl();
            if (port > 0) {
                WebServerManager.startServer(port);
            }
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