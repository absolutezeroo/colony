package com.akikazu.colony.api.workzone;

import com.akikazu.colony.core.registry.Identifier;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import org.jetbrains.annotations.ApiStatus;

/**
 * Type descriptor for a registered work-zone anchor. Implementations are produced by content registrations and
 * dispatched via {@link com.akikazu.colony.api.registry.ColonyRegistries#ANCHOR_TYPE}.
 *
 * <p>
 * Each anchor type pairs a concrete in-world {@link #anchorBlock()} with the {@link #configurationItemTag()} that
 * configures it on right-click and the {@link #configurationHandler()} that applies the configuration. Multiple items
 * can configure a single anchor type (e.g. any seed configures a scarecrow), which is why a {@link TagKey} drives the
 * acceptance check instead of a single item.
 */
@ApiStatus.NonExtendable
public interface WorkZoneAnchorType
{
    Identifier id();

    Component displayName();

    Block anchorBlock();

    TagKey<Item> configurationItemTag();

    AnchorConfigurationHandler configurationHandler();
}
