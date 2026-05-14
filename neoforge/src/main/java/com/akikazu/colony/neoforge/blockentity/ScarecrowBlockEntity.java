package com.akikazu.colony.neoforge.blockentity;

import com.akikazu.colony.api.building.BuildingId;
import com.akikazu.colony.api.workzone.AnchorConfigurationResult;
import com.akikazu.colony.api.workzone.AnchorId;
import com.akikazu.colony.api.workzone.AxisAlignedZone;
import com.akikazu.colony.api.workzone.WorkZoneAnchor;
import com.akikazu.colony.api.workzone.WorkZoneAnchorType;
import com.akikazu.colony.common.workzone.impl.AnchorIndex;
import com.akikazu.colony.common.workzone.scarecrow.ScarecrowAnchorType;
import com.akikazu.colony.common.workzone.scarecrow.ScarecrowConfigurationHandler;
import com.akikazu.colony.core.registry.Identifier;
import com.mojang.serialization.DataResult;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;
import java.util.Optional;

/**
 * Persistent state for a placed scarecrow: identity, link to its owning building, work zone, and the crop the player
 * has assigned via right-click. Implements the cross-loader {@link WorkZoneAnchor} read view so services in
 * {@code :common} can treat any anchor uniformly without reaching into block-entity APIs.
 *
 * <p>
 * The default zone is the canonical scarecrow footprint declared in {@code docs/04-BUILDING-SYSTEM.md}: 3 blocks in
 * each horizontal direction, 1 up, 1 down — a 7×3×7 box around the post. Players adjust the offsets through the
 * configuration screen; persistence stores the offsets, the {@link AxisAlignedZone#center} is re-supplied from the
 * block's own {@link BlockPos} on every load to keep the two in sync if the block ever moves (e.g. piston).
 */
public final class ScarecrowBlockEntity extends BlockEntity implements WorkZoneAnchor
{
    public static final int DEFAULT_RADIUS = 3;
    public static final int DEFAULT_UP = 1;
    public static final int DEFAULT_DOWN = 1;

    private static final String KEY_ANCHOR_ID = "anchorId";
    private static final String KEY_LINKED_BUILDING = "linkedBuilding";
    private static final String KEY_WORK_ZONE = "workZone";
    private static final String KEY_ASSIGNED_CROP = "assignedCrop";
    private static final String KEY_CREATED_TICK = "createdAtTick";
    private static final String KEY_DATA = "anchorData";

    private AnchorId anchorId;

    private @org.jspecify.annotations.Nullable BuildingId linkedBuilding;

    private AxisAlignedZone workZone;

    private CompoundTag anchorData;

    private long createdAtTick;

    public ScarecrowBlockEntity(BlockPos pos, BlockState state)
    {
        super(ColonyBlockEntities.SCARECROW.get(), pos, state);
        this.anchorId = AnchorId.random();
        this.linkedBuilding = null;
        this.workZone = defaultZone(pos);
        this.anchorData = new CompoundTag();
        this.createdAtTick = 0L;
    }

    public static AxisAlignedZone defaultZone(BlockPos center)
    {
        return new AxisAlignedZone(
                center,
                DEFAULT_RADIUS, DEFAULT_RADIUS,
                DEFAULT_RADIUS, DEFAULT_RADIUS,
                DEFAULT_UP, DEFAULT_DOWN);
    }

    public void initializeIfFresh(ServerLevel level)
    {
        if (createdAtTick == 0L)
        {
            this.createdAtTick = level.getGameTime();
            setChanged();
        }

        AnchorIndex.get(level).registerIfAbsent(anchorId, ScarecrowAnchorType.ID, getBlockPos(), linkedBuilding());
    }

    @Override
    public AnchorId id()
    {
        return anchorId;
    }

    @Override
    public WorkZoneAnchorType type()
    {
        return ScarecrowAnchorType.INSTANCE;
    }

    @Override
    public BlockPos position()
    {
        return getBlockPos();
    }

    @Override
    public Optional<BuildingId> linkedBuilding()
    {
        return Optional.ofNullable(linkedBuilding);
    }

    @Override
    public AxisAlignedZone workZone()
    {
        return workZone.withCenter(getBlockPos());
    }

