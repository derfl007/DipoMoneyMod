package sk.dipo.moneymod.items;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUseContext;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraftforge.fml.network.NetworkHooks;
import sk.dipo.moneymod.init.ModBlocks;
import sk.dipo.moneymod.tileentity.AtmTileEntity;

import java.util.Objects;

public class CreditCardItem extends Item {

    public CreditCardItem(Properties properties) {
        super(properties
                .maxStackSize(1)
        );
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext context) {
        if (context.getWorld().isRemote)
            return super.onItemUse(context);
        if (context.getWorld().getBlockState(context.getPos()).getBlock() != ModBlocks.ATM_BLOCK.get())
            return super.onItemUse(context);

        TileEntity tileEntity = context.getWorld().getTileEntity(context.getPos());
        if (tileEntity instanceof AtmTileEntity) {
            // TODO: Only if is openable
            AtmTileEntity atmTileEntity = (AtmTileEntity) tileEntity;
            atmTileEntity.creditCard = context.getItem();
            NetworkHooks.openGui((ServerPlayerEntity) Objects.requireNonNull(context.getPlayer()), atmTileEntity, packet -> {
                        packet.writeBlockPos(context.getPos());
                        packet.writeByte(context.getHand().ordinal());
                    }
            );
        }

        return ActionResultType.SUCCESS;
    }
}
