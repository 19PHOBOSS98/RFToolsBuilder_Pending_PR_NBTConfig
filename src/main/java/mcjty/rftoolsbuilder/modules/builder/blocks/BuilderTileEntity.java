package mcjty.rftoolsbuilder.modules.builder.blocks;

import mcjty.lib.api.container.CapabilityContainerProvider;
import mcjty.lib.api.container.DefaultContainerProvider;
import mcjty.lib.api.infusable.CapabilityInfusable;
import mcjty.lib.api.infusable.DefaultInfusable;
import mcjty.lib.api.infusable.IInfusable;
import mcjty.lib.api.module.CapabilityModuleSupport;
import mcjty.lib.api.module.DefaultModuleSupport;
import mcjty.lib.api.module.IModuleSupport;
import mcjty.lib.bindings.DefaultAction;
import mcjty.lib.bindings.DefaultValue;
import mcjty.lib.bindings.IAction;
import mcjty.lib.bindings.IValue;
import mcjty.lib.blocks.BaseBlock;
import mcjty.lib.blocks.RotationType;
import mcjty.lib.builder.BlockBuilder;
import mcjty.lib.container.AutomationFilterItemHander;
import mcjty.lib.container.ContainerFactory;
import mcjty.lib.container.GenericContainer;
import mcjty.lib.container.NoDirectionItemHander;
import mcjty.lib.gui.widgets.ChoiceLabel;
import mcjty.lib.tileentity.GenericEnergyStorage;
import mcjty.lib.tileentity.GenericTileEntity;
import mcjty.lib.typed.Key;
import mcjty.lib.typed.Type;
import mcjty.lib.typed.TypedMap;
import mcjty.lib.varia.*;
import mcjty.rftoolsbase.api.client.IHudSupport;
import mcjty.rftoolsbase.modules.filter.items.FilterModuleItem;
import mcjty.rftoolsbase.modules.hud.network.PacketGetHudLog;
import mcjty.rftoolsbase.tools.ManualHelper;
import mcjty.rftoolsbuilder.RFToolsBuilder;
import mcjty.rftoolsbuilder.compat.RFToolsBuilderTOPDriver;
import mcjty.rftoolsbuilder.modules.builder.BlockInformation;
import mcjty.rftoolsbuilder.modules.builder.BuilderConfiguration;
import mcjty.rftoolsbuilder.modules.builder.BuilderModule;
import mcjty.rftoolsbuilder.modules.builder.SpaceChamberRepository;
import mcjty.rftoolsbuilder.modules.builder.items.ShapeCardItem;
import mcjty.rftoolsbuilder.modules.builder.items.ShapeCardType;
import mcjty.rftoolsbuilder.modules.builder.items.SpaceChamberCardItem;
import mcjty.rftoolsbuilder.setup.ClientCommandHandler;
import mcjty.rftoolsbuilder.setup.RFToolsBuilderMessages;
import mcjty.rftoolsbuilder.shapes.Shape;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameters;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

import static mcjty.lib.builder.TooltipBuilder.*;
import static mcjty.lib.container.ContainerFactory.CONTAINER_CONTAINER;
import static mcjty.lib.container.SlotDefinition.specific;

public class BuilderTileEntity extends GenericTileEntity implements ITickableTileEntity, IHudSupport {

    public static final String CMD_SETMODE = "builder.setMode";
    public static final String CMD_SETROTATE = "builder.setRotate";

    public static final String CMD_SETANCHOR = "builder.setAnchor";
    public static final Key<Integer> PARAM_ANCHOR_INDEX = new Key<>("anchorIndex", Type.INTEGER);

    public static final int SLOT_TAB = 0;
    public static final int SLOT_FILTER = 1;

    public static final Lazy<ContainerFactory> CONTAINER_FACTORY = Lazy.of(() -> new ContainerFactory(2)
            .slot(specific(s -> (s.getItem() instanceof ShapeCardItem) || (s.getItem() instanceof SpaceChamberCardItem)).in().out(),
                    CONTAINER_CONTAINER, SLOT_TAB, 100, 10)
            .slot(specific(s -> s.getItem() instanceof FilterModuleItem).in().out(),
                    CONTAINER_CONTAINER, SLOT_FILTER, 84, 46)
            .playerSlots(10, 70));

    public static final int MODE_COPY = 0;
    public static final int MODE_MOVE = 1;
    public static final int MODE_SWAP = 2;
    public static final int MODE_BACK = 3;
    public static final int MODE_COLLECT = 4;

    public static final String[] MODES = new String[]{"Copy", "Move", "Swap", "Back", "Collect"};

    public static final String ROTATE_0 = "0";
    public static final String ROTATE_90 = "90";
    public static final String ROTATE_180 = "180";
    public static final String ROTATE_270 = "270";

    public static final int ANCHOR_SW = 0;
    public static final int ANCHOR_SE = 1;
    public static final int ANCHOR_NW = 2;
    public static final int ANCHOR_NE = 3;

    private String lastError = null;
    private int mode = MODE_COPY;
    private int rotate = 0;
    private int anchor = ANCHOR_SW;
    private boolean silent = false;
    private boolean supportMode = false;
    private boolean entityMode = false;
    private boolean loopMode = false;
    private boolean waitMode = true;
    private boolean hilightMode = false;

    // For usage in the gui
    private static int currentLevel = 0;

    // Client-side
    private int scanLocCnt = 0;
    private static Map<BlockPos, Pair<Long, BlockPos>> scanLocClient = new HashMap<>();

    private int collectCounter = BuilderConfiguration.collectTimer.get();
    private int collectXP = 0;

    private boolean boxValid = false;
    private BlockPos minBox = null;
    private BlockPos maxBox = null;
    private BlockPos scan = null;
    private int projDx;
    private int projDy;
    private int projDz;

    private long lastHudTime = 0;
    private List<String> clientHudLog = new ArrayList<>();

    private ShapeCardType cardType = ShapeCardType.CARD_UNKNOWN;

    private static ItemStack TOOL_NORMAL;
    private static ItemStack TOOL_SILK;
    private static ItemStack TOOL_FORTUNE;

    private final Cached<Predicate<ItemStack>> filterCache = Cached.of(this::createFilterCache);

    // The currently forced chunk.
    private ChunkPos forcedChunk = null;

    // Cached set of blocks that we need to build in shaped mode
    private Map<BlockPos, BlockState> cachedBlocks = null;
    private ChunkPos cachedChunk = null;       // For which chunk are the cachedBlocks valid

    // Cached set of blocks that we want to void with the quarry.
    private final Cached<Set<Block>> cachedVoidableBlocks = Cached.of(this::getCachedVoidableBlocks);

    // Drops from a block that we broke but couldn't fit in an inventory
    private LazyList<ItemStack> overflowItems = new LazyList<>();

    private final FakePlayerGetter harvester = new FakePlayerGetter(this, "rftools_builder");

    private final NoDirectionItemHander items = createItemHandler();
    private final LazyOptional<AutomationFilterItemHander> itemHandler = LazyOptional.of(() -> new AutomationFilterItemHander(items));

    private final GenericEnergyStorage energyStorage = new GenericEnergyStorage(
            this, true, BuilderConfiguration.BUILDER_MAXENERGY.get(), BuilderConfiguration.BUILDER_RECEIVEPERTICK.get());
    private final LazyOptional<GenericEnergyStorage> energyHandler = LazyOptional.of(() -> energyStorage);
    private final LazyOptional<INamedContainerProvider> screenHandler = LazyOptional.of(() -> new DefaultContainerProvider<GenericContainer>("Builder")
            .containerSupplier((windowId, player) -> new GenericContainer(BuilderModule.CONTAINER_BUILDER.get(), windowId, CONTAINER_FACTORY.get(), getBlockPos(), BuilderTileEntity.this))
            .itemHandler(() -> items)
            .energyHandler(() -> energyStorage)
            .shortListener(Tools.holder(() -> scan == null ? -1 : scan.getY(), v -> currentLevel = v)));
    private final LazyOptional<IInfusable> infusableHandler = LazyOptional.of(() -> new DefaultInfusable(BuilderTileEntity.this));
    private final LazyOptional<IModuleSupport> moduleSupportHandler = LazyOptional.of(() -> new DefaultModuleSupport(SLOT_TAB) {
        @Override
        public boolean isModule(ItemStack itemStack) {
            return (itemStack.getItem() instanceof ShapeCardItem || itemStack.getItem() == BuilderModule.SPACE_CHAMBER_CARD.get());
        }
    });

    public BuilderTileEntity() {
        super(BuilderModule.TYPE_BUILDER.get());
        setRSMode(RedstoneMode.REDSTONE_ONREQUIRED);
    }


    public static final Key<Boolean> VALUE_WAIT = new Key<>("wait", Type.BOOLEAN);
    public static final Key<Boolean> VALUE_LOOP = new Key<>("loop", Type.BOOLEAN);
    public static final Key<Boolean> VALUE_HILIGHT = new Key<>("hilight", Type.BOOLEAN);
    public static final Key<Boolean> VALUE_SUPPORT = new Key<>("support", Type.BOOLEAN);
    public static final Key<Boolean> VALUE_SILENT = new Key<>("silent", Type.BOOLEAN);
    public static final Key<Boolean> VALUE_ENTITIES = new Key<>("entities", Type.BOOLEAN);

    @Override
    public IValue<?>[] getValues() {
        return new IValue[]{
                new DefaultValue<>(VALUE_RSMODE, this::getRSModeInt, this::setRSModeInt),
                new DefaultValue<>(VALUE_WAIT, this::isWaitMode, this::setWaitMode),
                new DefaultValue<>(VALUE_LOOP, this::hasLoopMode, this::setLoopMode),
                new DefaultValue<>(VALUE_HILIGHT, this::isHilightMode, this::setHilightMode),
                new DefaultValue<>(VALUE_SUPPORT, this::hasSupportMode, this::setSupportMode),
                new DefaultValue<>(VALUE_SILENT, this::isSilent, this::setSilent),
                new DefaultValue<>(VALUE_ENTITIES, this::hasEntityMode, this::setEntityMode),
        };
    }

    public static BaseBlock createBlock() {
        return new BaseBlock(new BlockBuilder()
                .tileEntitySupplier(BuilderTileEntity::new)
                .topDriver(RFToolsBuilderTOPDriver.DRIVER)
                .infusable()
                .manualEntry(ManualHelper.create("rftoolsbuilder:builder/builder_intro"))
                .info(key("message.rftoolsbuilder.shiftmessage"))
                .infoShift(header(), gold())) {
            @Override
            public RotationType getRotationType() {
                return RotationType.HORIZROTATION;
            }
        };
    }

    @Override
    public IAction[] getActions() {
        return new IAction[]{
                new DefaultAction("restart", this::restartScan)
        };
    }

    @Override
    protected boolean needsRedstoneMode() {
        return true;
    }

    @Override
    public Direction getBlockOrientation() {
        BlockState state = level.getBlockState(worldPosition);
        if (state.getBlock() == BuilderModule.BUILDER.get()) {
            return OrientationTools.getOrientationHoriz(state);
        } else {
            return null;
        }
    }

    @Override
    public boolean isBlockAboveAir() {
        return level.isEmptyBlock(worldPosition.above());
    }

    @Override
    public List<String> getClientLog() {
        return clientHudLog;
    }

