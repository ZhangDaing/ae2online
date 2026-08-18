package com.zhangdaing.ae2online;

import com.zhangdaing.ae2online.blockentity.OnlineTerminalBlockEntity;
import com.zhangdaing.ae2online.web.WebServer;
import com.zhangdaing.ae2online.block.OnlineTerminalBlock;
import com.zhangdaing.ae2online.UI.Networking;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(ae2online.MODID)
public class ae2online
{
    public static final String MODID = "ae2online";

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);



    //注册方块
    public static final RegistryObject<Block> ONLINE_TERMINAL = BLOCKS.register("online_terminal", 
            () -> new OnlineTerminalBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));

    //注册方块实体
    public static final RegistryObject<BlockEntityType<OnlineTerminalBlockEntity>> ONLINE_TERMINAL_BLOCK_ENTITY = 
            BLOCK_ENTITY_TYPES.register("online_terminal", 
                    () -> BlockEntityType.Builder.of(OnlineTerminalBlockEntity::new,ONLINE_TERMINAL.get()).build(null));

    //注册物品
    public static final RegistryObject<Item> ONLINE_TERMINAL_ITEM = ITEMS.register("online_terminal", 
            () -> new BlockItem(ONLINE_TERMINAL.get(), new Item.Properties()));

    
    public ae2online(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);


        MinecraftForge.EVENT_BUS.register(this);

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);


        // 服务器启动时开 Web 服务器，停止时关
        MinecraftForge.EVENT_BUS.addListener((ServerStartingEvent e) -> WebServer.startsever());
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> WebServer.stop());

        // 注册网络消息
        Networking.register();
    }



}