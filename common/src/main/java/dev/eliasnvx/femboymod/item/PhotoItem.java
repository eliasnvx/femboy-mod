package dev.eliasnvx.femboymod.item;

import dev.eliasnvx.femboymod.photo.PhotoData;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import java.util.function.Consumer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A printed photo (from the Phone's selfie). Shows its picture everywhere through a custom item renderer (the item
 * model extends {@code builtin/entity}); right-click opens it big. The client entrypoint installs {@link #onClientUse}.
 */
public class PhotoItem extends Item {

    /** Set by the client entrypoint; a no-op on dedicated servers. */
    public static Consumer<PhotoData> onClientUse = photo -> { };

    public PhotoItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        PhotoData photo = FemboyComponents.PHOTO.get(stack);
        if (photo == null) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide()) {
            onClientUse.accept(photo);
        }
        return InteractionResultHolder.success(stack);
    }
}
