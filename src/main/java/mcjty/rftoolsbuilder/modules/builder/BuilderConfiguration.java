package mcjty.rftoolsbuilder.modules.builder;

import com.google.common.collect.Lists;
import mcjty.lib.varia.Tools;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class BuilderConfiguration {
    public static final String CATEGORY_BUILDER = "builder";

    public static ModConfigSpec.IntValue BUILDER_MAXENERGY;// = 1000000;
    public static ModConfigSpec.IntValue BUILDER_RECEIVEPERTICK;// = 20000;

    public static ModConfigSpec.IntValue builderRfPerOperation;// = 500;
    public static ModConfigSpec.IntValue builderRfPerLiquid;// = 300;
    public static ModConfigSpec.IntValue builderRfPerQuarry;// = 300;
    public static ModConfigSpec.IntValue builderRfPerSkipped;// = 50;
    public static ModConfigSpec.IntValue builderRfPerEntity;// = 5000;
    public static ModConfigSpec.IntValue builderRfPerPlayer;// = 40000;
    public static ModConfigSpec.DoubleValue dimensionCostFactor;

    public static ModConfigSpec.ConfigValue<BuilderTileEntityMode> teMode;

    private static String[] blackWhiteListedBlocksAr = new String[] {
    };
    public static ModConfigSpec.ConfigValue<List<? extends String>> blackWhiteListedBlocks;

    public static ModConfigSpec.BooleanValue showProgressHud;

    public static ModConfigSpec.IntValue maxSpaceChamberDimension;

    public static ModConfigSpec.DoubleValue voidShapeCardFactor;
    public static ModConfigSpec.DoubleValue silkquarryShapeCardFactor;
    public static ModConfigSpec.DoubleValue fortunequarryShapeCardFactor;

    public static ModConfigSpec.ConfigValue<String> quarryReplace;
    private static BlockState quarryReplaceBlock = null;

    public static ModConfigSpec.BooleanValue quarryChunkloads;
    public static ModConfigSpec.BooleanValue shapeCardAllowed;
    public static ModConfigSpec.BooleanValue quarryAllowed;
    public static ModConfigSpec.BooleanValue clearingQuarryAllowed;

    public static ModConfigSpec.IntValue quarryBaseSpeed;
    public static ModConfigSpec.IntValue quarryInfusionSpeedFactor;
    public static ModConfigSpec.BooleanValue quarryTileEntities;

    public static ModConfigSpec.IntValue maxBuilderOffset;
    public static ModConfigSpec.IntValue maxBuilderDimension;

    public static ModConfigSpec.BooleanValue oldSphereCylinderShape;

    public static ModConfigSpec.IntValue collectTimer;
    public static ModConfigSpec.IntValue collectRFPerItem;
    public static ModConfigSpec.DoubleValue collectRFPerXP;
    public static ModConfigSpec.DoubleValue collectRFPerTickPerArea;

    public static void init(ModConfigSpec.Builder SERVER_BUILDER, ModConfigSpec.Builder CLIENT_BUILDER) {
        SERVER_BUILDER.comment("Settings for the builder").push(CATEGORY_BUILDER);
        CLIENT_BUILDER.comment("Settings for the builder").push(CATEGORY_BUILDER);

        BUILDER_MAXENERGY = SERVER_BUILDER
                .comment("Maximum RF storage that the builder can hold")
                .defineInRange("builderMaxRF", 1000000, 0, Integer.MAX_VALUE);
        BUILDER_RECEIVEPERTICK = SERVER_BUILDER
                .comment("RF per tick that the builder can receive")
                .defineInRange("builderRFPerTick", 20000, 0, Integer.MAX_VALUE);
        builderRfPerOperation = SERVER_BUILDER
                .comment("RF per block operation for the builder when used to build")
                .defineInRange("builderRfPerOperation", 500, 0, Integer.MAX_VALUE);
        builderRfPerLiquid = SERVER_BUILDER
                .comment("Base RF per block operation for the builder when used as a pump")
                .defineInRange("builderRfPerLiquid", 300, 0, Integer.MAX_VALUE);
        builderRfPerQuarry = SERVER_BUILDER
                .comment("Base RF per block operation for the builder when used as a quarry or voider (actual cost depends on hardness of block)")
                .defineInRange("builderRfPerQuarry", 300, 0, Integer.MAX_VALUE);
        builderRfPerSkipped = SERVER_BUILDER
                .comment("RF per block that is skipped (used when a filter is added to the builder)")
                .defineInRange("builderRfPerSkipped", 50, 0, Integer.MAX_VALUE);
        builderRfPerEntity = SERVER_BUILDER
                .comment("RF per entity move operation for the builder")
                .defineInRange("builderRfPerEntity", 5000, 0, Integer.MAX_VALUE);
        builderRfPerPlayer = SERVER_BUILDER
                .comment("RF per player move operation for the builder")
                .defineInRange("builderRfPerPlayer", 40000, 0, Integer.MAX_VALUE);
        teMode = SERVER_BUILDER
                .comment("Can Tile Entities be moved? 'forbidden' means never, 'whitelist' means only whitelisted, 'blacklist' means all except blacklisted, 'allowed' means all")
                .defineEnum("tileEntityMode", BuilderTileEntityMode.MOVE_BLACKLIST, BuilderTileEntityMode.values());
        blackWhiteListedBlocks = SERVER_BUILDER
                .comment("This is a list of blocks that are either blacklisted or whitelisted from the Builder when it tries to move tile entities (format <id>=<cost>)")
                .defineList("blackWhiteListedBlocks", Lists.newArrayList(blackWhiteListedBlocksAr), s -> s instanceof String);
        maxSpaceChamberDimension = SERVER_BUILDER
                .comment("Maximum dimension for the space chamber")
                .defineInRange("maxSpaceChamberDimension", 128, 0, 100000);

        collectTimer = SERVER_BUILDER
                .comment("How many ticks we wait before collecting again (with the builder 'collect items' mode)")
                .defineInRange("collectTimer", 10, 0, Integer.MAX_VALUE);
        collectRFPerItem = SERVER_BUILDER
                .comment("The cost of collecting an item (builder 'collect items' mode))")
                .defineInRange("collectRFPerItem", 20, 0, Integer.MAX_VALUE);

        dimensionCostFactor = SERVER_BUILDER
                .comment("How much more expensive a move accross dimensions is")
                .defineInRange("dimensionCostFactor", 5.0, 0, 1000000.0);

        collectRFPerXP = SERVER_BUILDER
                .comment("The cost of collecting 1 XP level (builder 'collect items' mode))")
                .defineInRange("collectRFPerXP", 2.0, 0, 1000000.0);
        collectRFPerTickPerArea = SERVER_BUILDER
                .comment("The RF/t per area to keep checking for items in a given area (builder 'collect items' mode))")
                .defineInRange("collectRFPerTickPerArea", 0.5, 0, 1000000.0);

        voidShapeCardFactor = SERVER_BUILDER
                .comment("The RF per operation of the builder is multiplied with this factor when using the void shape card")
                .defineInRange("voidShapeCardFactor", 0.5, 0, 1000000.0);
        silkquarryShapeCardFactor = SERVER_BUILDER
                .comment("The RF per operation of the builder is multiplied with this factor when using the silk quarry shape card")
                .defineInRange("silkquarryShapeCardFactor", 3.0, 0, 1000000.0);
        fortunequarryShapeCardFactor = SERVER_BUILDER
                .comment("The RF per operation of the builder is multiplied with this factor when using the fortune quarry shape card")
                .defineInRange("fortunequarryShapeCardFactor", 2, 0, 1000000.0);

        quarryReplace = SERVER_BUILDER
                .comment("Use this block for the builder to replace with")
                .define("quarryReplace", "minecraft:dirt");
        quarryTileEntities = SERVER_BUILDER
                .comment("If true the quarry will also quarry tile entities. Otherwise it just ignores them")
                .define("quarryTileEntities", true);
        quarryChunkloads = SERVER_BUILDER
                .comment("If true the quarry will chunkload a chunk at a time. If false the quarry will stop if a chunk is not loaded")
                .define("quarryChunkloads", true);
        shapeCardAllowed = SERVER_BUILDER
                .comment("If true we allow shape cards to be crafted. Note that in order to use the quarry system you must also enable this")
                .define("shapeCardAllowed", true);
        quarryAllowed = SERVER_BUILDER
                .comment("If true we allow quarry cards to be crafted")
                .define("quarryAllowed", true);
        clearingQuarryAllowed = SERVER_BUILDER
                .comment("If true we allow the clearing quarry cards to be crafted (these can be heavier on the server)")
                .define("clearingQuarryAllowed", true);

        quarryBaseSpeed = SERVER_BUILDER
                .comment("The base speed (number of blocks per tick) of the quarry")
                .defineInRange("quarryBaseSpeed", 8, 0, Integer.MAX_VALUE);
        quarryInfusionSpeedFactor = SERVER_BUILDER
                .comment("Multiply the infusion factor with this value and add that to the quarry base speed")
                .defineInRange("quarryInfusionSpeedFactor", 20, 0, Integer.MAX_VALUE);

        maxBuilderOffset = SERVER_BUILDER
                .comment("Maximum offset of the shape when a shape card is used in the builder")
                .defineInRange("maxBuilderOffset", 260, 0, Integer.MAX_VALUE);
        maxBuilderDimension = SERVER_BUILDER
                .comment("Maximum dimension of the shape when a shape card is used in the builder")
                .defineInRange("maxBuilderDimension", 512, 0, Integer.MAX_VALUE);

        oldSphereCylinderShape = SERVER_BUILDER
                .comment("If true we go back to the old (wrong) sphere/cylinder calculation for the builder/shield")
                .define("oldSphereCylinderShape", false);
        showProgressHud = CLIENT_BUILDER
                .comment("If true a holo hud with current progress is shown above the builder")
                .define("showProgressHud", true);

        SERVER_BUILDER.pop();
        CLIENT_BUILDER.pop();
    }

    public static BlockState getQuarryReplace() {
        if (quarryReplaceBlock == null) {
            int index = quarryReplace.get().indexOf(' ');
            if(index == -1) {
                quarryReplaceBlock = Tools.getBlock(new ResourceLocation(quarryReplace.get())).defaultBlockState();
            } else {
                // @todo 1.14
//                try {
//                    quarryReplaceBlock = CommandBase.convertArgToBlockState(ForgeRegistries.BLOCKS.getValue(new ResourceLocation(quarryReplace.get().substring(0, index))), quarryReplace.get().substring(index + 1));
//                } catch (NumberInvalidException | InvalidBlockStateException e) {
//                    Logging.logError("Invalid builder quarry replace block: " + quarryReplace, e);
//                }
            }
            if (quarryReplaceBlock == null) {
                quarryReplaceBlock = Blocks.DIRT.defaultBlockState();
            }
        }
        return quarryReplaceBlock;
    }
}
