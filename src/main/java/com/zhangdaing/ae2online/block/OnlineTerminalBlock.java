package com.zhangdaing.ae2online.block;

import com.zhangdaing.ae2online.UI.Networking;
import com.zhangdaing.ae2online.blockentity.OnlineTerminalBlockEntity;
import com.zhangdaing.ae2online.UI.OpenTerminalScreenPacket;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

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
            OnlineTerminalBlockEntity terminal = (OnlineTerminalBlockEntity) level.getBlockEntity(pos);
            Networking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player),
                new OpenTerminalScreenPacket(pos, terminal.getUsername(), terminal.getPassword(), terminal.getUrl()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}