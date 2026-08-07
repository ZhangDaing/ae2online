package com.zhangdaing.ae2online.block;

import com.zhangdaing.ae2online.blockentity.OnlineTerminalBlockEntity;

import appeng.api.networking.IGrid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;

public class OnlineTerminalBlock extends BaseEntityBlock {

    public OnlineTerminalBlock(Properties properties) {
    super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OnlineTerminalBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public  InteractionResult use(
            BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        // 只在服务端处理（客户端处理会重复/出错）
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof OnlineTerminalBlockEntity terminal) {
                IGrid grid = terminal.getGrid();   // 调用方块实体的 getGrid()
                if (!terminal.isOnline()) {
                    player.sendSystemMessage(Component.literal(
                            "未连接到 AE 网络"));
                } 
                else {
                    // getStorageService() 拿网络存储，getCachedInventory() 拿库存，size() 是物品类型数
                    player.sendSystemMessage(Component.literal(
                             "已连接网络！物品类型数: " + grid.getStorageService().getCachedInventory().size()));
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}