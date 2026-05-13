package com.akikazu.colony.neoforge.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Placeholder for the Colony Tool, the single multi-purpose item given to the player at colony founding.
 *
 * <p>
 * Modes (Zone / Storage / Link / Inspect, see {@code docs/04-BUILDING-SYSTEM.md}) land in prompt 2.1; this stub only
 * ships the item registration and a chat acknowledgement so the founding flow can hand it to the player. Persistence
 * across death (#colony:undroppable_on_death tag) is also deferred to 2.1 — the founding test does not exercise death.
 */
public final class ColonyToolItem extends Item
{
    public ColonyToolItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide())
        {
            player.sendSystemMessage(Component.literal("Colony Tool used (modes coming in prompt 2.1)"));
        }

        return InteractionResultHolder.success(stack);
    }
}
