package com.zhangdaing.ae2online.blockentity;

import com.zhangdaing.ae2online.ae2online;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.blockentity.grid.AENetworkBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class OnlineTerminalBlockEntity extends AENetworkBlockEntity {

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

}