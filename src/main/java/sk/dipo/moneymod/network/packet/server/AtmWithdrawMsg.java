package sk.dipo.moneymod.network.packet.server;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import sk.dipo.moneymod.capability.capability.ICreditCardInfo;
import sk.dipo.moneymod.capability.provider.CreditCardProvider;
import sk.dipo.moneymod.network.ModPacketHandler;
import sk.dipo.moneymod.network.packet.client.AtmBalanceMsg;
import sk.dipo.moneymod.network.packet.client.AtmErrorMsg;
import sk.dipo.moneymod.tileentity.AtmTileEntity;
import sk.dipo.moneymod.world.AccountWorldSavedData;

import java.util.Objects;
import java.util.function.Supplier;

public class AtmWithdrawMsg {

    private final Hand hand;
    private final BlockPos blockPos;
    private final int value;

    public AtmWithdrawMsg(Hand hand, BlockPos blockPos, int value) {
        this.hand = hand;
        this.blockPos = blockPos;
        this.value = value;
    }

    public void encode(PacketBuffer buffer) {
        buffer.writeEnumValue(hand);
        buffer.writeBlockPos(blockPos);
        buffer.writeInt(value);
    }

    public static AtmWithdrawMsg decode(PacketBuffer buffer) {
        return new AtmWithdrawMsg(buffer.readEnumValue(Hand.class), buffer.readBlockPos(), buffer.readInt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            final ServerPlayerEntity sender = Objects.requireNonNull(ctx.get().getSender());
            final ICreditCardInfo cap = sender.getHeldItem(this.hand).getCapability(CreditCardProvider.CREDIT_CARD_CAPABILITY).orElseThrow(
                    () -> new NullPointerException("Null CreditCard capability")
            );

            if (!cap.hasOwner()) {
                ModPacketHandler.INSTANCE.send(
                        PacketDistributor.PLAYER.with(() -> sender), new AtmErrorMsg()
                );
                return;
            }

            final AccountWorldSavedData accountData = AccountWorldSavedData.get(sender.getServerWorld());
            final AtmTileEntity atmTileEntity = (AtmTileEntity) sender.getServerWorld().getTileEntity(this.blockPos);
            if (atmTileEntity != null) {
                accountData.deposit(cap.getOwner(), atmTileEntity.withdraw(accountData.withdraw(cap.getOwner(), this.value)));
                ModPacketHandler.INSTANCE.send(
                        PacketDistributor.PLAYER.with(() -> sender),
                        new AtmBalanceMsg(accountData.getBalance(cap.getOwner()))
                );
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
