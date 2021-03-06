package sk.dipo.moneymod.network.packet.server;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import sk.dipo.moneymod.capability.capability.ICreditCardInfo;
import sk.dipo.moneymod.network.packet.client.AtmCardSignedMsg;
import sk.dipo.moneymod.world.AccountWorldSavedData;
import sk.dipo.moneymod.capability.provider.CreditCardProvider;
import sk.dipo.moneymod.network.ModPacketHandler;

import java.util.Objects;
import java.util.function.Supplier;

public class AtmInitSessionMsg {

    private final Hand hand;

    public AtmInitSessionMsg(Hand hand) {
        this.hand = hand;
    }

    public void encode(PacketBuffer buffer) {
        buffer.writeEnumValue(hand);
    }

    public static AtmInitSessionMsg decode(PacketBuffer buffer) {
        return new AtmInitSessionMsg(buffer.readEnumValue(Hand.class));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            final ServerPlayerEntity sender = Objects.requireNonNull(ctx.get().getSender());
            final ICreditCardInfo cap = sender.getHeldItem(this.hand).getCapability(CreditCardProvider.CREDIT_CARD_CAPABILITY).orElseThrow(
                    () -> new NullPointerException("Null CreditCard capability")
            );
            final String name = cap.hasOwner() ? AccountWorldSavedData.get(sender.getServerWorld()).getPlayerName(cap.getOwner()) : "";

            ModPacketHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> sender),
                    new AtmCardSignedMsg(cap.hasOwner(), name)
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
