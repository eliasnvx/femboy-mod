package dev.eliasnvx.femboymod.neoforge;

import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.gametest.InteriorGameTests;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** NeoForge only: grave mods read {@link LivingDropsEvent}, so a dying player's cosmetics must be in its drops. */
final class GraveDropsGameTest {

    /** What the stand-in grave mod took, per player it acts on (it ignores everyone else). */
    private static final Map<UUID, List<ItemStack>> GRAVES = new ConcurrentHashMap<>();
    private static boolean listening;

    private GraveDropsGameTest() {
    }

    private static synchronized void listen() {
        if (listening) {
            return;
        }
        listening = true;
        // like a grave mod: last in line, takes the drops into the grave and cancels them
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (LivingDropsEvent event) -> {
            if (GRAVES.containsKey(event.getEntity().getUUID())) {
                GRAVES.put(event.getEntity().getUUID(), event.getDrops().stream().map(ItemEntity::getItem).toList());
                event.setCanceled(true);
            }
        });
    }

    static void graveModsCollectCosmetics(GameTestHelper helper) {
        listen();
        ServerPlayer player = InteriorGameTests.survivalPlayer(helper, "grave_tester", new BlockPos(1, 1, 1));
        GRAVES.put(player.getUUID(), List.of());
        try {
            CosmeticsManager.set(player, FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.CAT_EARS.get()));
            player.getInventory().add(new ItemStack(Items.STICK));
            player.die(player.damageSources().genericKill());
            List<ItemStack> grave = GRAVES.get(player.getUUID());
            helper.assertTrue(grave.stream().anyMatch(stack -> stack.is(Items.STICK)), "the grave got the inventory");
            helper.assertTrue(grave.stream().anyMatch(stack -> stack.is(FemboyItems.CAT_EARS.get())), "and the outfit with it");
            helper.assertTrue(CosmeticsManager.get(player).isEmpty(), "the slots are empty");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(3),
                    item -> item.getItem().is(FemboyItems.CAT_EARS.get())).isEmpty(), "nothing left on the ground");
            helper.succeed();
        } finally {
            GRAVES.remove(player.getUUID());
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
    }
}