    public List<String> getHudLog() {
        List<String> list = new ArrayList<>();
        list.add(TextFormatting.BLUE + "Mode:");
        if (isShapeCard()) {
            getCardType().addHudLog(list, items);
        } else {
            list.add("    Space card: " + new String[]{"copy", "move", "swap", "back", "collect"}[mode]);
        }
        if (scan != null) {
            list.add(TextFormatting.BLUE + "Progress:");
            list.add("    Y level: " + scan.getY());
            int minChunkX = minBox.getX() >> 4;
            int minChunkZ = minBox.getZ() >> 4;
            int maxChunkX = maxBox.getX() >> 4;
            int maxChunkZ = maxBox.getZ() >> 4;
            int curX = scan.getX() >> 4;
            int curZ = scan.getZ() >> 4;
            int totChunks = (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
            int curChunk = (curZ - minChunkZ) * (maxChunkX - minChunkX) + curX - minChunkX;
            list.add("    Chunk:  " + curChunk + " of " + totChunks);
        }
        if (lastError != null && !lastError.isEmpty()) {
            String[] errors = StringUtils.split(lastError, "\n");
            for (String error : errors) {
                list.add(TextFormatting.RED + error);
            }
        }
        return list;
    }

    @Override
    public BlockPos getHudPos() {
        return getBlockPos();
    }

    @Override
    public long getLastUpdateTime() {
        return lastHudTime;
    }

    @Override
    public void setLastUpdateTime(long t) {
        lastHudTime = t;
    }

    private boolean isShapeCard() {
        return items.getStackInSlot(SLOT_TAB).getItem() instanceof ShapeCardItem;
    }

    private CompoundNBT hasCard() {
        return items.getStackInSlot(SLOT_TAB).getTag();
    }

    private void makeSupportBlocksShaped() {
        ItemStack shapeCard = items.getStackInSlot(SLOT_TAB);
        BlockPos dimension = ShapeCardItem.getClampedDimension(shapeCard, BuilderConfiguration.maxBuilderDimension.get());
        BlockPos offset = ShapeCardItem.getClampedOffset(shapeCard, BuilderConfiguration.maxBuilderOffset.get());
        Shape shape = ShapeCardItem.getShape(shapeCard);
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        ShapeCardItem.composeFormula(shapeCard, shape.getFormulaFactory().get(), level, getBlockPos(), dimension, offset, blocks, BuilderConfiguration.maxBuilderDimension.get() * 256 * BuilderConfiguration.maxBuilderDimension.get(), false, false, null);
        BlockState state = BuilderModule.SUPPORT.get().defaultBlockState().setValue(SupportBlock.STATUS, SupportBlock.SupportStatus.STATUS_OK);
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            BlockPos p = entry.getKey();
            if (level.isEmptyBlock(p)) {
                level.setBlock(p, state, 2);
            }
        }
    }

    private void makeSupportBlocks() {
        if (isShapeCard()) {
            makeSupportBlocksShaped();
            return;
        }

        SpaceChamberRepository.SpaceChamberChannel chamberChannel = calculateBox();
        if (chamberChannel != null) {
            RegistryKey<World> dimension = chamberChannel.getDimension();
            World world = LevelTools.getLevel(this.level, dimension);
            if (world == null) {
                return;
            }

            PlayerEntity player = harvester.get();
            BlockPos.Mutable src = new BlockPos.Mutable();
            BlockPos.Mutable dest = new BlockPos.Mutable();
            for (int x = minBox.getX(); x <= maxBox.getX(); x++) {
                for (int y = minBox.getY(); y <= maxBox.getY(); y++) {
                    for (int z = minBox.getZ(); z <= maxBox.getZ(); z++) {
                        src.set(x, y, z);
                        sourceToDest(src, dest);
                        BlockState srcState = world.getBlockState(src);
                        Block srcBlock = srcState.getBlock();
                        BlockState dstState = world.getBlockState(dest);
                        Block dstBlock = dstState.getBlock();
                        SupportBlock.SupportStatus error = SupportBlock.SupportStatus.STATUS_OK;
                        if (mode != MODE_COPY) {
                            TileEntity srcTileEntity = world.getBlockEntity(src);
                            TileEntity dstTileEntity = world.getBlockEntity(dest);

                            SupportBlock.SupportStatus error1 = isMovable(player, world, src, srcBlock, srcTileEntity);
                            SupportBlock.SupportStatus error2 = isMovable(player, world, dest, dstBlock, dstTileEntity);
                            error = SupportBlock.SupportStatus.max(error1, error2);
                        }
                        if (isEmpty(srcState, srcBlock) && !isEmpty(dstState, dstBlock)) {
                            world.setBlock(src, BuilderModule.SUPPORT.get().defaultBlockState().setValue(SupportBlock.STATUS, error), 3);
                        }
                        if (isEmpty(dstState, dstBlock) && !isEmpty(srcState, srcBlock)) {
                            world.setBlock(dest, BuilderModule.SUPPORT.get().defaultBlockState().setValue(SupportBlock.STATUS, error), 3);
                        }
                    }
                }
            }
        }
    }

