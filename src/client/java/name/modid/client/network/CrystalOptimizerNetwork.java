package name.modid.client.network;

import name.modid.client.CrystalOptimizerClient;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public class CrystalOptimizerNetwork {

    public void register() {
        PayloadTypeRegistry.serverboundPlay().register(JoinPayload.TYPE, JoinPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OptOutPayload.TYPE, OptOutPayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(OptOutPayload.TYPE, (payload, context) -> {
            Minecraft minecraft = Minecraft.getInstance();
            context.client().execute(() -> {
                switch (payload.action()) {
                    case 0 -> {
                        CrystalOptimizerClient.setServerDisabled(true);
                        if (payload.message() != null && !payload.message().isEmpty() && context.player() != null) {
                            context.player().sendSystemMessage(Component.literal(payload.message()));
                        }
                    }
                    case 1 -> {
                        var connection = minecraft.getConnection();
                        if (context.player() != null && connection.getConnection() != null) {
                            Component reason = payload.message() != null && !payload.message().isEmpty()
                                    ? Component.literal(payload.message())
                                    : Component.literal("Disconnected by server");
                            connection.getConnection().disconnect(reason);
                        }
                    }
                }
            });
        });
    }

    public record JoinPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<JoinPayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("crystaloptimizer", "join"));

        public static final StreamCodec<RegistryFriendlyByteBuf, JoinPayload> CODEC =
                StreamCodec.of(
                        (buffer, value) -> { },
                        buffer -> new JoinPayload()
                );

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record OptOutPayload(int action, String message) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<OptOutPayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("crystaloptimizer", "opt_out"));

        public static final StreamCodec<RegistryFriendlyByteBuf, OptOutPayload> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT,
                        OptOutPayload::action,
                        ByteBufCodecs.STRING_UTF8,
                        OptOutPayload::message,
                        OptOutPayload::new
                );

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}