package com.akikazu.colony.neoforge.item;

import com.akikazu.colony.api.item.ColonyToolMode;
import com.akikazu.colony.neoforge.ColonyMod;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registry holder for Colony's item {@link DataComponentType}s.
 *
 * <p>
 * The {@code tool_mode} component is attached to each Colony Tool stack so that the cycled mode survives stack splits,
 * inventory moves, and save reloads. Codec and stream codec live on {@link ColonyToolMode} so that {@code :api} stays
 * loader-agnostic.
 */
public final class ColonyDataComponents
{
    private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister
            .createDataComponents(Registries.DATA_COMPONENT_TYPE, ColonyMod.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ColonyToolMode>> TOOL_MODE = COMPONENTS
            .registerComponentType(
                    "tool_mode",
                    builder -> builder
                            .persistent(ColonyToolMode.CODEC)
                            .networkSynchronized(ColonyToolMode.STREAM_CODEC));

    private ColonyDataComponents()
    {
    }

    public static void register(IEventBus modEventBus)
    {
        COMPONENTS.register(modEventBus);
    }
}