    private void clearSupportBlocksShaped() {
        ItemStack shapeCard = items.getStackInSlot(SLOT_TAB);
        BlockPos dimension = ShapeCardItem.getClampedDimension(shapeCard, BuilderConfiguration.maxBuilderDimension.get());
        BlockPos offset = ShapeCardItem.getClampedOffset(shapeCard, BuilderConfiguration.maxBuilderOffset.get());
        Shape shape = ShapeCardItem.getShape(shapeCard);
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        ShapeCardItem.composeFormula(shapeCard, shape.getFormulaFactory().get(), level, getBlockPos(), dimension, offset, blocks, BuilderConfiguration.maxSpaceChamberDimension.get() * BuilderConfiguration.maxSpaceChamberDimension.get() * BuilderConfiguration.maxSpaceChamberDimension.get(), false, false, null);
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            BlockPos p = entry.getKey();
            if (level.getBlockState(p).getBlock() == BuilderModule.SUPPORT.get()) {
                level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
            }
        }
    }

    public void clearSupportBlocks() {
        if (level.isClientSide) {
            // Don't do anything on the client.
            return;
        }

        if (isShapeCard()) {
            clearSupportBlocksShaped();
            return;
        }

        SpaceChamberRepository.SpaceChamberChannel chamberChannel = calculateBox();
        if (chamberChannel != null) {
            RegistryKey<World> dimension = chamberChannel.getDimension();
            World world = LevelTools.getLevel(this.level, dimension);

            BlockPos.Mutable src = new BlockPos.Mutable();
            BlockPos.Mutable dest = new BlockPos.Mutable();
            for (int x = minBox.getX(); x <= maxBox.getX(); x++) {
                for (int y = minBox.getY(); y <= maxBox.getY(); y++) {
                    for (int z = minBox.getZ(); z <= maxBox.getZ(); z++) {
                        src.set(x, y, z);
                        if (world != null) {
                            Block srcBlock = world.getBlockState(src).getBlock();
                            if (srcBlock == BuilderModule.SUPPORT.get()) {
                                world.setBlockAndUpdate(src, Blocks.AIR.defaultBlockState());
                            }
                        }
                        sourceToDest(src, dest);
                        Block dstBlock = world.getBlockState(dest).getBlock();
                        if (dstBlock == BuilderModule.SUPPORT.get()) {
                            world.setBlockAndUpdate(dest, Blocks.AIR.defaultBlockState());
                        }
                    }
                }
            }
        }
    }

    public boolean isHilightMode() {
        return hilightMode;
    }

    public void setHilightMode(boolean hilightMode) {
        this.hilightMode = hilightMode;
    }

    public boolean isWaitMode() {
        return waitMode;
    }

    public void setWaitMode(boolean waitMode) {
        this.waitMode = waitMode;
        markDirtyClient();
    }

    private boolean waitOrSkip(String error) {
        if (waitMode) {
            lastError = error;
        }
        return waitMode;
    }

    private boolean skip() {
        lastError = null;
        return false;
    }

    private boolean skip(String error) {
        lastError = error;
        return false;
    }

    public boolean suspend(int rfNeeded, BlockPos srcPos, BlockState srcState, BlockState pickState) {
        lastError = null;
        return true;
    }

    private boolean suspend(String error) {
        lastError = error;
        return true;
    }

    public boolean hasLoopMode() {
        return loopMode;
    }

    public void setLoopMode(boolean loopMode) {
        this.loopMode = loopMode;
        markDirtyClient();
    }

    public boolean hasEntityMode() {
        return entityMode;
    }

    public void setEntityMode(boolean entityMode) {
        this.entityMode = entityMode;
        markDirtyClient();
    }

    public boolean hasSupportMode() {
        return supportMode;
    }

    public void setSupportMode(boolean supportMode) {
        this.supportMode = supportMode;
        if (supportMode) {
            makeSupportBlocks();
        } else {
            clearSupportBlocks();
        }
        markDirtyClient();
    }

    public boolean isSilent() {
        return silent;
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
        markDirtyClient();
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        if (mode != this.mode) {
            this.mode = mode;
            restartScan();
            markDirtyClient();
        }
    }

    public void resetBox() {
        boxValid = false;
    }

    public int getAnchor() {
        return anchor;
    }

    public void setAnchor(int anchor) {
        if (supportMode) {
            clearSupportBlocks();
        }
        boxValid = false;

        this.anchor = anchor;

        if (isShapeCard()) {
            // If there is a shape card we modify it for the new settings.
            ItemStack shapeCard = items.getStackInSlot(SLOT_TAB);
            BlockPos dimension = ShapeCardItem.getDimension(shapeCard);
            BlockPos minBox = positionBox(dimension);
            int dx = dimension.getX();
            int dy = dimension.getY();
            int dz = dimension.getZ();

            BlockPos offset = new BlockPos(minBox.getX() + (int) Math.ceil(dx / 2), minBox.getY() + (int) Math.ceil(dy / 2), minBox.getZ() + (int) Math.ceil(dz / 2));
            ShapeCardItem.setOffset(shapeCard, offset.getX(), offset.getY(), offset.getZ());
        }

        if (supportMode) {
            makeSupportBlocks();
        }
        markDirtyClient();
    }

    // Give a dimension, return a min coordinate of the box right in front of the builder
    private BlockPos positionBox(BlockPos dimension) {
        BlockState state = level.getBlockState(getBlockPos());
        Direction direction = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        int spanX = dimension.getX();
        int spanY = dimension.getY();
        int spanZ = dimension.getZ();
        int x = 0;
        int y;
        int z = 0;
        y = -((anchor == ANCHOR_NE || anchor == ANCHOR_NW) ? spanY - 1 : 0);
        switch (direction) {
            case SOUTH:
                x = -((anchor == ANCHOR_NE || anchor == ANCHOR_SE) ? spanX - 1 : 0);
                z = -spanZ;
                break;
            case NORTH:
                x = 1 - spanX + ((anchor == ANCHOR_NE || anchor == ANCHOR_SE) ? spanX - 1 : 0);
                z = 1;
                break;
            case WEST:
                x = 1;
                z = -((anchor == ANCHOR_NE || anchor == ANCHOR_SE) ? spanZ - 1 : 0);
                break;
            case EAST:
                x = -spanX;
                z = -((anchor == ANCHOR_NE || anchor == ANCHOR_SE) ? 0 : spanZ - 1);
                break;
            case DOWN:
            case UP:
            default:
                break;
        }
        return new BlockPos(x, y, z);
    }


    public int getRotate() {
        return rotate;
    }

    public void setRotate(int rotate) {
        if (supportMode) {
            clearSupportBlocks();
        }
        boxValid = false;
        this.rotate = rotate;
        if (supportMode) {
            makeSupportBlocks();
        }
        markDirtyClient();
    }

    @Override
    public void setPowerInput(int powered) {
        boolean o = isMachineEnabled();
        super.setPowerInput(powered);
        boolean n = isMachineEnabled();
        if (o != n) {
            if (loopMode || (n && scan == null)) {
                restartScan();
            }
        }
    }

    private void createProjection(SpaceChamberRepository.SpaceChamberChannel chamberChannel) {
        BlockPos minC = rotate(chamberChannel.getMinCorner());
        BlockPos maxC = rotate(chamberChannel.getMaxCorner());
        BlockPos minCorner = new BlockPos(Math.min(minC.getX(), maxC.getX()), Math.min(minC.getY(), maxC.getY()), Math.min(minC.getZ(), maxC.getZ()));
        BlockPos maxCorner = new BlockPos(Math.max(minC.getX(), maxC.getX()), Math.max(minC.getY(), maxC.getY()), Math.max(minC.getZ(), maxC.getZ()));

        BlockState state = level.getBlockState(getBlockPos());
        Direction direction = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        int xCoord = getBlockPos().getX();
        int yCoord = getBlockPos().getY();
        int zCoord = getBlockPos().getZ();
        int spanX = maxCorner.getX() - minCorner.getX();
        int spanY = maxCorner.getY() - minCorner.getY();
        int spanZ = maxCorner.getZ() - minCorner.getZ();
        switch (direction) {
            case SOUTH:
                projDx = xCoord + Direction.NORTH.getNormal().getX() - minCorner.getX() - ((anchor == ANCHOR_NE || anchor == ANCHOR_SE) ? spanX : 0);
                projDz = zCoord + Direction.NORTH.getNormal().getZ() - minCorner.getZ() - spanZ;
                break;
            case NORTH:
                projDx = xCoord + Direction.SOUTH.getNormal().getX() - minCorner.getX() - spanX + ((anchor == ANCHOR_NE || anchor == ANCHOR_SE) ? spanX : 0);
                projDz = zCoord + Direction.SOUTH.getNormal().getZ() - minCorner.getZ();
                break;
            case WEST:
                projDx = xCoord + Direction.EAST.getNormal().getX() - minCorner.getX();
                projDz = zCoord + Direction.EAST.getNormal().getZ() - minCorner.getZ() - ((anchor == ANCHOR_NE || anchor == ANCHOR_SE) ? spanZ : 0);
                break;
            case EAST:
                projDx = xCoord + Direction.WEST.getNormal().getX() - minCorner.getX() - spanX;
                projDz = zCoord + Direction.WEST.getNormal().getZ() - minCorner.getZ() - spanZ + ((anchor == ANCHOR_NE || anchor == ANCHOR_SE) ? spanZ : 0);
                break;
            case DOWN:
            case UP:
            default:
                break;
        }
        projDy = yCoord - minCorner.getY() - ((anchor == ANCHOR_NE || anchor == ANCHOR_NW) ? spanY : 0);
    }

    private void calculateBox(CompoundNBT cardCompound) {
        int channel = cardCompound.getInt("channel");

        SpaceChamberRepository repository = SpaceChamberRepository.get(level);
        SpaceChamberRepository.SpaceChamberChannel chamberChannel = repository.getChannel(channel);
        BlockPos minCorner = chamberChannel.getMinCorner();
        BlockPos maxCorner = chamberChannel.getMaxCorner();
        if (minCorner == null || maxCorner == null) {
            return;
        }

        if (boxValid) {
            // Double check if the box is indeed still valid.
            if (minCorner.equals(minBox) && maxCorner.equals(maxBox)) {
                return;
            }
        }

        boxValid = true;
        cardType = ShapeCardType.CARD_SPACE;

        createProjection(chamberChannel);

        minBox = minCorner;
        maxBox = maxCorner;
        restartScan();
    }

    private void checkStateServerShaped() {
        float factor = infusableHandler.map(IInfusable::getInfusedFactor).orElse(0.0f);
        for (int i = 0; i < BuilderConfiguration.quarryBaseSpeed.get() + (factor * BuilderConfiguration.quarryInfusionSpeedFactor.get()); i++) {
            if (scan != null) {
                handleBlockShaped();
            }
        }
    }


    @Override
    public void tick() {
        if( level == null )
            return;

        if (!level.isClientSide) {
            checkStateServer();
        }
    }

    private void checkStateServer() {
        if (!overflowItems.isEmpty()) {
            insertItems(overflowItems.extractList());
        }

        if (!isMachineEnabled() && loopMode) {
            return;
        }

        if (scan == null) {
            return;
        }

        if (isHilightMode()) {
            updateHilight();
        }

        if (isShapeCard()) {
            if (!isMachineEnabled()) {
                chunkUnload();
                return;
            }
            checkStateServerShaped();
            return;
        }

        SpaceChamberRepository.SpaceChamberChannel chamberChannel = calculateBox();
        if (chamberChannel == null) {
            scan = null;
            setChanged();
            return;
        }

        RegistryKey<World> dimension = chamberChannel.getDimension();
        World world = LevelTools.getLevel(this.level, dimension);
        if (world == null) {
            // The other location must be loaded.
            return;
        }

        if (mode == MODE_COLLECT) {
            collectItems(world);
        } else {
            float factor = infusableHandler.map(IInfusable::getInfusedFactor).orElse(0.0f);
            for (int i = 0; i < 2 + (factor * 40); i++) {
                if (scan != null) {
                    handleBlock(world);
                }
            }
        }
    }

    private void updateHilight() {
        scanLocCnt--;
        if (scanLocCnt <= 0) {
            scanLocCnt = 5;
            int x = scan.getX();
            int y = scan.getY();
            int z = scan.getZ();
            double sqradius = 30 * 30;
            for (ServerPlayerEntity player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                if (Objects.equals(player.getCommandSenderWorld().dimension(), level.dimension())) {
                    double d0 = x - player.getX();
                    double d1 = y - player.getY();
                    double d2 = z - player.getZ();
                    if (d0 * d0 + d1 * d1 + d2 * d2 < sqradius) {
                        RFToolsBuilderMessages.sendToClient(player, ClientCommandHandler.CMD_POSITION_TO_CLIENT,
                                TypedMap.builder().put(ClientCommandHandler.PARAM_POS, getBlockPos()).put(ClientCommandHandler.PARAM_SCAN, scan));
                    }
                }
            }
        }
    }

    private void collectItems(World world) {
        // Collect item mode
        collectCounter--;
        if (collectCounter > 0) {
            return;
        }
        collectCounter = BuilderConfiguration.collectTimer.get();
        if (!loopMode) {
            scan = null;
        }

        float factor = infusableHandler.map(IInfusable::getInfusedFactor).orElse(0.0f);

        long rf = energyStorage.getEnergyStored();
        float area = (maxBox.getX() - minBox.getX() + 1) * (maxBox.getY() - minBox.getY() + 1) * (maxBox.getZ() - minBox.getZ() + 1);
        float infusedFactor = (4.0f - factor) / 4.0f;
        int rfNeeded = (int) (BuilderConfiguration.collectRFPerTickPerArea.get() * area * infusedFactor) * BuilderConfiguration.collectTimer.get();
        if (rfNeeded > rf) {
            // Not enough energy.
            return;
        }
        energyStorage.consumeEnergy(rfNeeded);

        AxisAlignedBB bb = new AxisAlignedBB(minBox.getX() - .8, minBox.getY() - .8, minBox.getZ() - .8, maxBox.getX() + .8, maxBox.getY() + .8, maxBox.getZ() + .8);
        List<Entity> items = world.getEntitiesOfClass(Entity.class, bb);
        for (Entity entity : items) {
            if (entity instanceof ItemEntity) {
                if (collectItem(world, factor, (ItemEntity) entity)) {
                    return;
                }
            } else if (entity instanceof ExperienceOrbEntity) {
                if (collectXP(world, factor, (ExperienceOrbEntity) entity)) {
                    return;
                }
            }
        }
    }

    private boolean collectXP(World world, float infusedFactor, ExperienceOrbEntity orb) {
        int xp = orb.getValue();
        long rf = energyStorage.getEnergyStored();
        int rfNeeded = (int) (BuilderConfiguration.collectRFPerXP.get() * infusedFactor * xp);
        if (rfNeeded > rf) {
            // Not enough energy.
            return true;
        }

        collectXP += xp;

        int bottles = collectXP / 7;
        if (bottles > 0) {
            if (insertItem(new ItemStack(Items.EXPERIENCE_BOTTLE, bottles)).isEmpty()) {
                collectXP = collectXP % 7;
                ((ServerWorld) world).despawn(orb);
                energyStorage.consumeEnergy(rfNeeded);
            } else {
                collectXP = 0;
            }
        }

        return false;
    }

    private boolean collectItem(World world, float infusedFactor, ItemEntity item) {
        ItemStack stack = item.getItem();

        Predicate<ItemStack> predicate = filterCache.get();
        if (predicate != null && !predicate.test(stack)) {
            return false;
        }

        long rf = energyStorage.getEnergyStored();
        int rfNeeded = (int) (BuilderConfiguration.collectRFPerItem.get() * infusedFactor) * stack.getCount();
        if (rfNeeded > rf) {
            // Not enough energy.
            return true;
        }
        energyStorage.consumeEnergy(rfNeeded);

        ((ServerWorld) world).despawn(item);
        stack = insertItem(stack);
        if (!stack.isEmpty()) {
            BlockPos position = item.blockPosition();
            ItemEntity entityItem = new ItemEntity(world, position.getX(), position.getY(), position.getZ(), stack);
            world.addFreshEntity(entityItem);
        }
        return false;
    }

    private void calculateBoxShaped() {
        ItemStack shapeCard = items.getStackInSlot(SLOT_TAB);
        if (shapeCard.isEmpty()) {
            return;
        }
        BlockPos dimension = ShapeCardItem.getClampedDimension(shapeCard, BuilderConfiguration.maxBuilderDimension.get());
        BlockPos offset = ShapeCardItem.getClampedOffset(shapeCard, BuilderConfiguration.maxBuilderOffset.get());

        BlockPos minCorner = ShapeCardItem.getMinCorner(getBlockPos(), dimension, offset);
        BlockPos maxCorner = ShapeCardItem.getMaxCorner(getBlockPos(), dimension, offset);
        if (minCorner.getY() < 0) {
            minCorner = new BlockPos(minCorner.getX(), 0, minCorner.getZ());
        } else if (minCorner.getY() > 255) {
            minCorner = new BlockPos(minCorner.getX(), 255, minCorner.getZ());
        }
        if (maxCorner.getY() < 0) {
            maxCorner = new BlockPos(maxCorner.getX(), 0, maxCorner.getZ());
        } else if (maxCorner.getY() > 255) {
            maxCorner = new BlockPos(maxCorner.getX(), 255, maxCorner.getZ());
        }

        if (boxValid) {
            // Double check if the box is indeed still valid.
            if (minCorner.equals(minBox) && maxCorner.equals(maxBox)) {
                return;
            }
        }

        boxValid = true;
        cardType = ShapeCardItem.getType(shapeCard);

        cachedBlocks = null;
        cachedChunk = null;
        cachedVoidableBlocks.clear();
        minBox = minCorner;
        maxBox = maxCorner;
        restartScan();
    }

    private SpaceChamberRepository.SpaceChamberChannel calculateBox() {
        CompoundNBT tc = hasCard();
        if (tc == null) {
            return null;
        }

        int channel = tc.getInt("channel");
        if (channel == -1) {
            return null;
        }

        SpaceChamberRepository repository = SpaceChamberRepository.get(level);
        SpaceChamberRepository.SpaceChamberChannel chamberChannel = repository.getChannel(channel);
        if (chamberChannel == null) {
            return null;
        }

        calculateBox(tc);

        if (!boxValid) {
            return null;
        }
        return chamberChannel;
    }

    private Map<BlockPos, BlockState> getCachedBlocks(ChunkPos chunk) {
        if ((chunk != null && !chunk.equals(cachedChunk)) || (chunk == null && cachedChunk != null)) {
            cachedBlocks = null;
        }

        if (cachedBlocks == null) {
            cachedBlocks = new HashMap<>();
            ItemStack shapeCard = items.getStackInSlot(SLOT_TAB);
            Shape shape = ShapeCardItem.getShape(shapeCard);
            boolean solid = ShapeCardItem.isSolid(shapeCard);
            BlockPos dimension = ShapeCardItem.getClampedDimension(shapeCard, BuilderConfiguration.maxBuilderDimension.get());
            BlockPos offset = ShapeCardItem.getClampedOffset(shapeCard, BuilderConfiguration.maxBuilderOffset.get());
            boolean forquarry = !ShapeCardItem.isNormalShapeCard(shapeCard);
            ShapeCardItem.composeFormula(shapeCard, shape.getFormulaFactory().get(), level, getBlockPos(), dimension, offset, cachedBlocks, BuilderConfiguration.maxSpaceChamberDimension.get() * BuilderConfiguration.maxSpaceChamberDimension.get() * BuilderConfiguration.maxSpaceChamberDimension.get(), solid, forquarry, chunk);
            cachedChunk = chunk;
        }
        return cachedBlocks;
    }

    private void handleBlockShaped() {
        for (int i = 0; i < 100; i++) {
            if (scan == null) {
                return;
            }
            Map<BlockPos, BlockState> blocks = getCachedBlocks(new ChunkPos(scan.getX() >> 4, scan.getZ() >> 4));
            if (blocks.containsKey(scan)) {
                BlockState state = blocks.get(scan);
                if (!handleSingleBlock(state)) {
                    nextLocation();
                }
                return;
            } else {
                nextLocation();
            }
        }
    }

    private ShapeCardType getCardType() {
        if (cardType == ShapeCardType.CARD_UNKNOWN) {
            return ShapeCardItem.getType(items.getStackInSlot(SLOT_TAB));
        }
        return cardType;
    }

    // Return true if we have to wait at this spot.
    private boolean handleSingleBlock(BlockState pickState) {
        if( level == null )
            return false;

        BlockPos srcPos = scan;
        int sx = scan.getX();
        int sy = scan.getY();
        int sz = scan.getZ();
        if (!chunkLoad(sx, sz)) {
            // The chunk is not available and we could not chunkload it. We have to wait.
            return suspend("Chunk not available!");
        }

        int rfNeeded = getCardType().getRfNeeded();

        BlockState state = null;
        if (getCardType() != ShapeCardType.CARD_SHAPE && getCardType() != ShapeCardType.CARD_PUMP_LIQUID) {
            state = level.getBlockState(srcPos);
            Block block = state.getBlock();
            if (!isEmpty(state, block)) {
                float hardness;
                if (isFluidBlock(block)) {
                    hardness = 1.0f;
                } else {
                    if (cachedVoidableBlocks.get().contains(block)) {
                        rfNeeded = (int) (BuilderConfiguration.builderRfPerQuarry.get() * BuilderConfiguration.voidShapeCardFactor.get());
                    }
                    hardness = state.getDestroySpeed(level, srcPos);
                }
                rfNeeded *= (int) ((hardness + 1) * 2);
            }
        }

        float factor = infusableHandler.map(IInfusable::getInfusedFactor).orElse(0.0f);
        rfNeeded = (int) (rfNeeded * (3.0f - factor) / 3.0f);

        if (rfNeeded > energyStorage.getEnergyStored()) {
            // Not enough energy.
            return suspend("Not enough power!");
        }

        return getCardType().handleSingleBlock(this, rfNeeded, srcPos, state, pickState);
    }

    public boolean buildBlock(int rfNeeded, BlockPos srcPos, BlockState srcState, BlockState pickState) {
        if( level == null )
            return false;

        if (isEmptyOrReplacable(level, srcPos)) {
            TakeableItem item = createTakeableItem(level, srcPos, pickState);
            ItemStack stack = item.peek();
            if (stack.isEmpty()) {
                return waitOrSkip("Cannot find block!\nor missing inventory\non top or below");    // We could not find a block. Wait
            }

            PlayerEntity fakePlayer = harvester.get();
            BlockState newState = BlockTools.placeStackAt(fakePlayer, stack, level, srcPos, pickState);
            if (newState == null) {
                return waitOrSkip("Cannot place block!");
            }
            if (!ItemStack.matches(stack, item.peek())) { // Did we actually use up whatever we were holding?
                if (!stack.isEmpty()) { // Are we holding something else that we should put back?
                    stack = item.takeAndReplace(stack); // First try to put our new item where we got what we placed
                    if (!stack.isEmpty()) { // If that didn't work, then try to put it anywhere it will fit
                        stack = insertItem(stack);
                        if (!stack.isEmpty()) { // If that still didn't work, then just drop whatever we're holding
                            level.addFreshEntity(new ItemEntity(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), stack));
                        }
                    }
                } else {
                    item.take(); // If we aren't holding anything, then just consume what we placed
                }
            }

            if (!silent) {
                SoundType sound = newState.getBlock().getSoundType(newState, level, srcPos, fakePlayer);
                playPlaceSoundSafe(sound, level, newState, srcPos.getX(), srcPos.getY(), srcPos.getZ());
            }

            energyStorage.consumeEnergy(rfNeeded);
        }
        return skip();
    }

    private void playPlaceSoundSafe(SoundType sound, World world, BlockState state, int x, int y, int z) {
        try {
            SoundTools.playSound(world, sound.getPlaceSound(), x, y, z, 1.0f, 1.0f);
        } catch (Exception e) {
            Logging.getLogger().error("Error getting soundtype from " + state.getBlock().getRegistryName() + "! Please report to the mod owner!");
        }
    }

    private void playBreakSoundSafe(SoundType sound, World world, BlockState state, int x, int y, int z) {
        try {
            SoundTools.playSound(world, sound.getBreakSound(), x, y, z, 1.0f, 1.0f);
        } catch (Exception e) {
            Logging.getLogger().error("Error getting soundtype from " + state.getBlock().getRegistryName() + "! Please report to the mod owner!");
        }
    }

    private Set<Block> getCachedVoidableBlocks() {
        ItemStack card = items.getStackInSlot(SLOT_TAB);
        if (!card.isEmpty() && card.getItem() instanceof ShapeCardItem) {
            return ShapeCardItem.getVoidedBlocks(card);
        } else {
            return Collections.emptySet();
        }
    }

    private void clearOrDirtBlock(int rfNeeded, BlockPos spos, BlockState srcState, boolean clear) {
        if (clear) {
            level.setBlock(spos, Blocks.AIR.defaultBlockState(), 2);
        } else {
            level.setBlock(spos, getReplacementBlock(), 2);       // No block update!
        }
        energyStorage.consumeEnergy(rfNeeded);
        if (!silent) {
            SoundType soundType = srcState.getBlock().getSoundType(srcState, level, spos, null);
            playBreakSoundSafe(soundType, level, srcState, spos.getX(), spos.getY(), spos.getZ());
        }
    }

    private BlockState getReplacementBlock() {
        return BuilderConfiguration.getQuarryReplace();
    }

    public boolean silkQuarryBlock(int rfNeeded, BlockPos srcPos, BlockState srcState, BlockState pickState) {
        return commonQuarryBlock(true, rfNeeded, srcPos, srcState);
    }

    private Predicate<ItemStack> createFilterCache() {
        return FilterModuleItem.getCache(items.getStackInSlot(SLOT_FILTER));
    }

    private boolean allowedToBreak(BlockState state, World world, BlockPos pos, PlayerEntity player) {
        if (!state.getBlock().canEntityDestroy(state, world, pos, player)) {
            return skip("Cannot destroy!\nAre fake players\nallowed?");
        }
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(world, pos, state, player);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return skip("Break was canceled!");
        }
        return true;
    }

    private static boolean allowedToBreakS(BlockState state, World world, BlockPos pos, PlayerEntity player) {
        if (!state.getBlock().canEntityDestroy(state, world, pos, player)) {
            return false;
        }
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(world, pos, state, player);
        MinecraftForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

    public boolean quarryBlock(int rfNeeded, BlockPos srcPos, BlockState srcState, BlockState pickState) {
        return commonQuarryBlock(false, rfNeeded, srcPos, srcState);
    }

    private static ItemStack getHarvesterTool(boolean silk, int fortune) {
        if (silk) {
            if (TOOL_SILK == null || TOOL_SILK.isEmpty()) {
                TOOL_SILK = new ItemStack(BuilderModule.SUPER_HARVESTING_TOOL.get());
                TOOL_SILK.enchant(Enchantments.SILK_TOUCH, 1);
            }
            return TOOL_SILK;
        } else if (fortune > 0) {
            if (TOOL_FORTUNE == null || TOOL_FORTUNE.isEmpty()) {
                TOOL_FORTUNE = new ItemStack(BuilderModule.SUPER_HARVESTING_TOOL.get());
                TOOL_FORTUNE.enchant(Enchantments.BLOCK_FORTUNE, fortune);
            }
            return TOOL_FORTUNE;

        } else {
            if (TOOL_NORMAL == null || TOOL_NORMAL.isEmpty()) {
                TOOL_NORMAL = new ItemStack(BuilderModule.SUPER_HARVESTING_TOOL.get());
            }
            return TOOL_NORMAL;
        }
    }

    private boolean commonQuarryBlock(boolean silk, int rfNeeded, BlockPos srcPos, BlockState srcState) {
        Block block = srcState.getBlock();
        int xCoord = getBlockPos().getX();
        int yCoord = getBlockPos().getY();
        int zCoord = getBlockPos().getZ();
        int sx = srcPos.getX();
        int sy = srcPos.getY();
        int sz = srcPos.getZ();
        if (sx >= xCoord - 1 && sx <= xCoord + 1 && sy >= yCoord - 1 && sy <= yCoord + 1 && sz >= zCoord - 1 && sz <= zCoord + 1) {
            // Skip a 3x3x3 block around the builder.
            return skip();
        }
        if (isEmpty(srcState, block)) {
            return skip();
        }
        if (srcState.getDestroySpeed(level, srcPos) >= 0) {
            boolean clear = getCardType().isClearing();
            if ((!clear) && srcState == getReplacementBlock()) {
                // We can skip dirt if we are not clearing.
                return skip();
            }
            if ((!BuilderConfiguration.quarryTileEntities.get()) && level.getBlockEntity(srcPos) != null) {
                // Skip tile entities
                return skip();
            }

            PlayerEntity fakePlayer = harvester.get();
            if (allowedToBreak(srcState, level, srcPos, fakePlayer)) {
                ItemStack filter = items.getStackInSlot(SLOT_FILTER);
                if (!filter.isEmpty()) {
                    if (filterCache.get() != null) {
                        boolean match;
                        try {
                            match = filterCache.get().test(block.getCloneItemStack(level, srcPos, srcState));
                        } catch (Exception e) {
                            // In case block.getItem() fails (like can happen for banner blocks because the getItem()
                            // for that fails on servers due to calling a client-side method)
                            match = false;
                        }
                        if (!match) {
                            energyStorage.consumeEnergy(Math.min(rfNeeded, BuilderConfiguration.builderRfPerSkipped.get()));
                            return skip();   // Skip this
                        }
                    }
                }
                if (!cachedVoidableBlocks.get().contains(block)) {
                    if (!overflowItems.isEmpty()) {
                        // Don't harvest any new blocks if we're still overflowing with the drops from a previous block
                        return waitOrSkip("Not enough room!\nor no usable storage\non top or below!");
                    }

                    int fortune = getCardType().isFortune() ? 3 : 0;
                    LootContext.Builder builder = new LootContext.Builder((ServerWorld) level)
                            .withRandom(level.random)
                            .withParameter(LootParameters.ORIGIN, new Vector3d(srcPos.getX(), srcPos.getY(), srcPos.getZ()))
                            .withParameter(LootParameters.TOOL, getHarvesterTool(silk, fortune))
                            .withOptionalParameter(LootParameters.BLOCK_ENTITY, level.getBlockEntity(srcPos));
                    if (fortune > 0) {
                        builder.withLuck(fortune);
                    }
                    List<ItemStack> drops = srcState.getDrops(builder);
                    if (checkValidItems(block, drops) && !insertItems(drops)) {
                        clearOrDirtBlock(rfNeeded, srcPos, srcState, clear);
                        return waitOrSkip("Not enough room!\nor no usable storage\non top or below!");    // Not enough room. Wait
                    }
                }
                clearOrDirtBlock(rfNeeded, srcPos, srcState, clear);
            } else {
                return waitOrSkip(lastError);
            }
        }
        return false;
    }

    private static boolean isFluidBlock(Block block) {
        return block instanceof FlowingFluidBlock;
    }

    private static int getFluidLevel(BlockState srcState) {
        if (srcState.getBlock() instanceof FlowingFluidBlock) {
            return srcState.getValue(FlowingFluidBlock.LEVEL);
        }
        return -1;
    }

    public boolean placeLiquidBlock(int rfNeeded, BlockPos srcPos, BlockState srcState, BlockState pickState) {

        if (isEmptyOrReplacable(level, srcPos)) {
            FluidStack stack = consumeLiquid(level, srcPos);
            if (stack.isEmpty()) {
                return waitOrSkip("Cannot find liquid!\nor no usable tank\nabove or below");    // We could not find a block. Wait
            }

            Fluid fluid = stack.getFluid();
            if (fluid.getAttributes().doesVaporize(level, srcPos, stack) && level.dimensionType().ultraWarm()) {
                fluid.getAttributes().vaporize(null, level, srcPos, stack);
            } else {
                // We assume here the liquid is placable.
                Block block = fluid.defaultFluidState().createLegacyBlock().getBlock();   // @todo 1.14 check blockstate
                PlayerEntity fakePlayer = harvester.get();
                level.setBlock(srcPos, block.defaultBlockState(), 11);

                if (!silent) {
                    SoundType soundType = block.getSoundType(block.defaultBlockState(), level, srcPos, fakePlayer);
                    playPlaceSoundSafe(soundType, level, block.defaultBlockState(), srcPos.getX(), srcPos.getY(), srcPos.getZ());
                }
            }

            energyStorage.consumeEnergy(rfNeeded);
        }
        return skip();
    }

    public boolean pumpBlock(int rfNeeded, BlockPos srcPos, BlockState srcState, BlockState pickState) {
        Block block = srcState.getBlock();

        FluidState fluidState = level.getFluidState(srcPos);

        if (fluidState == null) {
            return skip();
        }

        if (!fluidState.isSource()) {
            return skip();
        }

        FluidStack fluidStack = FluidTools.pickupFluidBlock(level, srcPos, s -> false, () -> {
        });
        if (fluidStack.isEmpty()) {
            return skip();
        }

        // @todo 1.14, probably no longer needed?
//        if (!isFluidBlock(block)) {
//            return skip();
//        }

        // @todo 1.14, probably no longer needed?
//        if (getFluidLevel(srcState) != 0) {
//            return skip();
//        }

        if (srcState.getDestroySpeed(level, srcPos) >= 0) {
            PlayerEntity fakePlayer = harvester.get();
            if (allowedToBreak(srcState, level, srcPos, fakePlayer)) {
                if (checkAndInsertFluids(fluidStack)) {
                    energyStorage.consumeEnergy(rfNeeded);
                    boolean clear = getCardType().isClearing();
                    FluidTools.pickupFluidBlock(level, srcPos, s -> true, () -> {
                        if (clear) {
                            level.setBlock(srcPos, Blocks.AIR.defaultBlockState(), 2);
                        } else {
                            level.setBlock(srcPos, getReplacementBlock(), 2);       // No block update!
                        }
                    });
                    if (!silent) {
                        SoundType soundType = block.getSoundType(srcState, level, srcPos, fakePlayer);
                        playBreakSoundSafe(soundType, level, srcState, srcPos.getX(), srcPos.getY(), srcPos.getZ());
                    }
                    return skip();
                }
                return waitOrSkip("No room for liquid\nor no usable tank\nabove or below!");    // No room in tanks or not a valid tank: wait
            } else {
                return waitOrSkip(lastError);
            }
        }
        return skip();
    }

    public boolean voidBlock(int rfNeeded, BlockPos srcPos, BlockState srcState, BlockState pickState) {
        Block block = srcState.getBlock();
        int xCoord = getBlockPos().getX();
        int yCoord = getBlockPos().getY();
        int zCoord = getBlockPos().getZ();
        int sx = srcPos.getX();
        int sy = srcPos.getY();
        int sz = srcPos.getZ();
        if (sx >= xCoord - 1 && sx <= xCoord + 1 && sy >= yCoord - 1 && sy <= yCoord + 1 && sz >= zCoord - 1 && sz <= zCoord + 1) {
            // Skip a 3x3x3 block around the builder.
            return skip();
        }
        PlayerEntity fakePlayer = harvester.get();
        if (allowedToBreak(srcState, level, srcPos, fakePlayer)) {
            assert level != null;
            if (srcState.getDestroySpeed(level, srcPos) >= 0) {
                ItemStack filter = items.getStackInSlot(SLOT_FILTER);
                if (!filter.isEmpty()) {
                    if (filterCache.get() != null) {
                        boolean match = filterCache.get().test(block.getCloneItemStack(level, srcPos, srcState));
                        if (!match) {
                            energyStorage.consumeEnergy(Math.min(rfNeeded, BuilderConfiguration.builderRfPerSkipped.get()));
                            return skip();   // Skip this
                        }
                    }
                }

                if (!silent) {
                    SoundType soundType = block.getSoundType(srcState, level, srcPos, fakePlayer);
                    playBreakSoundSafe(soundType, level, srcState, sx, sy, sz);
                }
                level.setBlockAndUpdate(srcPos, Blocks.AIR.defaultBlockState());
                energyStorage.consumeEnergy(rfNeeded);
            }
        } else {
            return waitOrSkip(lastError);
        }
        return skip();
    }

    private void handleBlock(World world) {
        BlockPos srcPos = scan;
        BlockPos destPos = sourceToDest(scan);
        int x = scan.getX();
        int y = scan.getY();
        int z = scan.getZ();
        int destX = destPos.getX();
        int destY = destPos.getY();
        int destZ = destPos.getZ();

        switch (mode) {
            case MODE_COPY:
                copyBlock(world, srcPos, world, destPos);
                break;
            case MODE_MOVE:
                if (entityMode) {
                    moveEntities(world, x, y, z, world, destX, destY, destZ);
                }
                moveBlock(world, srcPos, world, destPos, rotate);
                break;
            case MODE_BACK:
                if (entityMode) {
                    moveEntities(world, destX, destY, destZ, world, x, y, z);
                }
                moveBlock(world, destPos, world, srcPos, oppositeRotate());
                break;
            case MODE_SWAP:
                if (entityMode) {
                    swapEntities(world, x, y, z, world, destX, destY, destZ);
                }
                swapBlock(world, srcPos, world, destPos);
                break;
        }

        nextLocation();
    }

    private static Random random = new Random();

    // Also works if block is null and just picks the first available block.
    private TakeableItem findBlockTakeableItem(IItemHandler inventory, World srcWorld, BlockPos srcPos, BlockState state) {
        if (state == null) {
            // We are not looking for a specific block. Pick a random one out of the chest.
            List<Integer> slots = new ArrayList<>();
            for (int i = 0; i < inventory.getSlots(); i++) {
                if (isPlacable(inventory.getStackInSlot(i))) {
                    slots.add(i);
                }
            }
            if (!slots.isEmpty()) {
                return new TakeableItem(inventory, slots.get(random.nextInt(slots.size())));
            }
        } else {
            Block block = state.getBlock();
            ItemStack srcItem = block.getCloneItemStack(srcWorld, srcPos, state);
            if (isPlacable(srcItem)) {
                for (int i = 0; i < inventory.getSlots(); i++) {
                    ItemStack stack = inventory.getStackInSlot(i);
                    if (!stack.isEmpty() && stack.sameItem(srcItem)) {
                        return new TakeableItem(inventory, i);
                    }
                }
            }
        }
        return TakeableItem.EMPTY;
    }

    private boolean isPlacable(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        return item instanceof BlockItem || item instanceof IPlantable;
    }

    // the items that we try to insert.
    private boolean checkValidItems(Block block, List<ItemStack> items) {
        for (ItemStack stack : items) {
            if ((!stack.isEmpty()) && stack.getItem() == null) {
                Logging.logError("Builder tried to quarry " + block.getRegistryName().toString() + " and it returned null item!");
                Broadcaster.broadcast(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), "Builder tried to quarry "
                                + block.getRegistryName().toString() + " and it returned null item!\nPlease report to mod author!",
                        10);
                return false; // We don't wait for this. Just skip the item
            }
        }
        return true;
    }

    private boolean checkAndInsertFluids(FluidStack fluid) {
        if (checkFluidTank(fluid, getBlockPos().above(), Direction.DOWN)) {
            return true;
        }
        if (checkFluidTank(fluid, getBlockPos().below(), Direction.UP)) {
            return true;
        }
        return false;
    }

    private boolean checkFluidTank(FluidStack fluidStack, BlockPos up, Direction side) {
        TileEntity te = level.getBlockEntity(up);
        if (te != null) {
            return te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side).map(h -> {
                int amount = h.fill(fluidStack, IFluidHandler.FluidAction.SIMULATE);
                if (amount == 1000) {
                    h.fill(fluidStack, IFluidHandler.FluidAction.EXECUTE);
                    return true;
                }
                return false;
            }).orElse(false);
        }
        return false;
    }

    private LazyList<ItemStack> couldntHandle1 = new LazyList<>();
    private LazyList<ItemStack> couldntHandle2 = new LazyList<>();

    // Tries to insert the items in the given item handler (if the te represents an item handler)
    // All items that could not be inserted are put on the couldntHandle list (for example, because
    // there is no item handler or the item handler is full)
    private void handleItemInsertion(@Nullable TileEntity te, Direction direction, List<ItemStack> items, LazyList<ItemStack> couldntHandle) {
        if (te == null) {
            couldntHandle.copyList(items);
            return;
        }
        LazyOptional<IItemHandler> capability = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, direction);
        if (!capability.isPresent()) {
            couldntHandle.copyList(items);
            return;
        }
        couldntHandle.clear();
        te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, Direction.DOWN).ifPresent(h -> {
            for (ItemStack item : items) {
                ItemStack overflow = ItemHandlerHelper.insertItem(h, item, false);
                if (!overflow.isEmpty()) {
                    couldntHandle.add(overflow);
                }
            }
        });
    }

    private boolean insertItems(List<ItemStack> items) {
        handleItemInsertion(level.getBlockEntity(worldPosition.above()), Direction.DOWN, items, couldntHandle1);
        if (couldntHandle1.isEmpty()) {
            // All ok
            return true;
        }

        handleItemInsertion(level.getBlockEntity(worldPosition.below()), Direction.UP, couldntHandle1.getList(), couldntHandle2);
        if (couldntHandle2.isEmpty()) {
            // Now it is ok
            return true;
        }

        // After trying both the top and the bottom chest there are still items remaining
        overflowItems.copyList(couldntHandle2.getList());
        return false;
    }

    // Return what could not be inserted
    private ItemStack insertItem(@Nonnull ItemStack s) {
        s = InventoryTools.insertItem(level, getBlockPos(), Direction.UP, s);
        if (!s.isEmpty()) {
            s = InventoryTools.insertItem(level, getBlockPos(), Direction.DOWN, s);
        }
        return s;
    }

    private static class TakeableItem {
        private final IItemHandler itemHandler;
        private final int slot;
        private final ItemStack peekStack;

        public static final TakeableItem EMPTY = new TakeableItem();

        private TakeableItem() {
            this.itemHandler = null;
            this.slot = -1;
            this.peekStack = ItemStack.EMPTY;
        }

        public TakeableItem(IItemHandler itemHandler, int slot) {
            Validate.inclusiveBetween(0, itemHandler.getSlots() - 1, slot);
            this.itemHandler = itemHandler;
            this.slot = slot;
            this.peekStack = itemHandler.extractItem(slot, 1, true);
        }

        public ItemStack peek() {
            return peekStack.copy();
        }

        public void take() {
            if (itemHandler != null) {
                itemHandler.extractItem(slot, 1, false);
            }
        }

        public ItemStack takeAndReplace(ItemStack replacement) {
            if (itemHandler != null) {
                itemHandler.extractItem(slot, 1, false);
                return itemHandler.insertItem(slot, replacement, false);
            }
            return replacement;
        }
    }

    /**
     * Create a way to let you consume a block out of an inventory. Returns a blockstate
     * from that inventory or else null if nothing could be found.
     * If the given blockstate parameter is null then a random block will be
     * returned. Otherwise the returned block has to match.
     */
    private TakeableItem createTakeableItem(Direction direction, World srcWorld, BlockPos srcPos, BlockState state) {
        TileEntity te = level.getBlockEntity(getBlockPos().relative(direction));
        if (te != null) {
            return te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, direction.getOpposite()).map(h -> {
                return findBlockTakeableItem(h, srcWorld, srcPos, state);
            }).orElse(TakeableItem.EMPTY);
        }
        return TakeableItem.EMPTY;
    }

    @Nonnull
    private FluidStack consumeLiquid(World srcWorld, BlockPos srcPos) {
        FluidStack b = consumeLiquid(Direction.UP, srcWorld, srcPos);
        if (b.isEmpty()) {
            b = consumeLiquid(Direction.DOWN, srcWorld, srcPos);
        }
        return b;
    }

    @Nonnull
    private FluidStack consumeLiquid(Direction direction, World srcWorld, BlockPos srcPos) {
        TileEntity te = level.getBlockEntity(getBlockPos().relative(direction));
        if (te != null) {
            LazyOptional<IFluidHandler> fluid = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, direction.getOpposite());
            if (!fluid.isPresent()) {
                fluid = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY);
            }
            if (fluid.isPresent()) {
                return fluid.map(h -> findAndConsumeLiquid(h, srcWorld, srcPos)).orElse(FluidStack.EMPTY);
            }
        }
        return FluidStack.EMPTY;
    }

    @Nonnull
    private FluidStack findAndConsumeLiquid(IFluidHandler tank, World srcWorld, BlockPos srcPos) {
        for (int i = 0; i < tank.getTanks(); i++) {
            FluidStack contents = tank.getFluidInTank(i);
            if (!contents.isEmpty()) {
                if (contents.getFluid() != null) {
                    if (contents.getAmount() >= 1000) {
                        FluidStack drained = tank.drain(new FluidStack(contents.getFluid(), 1000, contents.getTag()), IFluidHandler.FluidAction.EXECUTE);
//                        System.out.println("drained = " + drained);
                        return drained;
                    }
                }
            }
        }
        return FluidStack.EMPTY;
    }

    private TakeableItem createTakeableItem(World srcWorld, BlockPos srcPos, BlockState state) {
        TakeableItem b = createTakeableItem(Direction.UP, srcWorld, srcPos, state);
        if (b.peek().isEmpty()) {
            b = createTakeableItem(Direction.DOWN, srcWorld, srcPos, state);
        }
        return b;
    }

    public static BlockInformation getBlockInformation(PlayerEntity fakePlayer, World world, BlockPos pos, Block block, TileEntity tileEntity) {
        BlockState state = world.getBlockState(pos);
        if (isEmpty(state, block)) {
            return BlockInformation.FREE;
        }

        if (!allowedToBreakS(state, world, pos, fakePlayer)) {
            return BlockInformation.INVALID;
        }

        BlockInformation blockInformation = BlockInformation.getBlockInformation(block);
        if (tileEntity != null) {
            switch (BuilderConfiguration.teMode.get()) {
                case MOVE_FORBIDDEN:
                    return BlockInformation.INVALID;
                case MOVE_WHITELIST:
                    if (blockInformation == null || blockInformation.getBlockLevel() == SupportBlock.SupportStatus.STATUS_ERROR) {
                        return BlockInformation.INVALID;
                    }
                    break;
                case MOVE_BLACKLIST:
                    if (blockInformation != null && blockInformation.getBlockLevel() == SupportBlock.SupportStatus.STATUS_ERROR) {
                        return BlockInformation.INVALID;
                    }
                    break;
                case MOVE_ALLOWED:
                    break;
            }
        }
        if (blockInformation != null) {
            return blockInformation;
        }
        return BlockInformation.OK;
    }

    private SupportBlock.SupportStatus isMovable(PlayerEntity harvester, World world, BlockPos pos, Block block, TileEntity tileEntity) {
        return getBlockInformation(harvester, world, pos, block, tileEntity).getBlockLevel();
    }

    public static boolean isEmptyOrReplacable(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (state.getMaterial().isReplaceable()) {
            return true;
        }
        return isEmpty(state, block);
    }

    // True if this block can just be overwritten (i.e. are or support block)
    public static boolean isEmpty(BlockState state, Block block) {
        if (block == null) {
            return true;
        }
        if (state.getMaterial() == Material.AIR) {
            return true;
        }
        if (block == BuilderModule.SUPPORT.get()) {
            return true;
        }
        return false;
    }

    private void clearBlock(World world, BlockPos pos) {
        if (supportMode) {
            world.setBlock(pos, BuilderModule.SUPPORT.get().defaultBlockState(), 3);
        } else {
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private int oppositeRotate() {
        switch (rotate) {
            case 1:
                return 3;
            case 3:
                return 1;
        }
        return rotate;
    }

    private void copyBlock(World srcWorld, BlockPos srcPos, World destWorld, BlockPos destPos) {
        long rf = energyStorage.getEnergy();
        float factor = infusableHandler.map(IInfusable::getInfusedFactor).orElse(0.0f);
        int rfNeeded = (int) (BuilderConfiguration.builderRfPerOperation.get() * getDimensionCostFactor(srcWorld, destWorld) * (4.0f - factor) / 4.0f);
        if (rfNeeded > rf) {
            // Not enough energy.
            return;
        }

        if (isEmptyOrReplacable(destWorld, destPos)) {
            if (srcWorld.isEmptyBlock(srcPos)) {
                return;
            }
            BlockState srcState = srcWorld.getBlockState(srcPos);
            TakeableItem takeableItem = createTakeableItem(srcWorld, srcPos, srcState);
            ItemStack consumedStack = takeableItem.peek();
            if (consumedStack.isEmpty()) {
                return;
            }

            PlayerEntity fakePlayer = harvester.get();
            BlockState newState = BlockTools.placeStackAt(fakePlayer, consumedStack, destWorld, destPos, srcState);
            if (newState == null) {
                // This block can't be placed
                return;
            }
            destWorld.setBlock(destPos, newState, 3);  // placeBlockAt can reset the orientation. Restore it here

            if (!ItemStack.matches(consumedStack, takeableItem.peek())) { // Did we actually use up whatever we were holding?
                if (!consumedStack.isEmpty()) { // Are we holding something else that we should put back?
                    consumedStack = takeableItem.takeAndReplace(consumedStack); // First try to put our new item where we got what we placed
                    if (!consumedStack.isEmpty()) { // If that didn't work, then try to put it anywhere it will fit
                        consumedStack = insertItem(consumedStack);
                        if (!consumedStack.isEmpty()) { // If that still didn't work, then just drop whatever we're holding
                            level.addFreshEntity(new ItemEntity(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), consumedStack));
                        }
                    }
                } else {
                    takeableItem.take(); // If we aren't holding anything, then just consume what we placed
                }
            }

            if (!silent) {
                SoundType soundType = newState.getBlock().getSoundType(newState, destWorld, destPos, fakePlayer);
                playPlaceSoundSafe(soundType, destWorld, newState, destPos.getX(), destPos.getY(), destPos.getZ());
            }

            energyStorage.consumeEnergy(rfNeeded);
        }
    }

    private double getDimensionCostFactor(World world, World destWorld) {
        return (Objects.equals(destWorld.dimension(), world.dimension())) ? 1.0 : BuilderConfiguration.dimensionCostFactor.get();
    }

    private boolean consumeEntityEnergy(int rfNeeded, int rfNeededPlayer, Entity entity) {
        long rf = energyStorage.getEnergy();
        int rfn;
        if (entity instanceof PlayerEntity) {
            rfn = rfNeededPlayer;
        } else {
            rfn = rfNeeded;
        }
        if (rfn > rf) {
            // Not enough energy.
            return true;
        } else {
            energyStorage.consumeEnergy(rfn);
        }
        return false;
    }

    private void moveEntities(World world, int x, int y, int z, World destWorld, int destX, int destY, int destZ) {
        float factor = infusableHandler.map(IInfusable::getInfusedFactor).orElse(0.0f);
        int rfNeeded = (int) (BuilderConfiguration.builderRfPerEntity.get() * getDimensionCostFactor(world, destWorld) * (4.0f - factor) / 4.0f);
        int rfNeededPlayer = (int) (BuilderConfiguration.builderRfPerPlayer.get() * getDimensionCostFactor(world, destWorld) * (4.0f - factor) / 4.0f);

        // Check for entities.
        List<Entity> entities = world.getEntities(null, new AxisAlignedBB(x - .1, y - .1, z - .1, x + 1.1, y + 1.1, z + 1.1));
        for (Entity entity : entities) {

            if (consumeEntityEnergy(rfNeeded, rfNeededPlayer, entity)) {
                return;
            }

            double newX = destX + (entity.getX() - x);
            double newY = destY + (entity.getY() - y);
            double newZ = destZ + (entity.getZ() - z);

            teleportEntity(world, destWorld, entity, newX, newY, newZ);
        }
    }

    private void swapEntities(World world, int x, int y, int z, World destWorld, int destX, int destY, int destZ) {
        float factor = infusableHandler.map(IInfusable::getInfusedFactor).orElse(0.0f);
        int rfNeeded = (int) (BuilderConfiguration.builderRfPerEntity.get() * getDimensionCostFactor(world, destWorld) * (4.0f - factor) / 4.0f);
        int rfNeededPlayer = (int) (BuilderConfiguration.builderRfPerPlayer.get() * getDimensionCostFactor(world, destWorld) * (4.0f - factor) / 4.0f);

        // Check for entities.
        List<Entity> entitiesSrc = world.getEntities(null, new AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1));
        List<Entity> entitiesDst = destWorld.getEntities(null, new AxisAlignedBB(destX, destY, destZ, destX + 1, destY + 1, destZ + 1));
        for (Entity entity : entitiesSrc) {
            if (isEntityInBlock(x, y, z, entity)) {
                if (consumeEntityEnergy(rfNeeded, rfNeededPlayer, entity)) {
                    return;
                }

                double newX = destX + (entity.getX() - x);
                double newY = destY + (entity.getY() - y);
                double newZ = destZ + (entity.getZ() - z);
                teleportEntity(world, destWorld, entity, newX, newY, newZ);
            }
        }
        for (Entity entity : entitiesDst) {
            if (isEntityInBlock(destX, destY, destZ, entity)) {
                if (consumeEntityEnergy(rfNeeded, rfNeededPlayer, entity)) {
                    return;
                }

                double newX = x + (entity.getX() - destX);
                double newY = y + (entity.getY() - destY);
                double newZ = z + (entity.getZ() - destZ);
                teleportEntity(destWorld, world, entity, newX, newY, newZ);
            }
        }
    }

    private void teleportEntity(World world, World destWorld, Entity entity, double newX, double newY, double newZ) {
        // @todo 1.14 use api to check for allow teleportation
//        if (!TeleportationTools.allowTeleport(entity, world.getDimension().getType().getId(), entity.getPosition(), destWorld.getDimension().getType().getId(), new BlockPos(newX, newY, newZ))) {
//            return;
//        }
        TeleportationTools.teleportEntity(entity, destWorld, newX, newY, newZ, null);
    }


    private boolean isEntityInBlock(int x, int y, int z, Entity entity) {
        if (entity.getX() >= x && entity.getX() < x + 1 && entity.getY() >= y && entity.getY() < y + 1 && entity.getZ() >= z && entity.getZ() < z + 1) {
            return true;
        }
        return false;
    }

    private void moveBlock(World srcWorld, BlockPos srcPos, World destWorld, BlockPos destPos, int rotMode) {
        BlockState oldDestState = destWorld.getBlockState(destPos);
        Block oldDestBlock = oldDestState.getBlock();
        if (isEmpty(oldDestState, oldDestBlock)) {
            BlockState srcState = srcWorld.getBlockState(srcPos);
            Block srcBlock = srcState.getBlock();
            if (isEmpty(srcState, srcBlock)) {
                return;
            }
            TileEntity srcTileEntity = srcWorld.getBlockEntity(srcPos);
            BlockInformation srcInformation = getBlockInformation(harvester.get(), srcWorld, srcPos, srcBlock, srcTileEntity);
            if (srcInformation.getBlockLevel() == SupportBlock.SupportStatus.STATUS_ERROR) {
                return;
            }

            long rf = energyStorage.getEnergy();
            float factor = infusableHandler.map(IInfusable::getInfusedFactor).orElse(0.0f);
            int rfNeeded = (int) (BuilderConfiguration.builderRfPerOperation.get() * getDimensionCostFactor(srcWorld, destWorld) * srcInformation.getCostFactor() * (4.0f - factor) / 4.0f);
            if (rfNeeded > rf) {
                // Not enough energy.
                return;
            } else {
                energyStorage.consumeEnergy(rfNeeded);
            }

            CompoundNBT tc = null;
            if (srcTileEntity != null) {
                tc = new CompoundNBT();
                srcTileEntity.save(tc);
                srcWorld.removeBlockEntity(srcPos);
            }
            clearBlock(srcWorld, srcPos);

            destWorld.setBlock(destPos, srcState, 3);
            if (srcTileEntity != null && tc != null) {
                setTileEntityNBT(destWorld, tc, destPos, srcState);
            }
            if (!silent) {
                SoundType srcSoundType = srcBlock.getSoundType(srcState, srcWorld, srcPos, null);
                playBreakSoundSafe(srcSoundType, srcWorld, srcState, srcPos.getX(), srcPos.getY(), srcPos.getZ());
                SoundType dstSoundtype = srcBlock.getSoundType(srcState, destWorld, destPos, null);
                playPlaceSoundSafe(dstSoundtype, destWorld, srcState, destPos.getX(), destPos.getY(), destPos.getZ());
            }
        }
    }

    private void setTileEntityNBT(World destWorld, CompoundNBT tc, BlockPos destpos, BlockState newDestState) {
        tc.putInt("x", destpos.getX());
        tc.putInt("y", destpos.getY());
        tc.putInt("z", destpos.getZ());
        TileEntity tileEntity = TileEntity.loadStatic(newDestState, tc);
        if (tileEntity != null) {
            destWorld.getChunk(destpos).setBlockEntity(destpos, tileEntity);
            tileEntity.setChanged();
            destWorld.sendBlockUpdated(destpos, newDestState, newDestState, 3);
        }
    }

    private void swapBlock(World srcWorld, BlockPos srcPos, World destWorld, BlockPos dstPos) {
        BlockState oldSrcState = srcWorld.getBlockState(srcPos);
        Block srcBlock = oldSrcState.getBlock();
        TileEntity srcTileEntity = srcWorld.getBlockEntity(srcPos);

        BlockState oldDstState = destWorld.getBlockState(dstPos);
        Block dstBlock = oldDstState.getBlock();
        TileEntity dstTileEntity = destWorld.getBlockEntity(dstPos);

        if (isEmpty(oldSrcState, srcBlock) && isEmpty(oldDstState, dstBlock)) {
            return;
        }

        BlockInformation srcInformation = getBlockInformation(harvester.get(), srcWorld, srcPos, srcBlock, srcTileEntity);
        if (srcInformation.getBlockLevel() == SupportBlock.SupportStatus.STATUS_ERROR) {
            return;
        }

        BlockInformation dstInformation = getBlockInformation(harvester.get(), destWorld, dstPos, dstBlock, dstTileEntity);
        if (dstInformation.getBlockLevel() == SupportBlock.SupportStatus.STATUS_ERROR) {
            return;
        }

        long rf = energyStorage.getEnergy();
        float factor = infusableHandler.map(IInfusable::getInfusedFactor).orElse(0.0f);
        int rfNeeded = (int) (BuilderConfiguration.builderRfPerOperation.get() * getDimensionCostFactor(srcWorld, destWorld) * srcInformation.getCostFactor() * (4.0f - factor) / 4.0f);
        rfNeeded += (int) (BuilderConfiguration.builderRfPerOperation.get() * getDimensionCostFactor(srcWorld, destWorld) * dstInformation.getCostFactor() * (4.0f - factor) / 4.0f);
        if (rfNeeded > rf) {
            // Not enough energy.
            return;
        } else {
            energyStorage.consumeEnergy(rfNeeded);
        }

        srcWorld.removeBlockEntity(srcPos);
        srcWorld.setBlockAndUpdate(srcPos, Blocks.AIR.defaultBlockState());
        destWorld.removeBlockEntity(dstPos);
        destWorld.setBlockAndUpdate(dstPos, Blocks.AIR.defaultBlockState());

        BlockState newDstState = oldSrcState;
        destWorld.setBlock(dstPos, newDstState, 3);
//        destWorld.setBlockMetadataWithNotify(destX, destY, destZ, srcMeta, 3);
        if (srcTileEntity != null) {
            srcTileEntity.clearRemoved();
            destWorld.setBlockEntity(dstPos, srcTileEntity);
            srcTileEntity.setChanged();
            destWorld.sendBlockUpdated(dstPos, newDstState, newDstState, 3);
        }

        BlockState newSrcState = oldDstState;
        srcWorld.setBlock(srcPos, newSrcState, 3);
//        world.setBlockMetadataWithNotify(x, y, z, dstMeta, 3);
        if (dstTileEntity != null) {
            dstTileEntity.clearRemoved();
            srcWorld.setBlockEntity(srcPos, dstTileEntity);
            dstTileEntity.setChanged();
            srcWorld.sendBlockUpdated(srcPos, newSrcState, newSrcState, 3);
        }

        if (!silent) {
            if (!isEmpty(oldSrcState, srcBlock)) {
                SoundType srcSoundType = srcBlock.getSoundType(oldSrcState, srcWorld, srcPos, null);
                playBreakSoundSafe(srcSoundType, srcWorld, oldSrcState, srcPos.getX(), srcPos.getY(), srcPos.getZ());
                SoundType dstSoundType = srcBlock.getSoundType(oldSrcState, destWorld, dstPos, null);
                playPlaceSoundSafe(dstSoundType, destWorld, oldSrcState, dstPos.getX(), dstPos.getY(), dstPos.getZ());
            }
            if (!isEmpty(oldDstState, dstBlock)) {
                SoundType srcSoundType = dstBlock.getSoundType(oldDstState, destWorld, dstPos, null);
                playBreakSoundSafe(srcSoundType, destWorld, oldDstState, dstPos.getX(), dstPos.getY(), dstPos.getZ());
                SoundType dstSoundType = dstBlock.getSoundType(oldDstState, srcWorld, srcPos, null);
                playPlaceSoundSafe(dstSoundType, srcWorld, oldDstState, srcPos.getX(), srcPos.getY(), srcPos.getZ());
            }
        }
    }

    private BlockPos sourceToDest(BlockPos source) {
        return rotate(source).offset(projDx, projDy, projDz);
    }

    private BlockPos rotate(BlockPos c) {
        switch (rotate) {
            case 0:
                return c;
            case 1:
                return new BlockPos(-c.getZ(), c.getY(), c.getX());
            case 2:
                return new BlockPos(-c.getX(), c.getY(), -c.getZ());
            case 3:
                return new BlockPos(c.getZ(), c.getY(), -c.getX());
        }
        return c;
    }

    private void sourceToDest(BlockPos source, BlockPos.Mutable dest) {
        rotate(source, dest);
        dest.set(dest.getX() + projDx, dest.getY() + projDy, dest.getZ() + projDz);
    }


    private void rotate(BlockPos c, BlockPos.Mutable dest) {
        switch (rotate) {
            case 0:
                dest.set(c);
                break;
            case 1:
                dest.set(-c.getZ(), c.getY(), c.getX());
                break;
            case 2:
                dest.set(-c.getX(), c.getY(), -c.getZ());
                break;
            case 3:
                dest.set(c.getZ(), c.getY(), -c.getX());
                break;
        }
    }

    private void restartScan() {
        lastError = null;
        chunkUnload();
        if (loopMode || (isMachineEnabled() && scan == null)) {
            if (getCardType() == ShapeCardType.CARD_SPACE) {
                calculateBox();
                scan = minBox;
            } else if (getCardType() != ShapeCardType.CARD_UNKNOWN) {
                calculateBoxShaped();
                // We start at the top for a quarry or shape building
                scan = new BlockPos(minBox.getX(), maxBox.getY(), minBox.getZ());
            }
            cachedBlocks = null;
            cachedChunk = null;
            cachedVoidableBlocks.clear();
        } else {
            scan = null;
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        chunkUnload();
    }

    private void chunkUnload() {
        if (forcedChunk != null) {
            if (getOwnerUUID() != null) {
                ForgeChunkManager.forceChunk((ServerWorld) level, RFToolsBuilder.MODID, getOwnerUUID(), forcedChunk.x, forcedChunk.z, false, false);
            }
            forcedChunk = null;
        }
    }

    private boolean chunkLoad(int x, int z) {
        int cx = x >> 4;
        int cz = z >> 4;

        if (LevelTools.isLoaded(level, new BlockPos(x, 0, z))) {
            return true;
        }

        if (BuilderConfiguration.quarryChunkloads.get()) {
            ChunkPos pair = new ChunkPos(cx, cz);
            if (pair.equals(forcedChunk)) {
                return true;
            }
            if (forcedChunk != null) {
                if (getOwnerUUID() != null) {
                    ForgeChunkManager.forceChunk((ServerWorld) level, RFToolsBuilder.MODID, getOwnerUUID(), forcedChunk.x, forcedChunk.z, false, false);
                }
            }
            forcedChunk = pair;
            if (getOwnerUUID() != null) {
                ForgeChunkManager.forceChunk((ServerWorld) level, RFToolsBuilder.MODID, getOwnerUUID(), forcedChunk.x, forcedChunk.z, true, false);
            }
            return true;
        }
        // Chunk is not loaded and we don't do chunk loading so we cannot proceed.
        return false;
    }


    public static void setScanLocationClient(BlockPos tePos, BlockPos scanPos) {
        scanLocClient.put(tePos, Pair.of(System.currentTimeMillis(), scanPos));
    }

    public static Map<BlockPos, Pair<Long, BlockPos>> getScanLocClient() {
        if (scanLocClient.isEmpty()) {
            return scanLocClient;
        }
        Map<BlockPos, Pair<Long, BlockPos>> scans = new HashMap<>();
        long time = System.currentTimeMillis();
        for (Map.Entry<BlockPos, Pair<Long, BlockPos>> entry : scanLocClient.entrySet()) {
            if (entry.getValue().getKey() + 10000 > time) {
                scans.put(entry.getKey(), entry.getValue());
            }
        }
        scanLocClient = scans;
        return scanLocClient;
    }

    private void nextLocation() {
        if (scan != null) {
            int x = scan.getX();
            int y = scan.getY();
            int z = scan.getZ();

            if (getCardType() == ShapeCardType.CARD_SPACE) {
                nextLocationNormal(x, y, z);
            } else {
                nextLocationQuarry(x, y, z);
            }
        }
    }

    private void nextLocationQuarry(int x, int y, int z) {
        if (x >= maxBox.getX() || ((x + 1) % 16 == 0)) {
            if (z >= maxBox.getZ() || ((z + 1) % 16 == 0)) {
                if (y <= minBox.getY()) {
                    if (x < maxBox.getX()) {
                        x++;
                        z = (z >> 4) << 4;
                        y = maxBox.getY();
                        scan = new BlockPos(x, y, z);
                    } else if (z < maxBox.getZ()) {
                        x = minBox.getX();
                        z++;
                        y = maxBox.getY();
                        scan = new BlockPos(x, y, z);
                    } else {
                        restartScan();
                    }
                } else {
                    scan = new BlockPos((x >> 4) << 4, y - 1, (z >> 4) << 4);
                }
            } else {
                scan = new BlockPos((x >> 4) << 4, y, z + 1);
            }
        } else {
            scan = new BlockPos(x + 1, y, z);
        }
    }

    private void nextLocationNormal(int x, int y, int z) {
        if (x >= maxBox.getX()) {
            if (z >= maxBox.getZ()) {
                if (y >= maxBox.getY()) {
                    if (mode != MODE_SWAP || isShapeCard()) {
                        restartScan();
                    } else {
                        // We don't restart in swap mode.
                        scan = null;
                    }
                } else {
                    scan = new BlockPos(minBox.getX(), y + 1, minBox.getZ());
                }
            } else {
                scan = new BlockPos(minBox.getX(), y, z + 1);
            }
        } else {
            scan = new BlockPos(x + 1, y, z);
        }
    }

    private void refreshSettings() {
        clearSupportBlocks();
        cachedBlocks = null;
        cachedChunk = null;
        cachedVoidableBlocks.clear();
        boxValid = false;
        scan = null;
        cardType = ShapeCardType.CARD_UNKNOWN;
    }

    @Override
    public void read(CompoundNBT tagCompound) {
        super.read(tagCompound);
        if (tagCompound.contains("overflowItems")) {
            ListNBT overflowItemsNbt = tagCompound.getList("overflowItems", Constants.NBT.TAG_COMPOUND);
            overflowItems.clear();
            for (INBT overflowNbt : overflowItemsNbt) {
                overflowItems.add(ItemStack.of((CompoundNBT) overflowNbt));
            }
        }
    }

    @Override
    protected void readInfo(CompoundNBT tagCompound) {
        super.readInfo(tagCompound);
        CompoundNBT info = tagCompound.getCompound("Info");

        // Workaround to get the redstone mode for old builders to default to 'on'
        if (!info.contains("rsMode")) {
            rsMode = RedstoneMode.REDSTONE_ONREQUIRED;
        }

        if (info.contains("lastError")) {
            lastError = info.getString("lastError");
        } else {
            lastError = null;
        }
        mode = info.getInt("mode");
        anchor = info.getInt("anchor");
        rotate = info.getInt("rotate");
        silent = info.getBoolean("silent");
        supportMode = info.getBoolean("support");
        entityMode = info.getBoolean("entityMode");
        loopMode = info.getBoolean("loopMode");
        if (info.contains("waitMode")) {
            waitMode = info.getBoolean("waitMode");
        } else {
            waitMode = true;
        }
        hilightMode = info.getBoolean("hilightMode");
        scan = BlockPosTools.read(info, "scan");
        minBox = BlockPosTools.read(info, "minBox");
        maxBox = BlockPosTools.read(info, "maxBox");
    }

    @Override
    public CompoundNBT save(CompoundNBT tagCompound) {
        super.save(tagCompound);
        if (!overflowItems.isEmpty()) {
            ListNBT overflowItemsNbt = new ListNBT();
            for (ItemStack overflow : overflowItems.getList()) {
                overflowItemsNbt.add(overflow.save(new CompoundNBT()));
            }
            tagCompound.put("overflowItems", overflowItemsNbt);
        }
        if (lastError != null) {
            tagCompound.putString("lastError", lastError);
        }
        tagCompound.putInt("mode", mode);
        tagCompound.putInt("anchor", anchor);
        tagCompound.putInt("rotate", rotate);
        tagCompound.putBoolean("silent", silent);
        tagCompound.putBoolean("support", supportMode);
        tagCompound.putBoolean("entityMode", entityMode);
        tagCompound.putBoolean("loopMode", loopMode);
        tagCompound.putBoolean("waitMode", waitMode);
        tagCompound.putBoolean("hilightMode", hilightMode);
        BlockPosTools.write(tagCompound, "scan", scan);
        BlockPosTools.write(tagCompound, "minBox", minBox);
        BlockPosTools.write(tagCompound, "maxBox", maxBox);
        return tagCompound;
    }

    @Override
    protected void writeInfo(CompoundNBT tagCompound) {
        super.writeInfo(tagCompound);
        CompoundNBT infoTag = getOrCreateInfo(tagCompound);
        if (lastError != null) {
            infoTag.putString("lastError", lastError);
        }
        infoTag.putInt("mode", mode);
        infoTag.putInt("anchor", anchor);
        infoTag.putInt("rotate", rotate);
        infoTag.putBoolean("silent", silent);
        infoTag.putBoolean("support", supportMode);
        infoTag.putBoolean("entityMode", entityMode);
        infoTag.putBoolean("loopMode", loopMode);
        infoTag.putBoolean("waitMode", waitMode);
        infoTag.putBoolean("hilightMode", hilightMode);
        BlockPosTools.write(infoTag, "scan", scan);
        BlockPosTools.write(infoTag, "minBox", minBox);
        BlockPosTools.write(infoTag, "maxBox", maxBox);
    }

    public static int getCurrentLevelClientSide() {
        return currentLevel;
    }

    public int getCurrentLevel() {
        return scan == null ? -1 : scan.getY();
    }

    @Override
    public boolean execute(PlayerEntity playerMP, String command, TypedMap params) {
        boolean rc = super.execute(playerMP, command, params);
        if (rc) {
            return true;
        }
        if (CMD_SETROTATE.equals(command)) {
            setRotate(Integer.parseInt(params.get(ChoiceLabel.PARAM_CHOICE)) / 90);
            return true;
        } else if (CMD_SETANCHOR.equals(command)) {
            setAnchor(params.get(PARAM_ANCHOR_INDEX));
            return true;
        } else if (CMD_SETMODE.equals(command)) {
            setMode(params.get(ChoiceLabel.PARAM_CHOICE_IDX));
            return true;
        }
        return false;
    }

    @Nonnull
    @Override
    public <T> List<T> executeWithResultList(String command, TypedMap args, Type<T> type) {
        List<T> rc = super.executeWithResultList(command, args, type);
        if (!rc.isEmpty()) {
            return rc;
        }
        if (PacketGetHudLog.CMD_GETHUDLOG.equals(command)) {
            return type.convert(getHudLog());
        }
        return rc;
    }

    @Override
    public <T> boolean receiveListFromServer(String command, List<T> list, Type<T> type) {
        boolean rc = super.receiveListFromServer(command, list, type);
        if (rc) {
            return true;
        }
        if (PacketGetHudLog.CLIENTCMD_GETHUDLOG.equals(command)) {
            clientHudLog = Type.STRING.convert(list);
            return true;
        }
        return false;
    }


    @Override
    public void onReplaced(World world, BlockPos pos, BlockState state, BlockState newstate) {
        if (state.getBlock() == newstate.getBlock()) {
            return;
        }

        if (hasSupportMode()) {
            clearSupportBlocks();
        }
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(worldPosition, worldPosition.offset(1, 2, 1));
    }

    @Override
    public void rotateBlock(Rotation axis) {
        super.rotateBlock(axis);
        if (!level.isClientSide) {
            if (hasSupportMode()) {
                clearSupportBlocks();
                resetBox();
            }
        }
    }

    // @todo 1.14
//    @Override
//    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, BlockState metadata, int fortune) {
//        super.getDrops(drops, world, pos, metadata, fortune);
//        List<ItemStack> overflowItems = getOverflowItems();
//        if(overflowItems != null) {
//            drops.addAll(overflowItems);
//        }
//    }

    private NoDirectionItemHander createItemHandler() {
        return new NoDirectionItemHander(BuilderTileEntity.this, CONTAINER_FACTORY.get()) {

            // @todo all methods below could be avoided with a proper onUpdate method
            @Nonnull
            @Override
            public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
                checkShapeCard(slot, stack);
                return super.insertItem(slot, stack, simulate);
            }

            @Override
            public void setInventorySlotContents(int stackLimit, int index, ItemStack stack) {
                checkShapeCard(index, stack);
                super.setInventorySlotContents(stackLimit, index, stack);
            }

            @Nonnull
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                checkShapeCard(slot, ItemStack.EMPTY);
                return super.extractItem(slot, amount, simulate);
            }

            @Override
            public ItemStack decrStackSize(int index, int amount) {
                checkShapeCard(index, ItemStack.EMPTY);
                return super.decrStackSize(index, amount);
            }

            @Override
            public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
                checkShapeCard(slot, stack);
                super.setStackInSlot(slot, stack);
            }

            // @todo would be better if onUpdate had an ItemStack parameter
            @Override
            protected void onUpdate(int index) {
                super.onUpdate(index);
                if (index == SLOT_FILTER) {
                    filterCache.clear();
                }
            }

            private void checkShapeCard(int index, ItemStack newStack) {
                ItemStack stack = getStackInSlot(index);
                if (index == SLOT_TAB && ((stack.isEmpty()
                        && !newStack.isEmpty())
                        || (!stack.isEmpty() && newStack.isEmpty()))) {
                    // Restart if we go from having a stack to not having stack or the other way around.
                    refreshSettings();
                }
            }
        };
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction facing) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return itemHandler.cast();
        }
        if (cap == CapabilityEnergy.ENERGY) {
            return energyHandler.cast();
        }
        if (cap == CapabilityContainerProvider.CONTAINER_PROVIDER_CAPABILITY) {
            return screenHandler.cast();
        }
        if (cap == CapabilityInfusable.INFUSABLE_CAPABILITY) {
            return infusableHandler.cast();
        }
        if (cap == CapabilityModuleSupport.MODULE_CAPABILITY) {
            return moduleSupportHandler.cast();
        }
        return super.getCapability(cap, facing);
    }
}
