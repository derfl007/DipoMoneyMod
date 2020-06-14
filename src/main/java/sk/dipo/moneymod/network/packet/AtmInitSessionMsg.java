package sk.dipo.moneymod.network.packet;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import sk.dipo.moneymod.world.AccountWorldSavedData;
import sk.dipo.moneymod.capability.capability.ICreditCard;
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
        buffer.writeInt(hand.ordinal());
    }

    public static AtmInitSessionMsg decode(PacketBuffer buffer) {
        return new AtmInitSessionMsg(buffer.readEnumValue(Hand.class));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            final ICreditCard cap = Objects.requireNonNull(ctx.get().getSender()).getHeldItem(this.hand).getCapability(CreditCardProvider.CREDIT_CARD_CAPABILITY).orElseThrow(
                    () -> new NullPointerException("Null CreditCard capability")
            );
            final String name = cap.hasOwner() ? AccountWorldSavedData.get(Objects.requireNonNull(ctx.get().getSender()).getServerWorld()).getPlayerName(cap.getOwner()) : "";

            ModPacketHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> ctx.get().getSender()),
                    new AtmCardSignedMsg(cap.hasOwner(), name)
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
