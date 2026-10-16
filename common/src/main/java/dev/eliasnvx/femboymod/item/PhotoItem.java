package dev.eliasnvx.femboymod.item;

import dev.eliasnvx.femboymod.photo.PhotoData;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import java.util.function.Consumer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A printed photo (from the Phone's selfie). Shows its picture everywhere through a special item model; right-click
 * opens it big. The client entrypoint installs {@link #onClientUse}.
 */
public class PhotoItem extends Item {

    /** Set by the client entrypoint; a no-op on dedicated servers. */
    public static Consumer<PhotoData> onClientUse = photo -> { };

    public PhotoItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        PhotoData photo = stack.get(FemboyComponents.PHOTO.get());
        if (photo == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            onClientUse.accept(photo);
        }
        return InteractionResult.SUCCESS;
    }
}
