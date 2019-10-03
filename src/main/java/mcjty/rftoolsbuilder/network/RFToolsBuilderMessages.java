package mcjty.rftoolsbuilder.network;

import mcjty.lib.network.PacketHandler;
import mcjty.lib.network.PacketSendClientCommand;
import mcjty.lib.network.PacketSendServerCommand;
import mcjty.lib.typed.TypedMap;
import mcjty.rftoolsbuilder.RFToolsBuilder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import javax.annotation.Nonnull;

public class RFToolsBuilderMessages {
    public static SimpleChannel INSTANCE;

    private static int id() {
        return PacketHandler.nextPacketID();
    }

    public static void registerMessages(String name) {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(RFToolsBuilder.MODID, name))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        // Server side
//        net.registerMessage(id(), PacketGetTransmitters.class, PacketGetTransmitters::toBytes, PacketGetTransmitters::new, PacketGetTransmitters::handle);

        // Client side
//        net.registerMessage(id(), PacketAllReceiversReady.class, PacketAllReceiversReady::toBytes, PacketAllReceiversReady::new, PacketAllReceiversReady::handle);

        PacketHandler.registerStandardMessages(net);
    }

    public static void sendToServer(String command, @Nonnull TypedMap.Builder argumentBuilder) {
        INSTANCE.sendToServer(new PacketSendServerCommand(RFToolsBuilder.MODID, command, argumentBuilder.build()));
    }

    public static void sendToServer(String command) {
        INSTANCE.sendToServer(new PacketSendServerCommand(RFToolsBuilder.MODID, command, TypedMap.EMPTY));
    }

    public static void sendToClient(PlayerEntity player, String command, @Nonnull TypedMap.Builder argumentBuilder) {
        INSTANCE.sendTo(new PacketSendClientCommand(RFToolsBuilder.MODID, command, argumentBuilder.build()), ((ServerPlayerEntity) player).connection.netManager, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void sendToClient(PlayerEntity player, String command) {
        INSTANCE.sendTo(new PacketSendClientCommand(RFToolsBuilder.MODID, command, TypedMap.EMPTY), ((ServerPlayerEntity) player).connection.netManager, NetworkDirection.PLAY_TO_CLIENT);
    }
}