    public Optional<Identifier> assignedCrop()
    {
        if (!anchorData.contains(ScarecrowConfigurationHandler.ASSIGNED_CROP_KEY))
        {
            return Optional.empty();
        }

        String value = anchorData.getString(ScarecrowConfigurationHandler.ASSIGNED_CROP_KEY);

        if (value.isEmpty())
        {
            return Optional.empty();
        }

        try
        {
            ResourceLocation loc = ResourceLocation.parse(value);

            return Optional.of(Identifier.of(loc.getNamespace(), loc.getPath()));
        }
        catch (net.minecraft.ResourceLocationException ignored)
        {
            return Optional.empty();
        }
    }

    public long createdAtTick()
    {
        return createdAtTick;
    }

    public AnchorConfigurationResult applyConfiguration(ItemStack stack)
    {
        Objects.requireNonNull(stack, "stack");

        AnchorConfigurationResult result = ScarecrowAnchorType.INSTANCE
                .configurationHandler()
                .apply(stack, anchorData);

        if (result instanceof AnchorConfigurationResult.Applied)
        {
            setChanged();
        }

        return result;
    }

    public void setWorkZoneOffsets(int north, int south, int east, int west, int up, int down)
    {
        this.workZone = new AxisAlignedZone(getBlockPos(), north, south, east, west, up, down);
        setChanged();
    }

    public void setLinkedBuilding(@org.jspecify.annotations.Nullable BuildingId building)
    {
        this.linkedBuilding = building;
        setChanged();

        if (level instanceof ServerLevel serverLevel)
        {
            AnchorIndex.get(serverLevel).updateLink(anchorId, Optional.ofNullable(building));
        }
    }

    public void clearAssignedCrop()
    {
        anchorData.remove(ScarecrowConfigurationHandler.ASSIGNED_CROP_KEY);
        setChanged();
    }

    @Override
    public void setRemoved()
    {
        if (level instanceof ServerLevel serverLevel)
        {
            AnchorIndex.get(serverLevel).remove(anchorId);
        }

        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider)
    {
        super.saveAdditional(tag, provider);

        DataResult<Tag> idEncoded = AnchorId.CODEC.encodeStart(NbtOps.INSTANCE, anchorId);
        tag.put(KEY_ANCHOR_ID, idEncoded.getOrThrow());

        if (linkedBuilding != null)
        {
            DataResult<Tag> linkEncoded = BuildingId.CODEC.encodeStart(NbtOps.INSTANCE, linkedBuilding);
            tag.put(KEY_LINKED_BUILDING, linkEncoded.getOrThrow());
        }

        DataResult<Tag> zoneEncoded = AxisAlignedZone.CODEC.encodeStart(NbtOps.INSTANCE, workZone);
        tag.put(KEY_WORK_ZONE, zoneEncoded.getOrThrow());

        tag.put(KEY_DATA, anchorData.copy());

        tag.putLong(KEY_CREATED_TICK, createdAtTick);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider)
    {
        super.loadAdditional(tag, provider);

        if (tag.contains(KEY_ANCHOR_ID))
        {
            this.anchorId = AnchorId.CODEC.parse(NbtOps.INSTANCE, tag.get(KEY_ANCHOR_ID)).getOrThrow();
        }

        if (tag.contains(KEY_LINKED_BUILDING))
        {
            this.linkedBuilding = BuildingId.CODEC
                    .parse(NbtOps.INSTANCE, tag.get(KEY_LINKED_BUILDING))
                    .getOrThrow();
        }
        else
        {
            this.linkedBuilding = null;
        }

        if (tag.contains(KEY_WORK_ZONE))
        {
            this.workZone = AxisAlignedZone.CODEC
                    .parse(NbtOps.INSTANCE, tag.get(KEY_WORK_ZONE))
                    .getOrThrow()
                    .withCenter(getBlockPos());
        }
        else
        {
            this.workZone = defaultZone(getBlockPos());
        }

        this.anchorData = decodeAnchorData(tag);

        this.createdAtTick = tag.getLong(KEY_CREATED_TICK);
    }

    private static CompoundTag decodeAnchorData(CompoundTag tag)
    {
        if (tag.contains(KEY_DATA, Tag.TAG_COMPOUND))
        {
            return tag.getCompound(KEY_DATA).copy();
        }

        CompoundTag built = new CompoundTag();

        if (tag.contains(KEY_ASSIGNED_CROP))
        {
            built.putString(
                    ScarecrowConfigurationHandler.ASSIGNED_CROP_KEY,
                    tag.getString(KEY_ASSIGNED_CROP));
        }

        return built;
    }
}
