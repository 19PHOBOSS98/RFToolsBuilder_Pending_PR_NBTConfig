package mcjty.rftoolsbuilder.modules.scanner;

import mcjty.lib.blocks.BaseBlock;
import mcjty.lib.modules.IModule;
import mcjty.lib.setup.DeferredBlock;
import mcjty.lib.setup.DeferredItem;
import mcjty.rftoolsbuilder.modules.scanner.blocks.ProjectorTileEntity;
import mcjty.rftoolsbuilder.modules.scanner.client.ProjectorRenderer;
import mcjty.rftoolsbuilder.setup.Config;
import mcjty.rftoolsbuilder.setup.Registration;
import mcjty.rftoolsbuilder.shapes.ShapeDataManagerClient;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.function.Supplier;

import static mcjty.rftoolsbuilder.RFToolsBuilder.tab;
import static mcjty.rftoolsbuilder.setup.Registration.*;

public class ScannerModule implements IModule {

    public static final DeferredBlock<BaseBlock> PROJECTOR_BLOCK = BLOCKS.register("projector", ProjectorTileEntity::createBlock);
    public static final DeferredItem<Item> PROJECTOR_ITEM = ITEMS.register("projector", tab(() -> new BlockItem(PROJECTOR_BLOCK.get(), Registration.createStandardProperties())));
    public static final Supplier<BlockEntityType<ProjectorTileEntity>> TYPE_PROJECTOR = TILES.register("projector", () -> BlockEntityType.Builder.of(ProjectorTileEntity::new, PROJECTOR_BLOCK.get()).build(null));

    @Override
    public void init(FMLCommonSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new ShapeHandler());
    }

    @Override
    public void initClient(FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.addListener(ShapeDataManagerClient::cleanupOldRenderers);
        ProjectorRenderer.register();
    }

    @Override
    public void initConfig(IEventBus bus) {
        ScannerConfiguration.init(Config.SERVER_BUILDER, Config.CLIENT_BUILDER);
    }
}
