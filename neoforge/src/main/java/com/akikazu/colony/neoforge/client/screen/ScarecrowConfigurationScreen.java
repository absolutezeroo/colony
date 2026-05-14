package com.akikazu.colony.neoforge.client.screen;

import com.akikazu.colony.api.workzone.AxisAlignedZone;
import com.akikazu.colony.neoforge.blockentity.ScarecrowBlockEntity;
import com.akikazu.colony.neoforge.network.UpdateScarecrowConfigurationPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import org.jspecify.annotations.Nullable;

/**
 * Configuration screen for a scarecrow anchor. Edits the zone offsets, displays the current crop assignment and link
 * status, and pushes the result to the server via {@link UpdateScarecrowConfigurationPayload} on save.
 *
 * <p>
 * Vanilla {@link Screen} rather than a {@link net.minecraft.world.inventory.AbstractContainerMenu}: the scarecrow has
 * no inventory to expose, so the menu/container machinery would be pure overhead. The screen reads the scarecrow's
 * current state directly from the client-synced {@link ScarecrowBlockEntity} on open.
 */
@OnlyIn(Dist.CLIENT)
public final class ScarecrowConfigurationScreen extends Screen
{
    private static final int MIN_VOLUME = 9;
    private static final int MAX_VOLUME = 4096;

    private final BlockPos anchorPos;

    private @Nullable EditBox northBox;

    private @Nullable EditBox southBox;

    private @Nullable EditBox eastBox;

    private @Nullable EditBox westBox;

    private @Nullable EditBox upBox;

    private @Nullable EditBox downBox;

    public ScarecrowConfigurationScreen(BlockPos anchorPos)
    {
        super(Component.translatable("colony.gui.scarecrow.title"));
        this.anchorPos = anchorPos;
    }

    @Override
    protected void init()
    {
        super.init();

        AxisAlignedZone current = currentZone();

        int centerX = this.width / 2;
        int topY = 50;
        int boxWidth = 40;
        int boxHeight = 20;
        int spacing = 4;

        this.northBox = makeBox(centerX - boxWidth / 2, topY, boxWidth, boxHeight, current.north());
        this.southBox = makeBox(centerX - boxWidth / 2, topY + 90, boxWidth, boxHeight, current.south());
        this.westBox = makeBox(centerX - boxWidth - spacing - boxWidth / 2, topY + 45, boxWidth, boxHeight,
                current.west());
        this.eastBox = makeBox(centerX + spacing + boxWidth / 2, topY + 45, boxWidth, boxHeight, current.east());
        this.upBox = makeBox(centerX - boxWidth - spacing - boxWidth / 2, topY + 90, boxWidth, boxHeight,
                current.up());
        this.downBox = makeBox(centerX + spacing + boxWidth / 2, topY + 90, boxWidth, boxHeight, current.down());

        int buttonY = this.height - 30;
        int buttonWidth = 80;

        this.addRenderableWidget(Button.builder(
                Component.translatable("colony.gui.scarecrow.reset"),
                btn -> resetZone())
                .bounds(centerX - buttonWidth - spacing - buttonWidth, buttonY, buttonWidth, 20)
                .build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("colony.gui.scarecrow.save"),
                btn -> saveAndClose())
                .bounds(centerX + spacing, buttonY, buttonWidth, 20)
                .build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.cancel"),
                btn -> this.onClose())
                .bounds(centerX - buttonWidth - spacing, buttonY, buttonWidth, 20)
                .build());
    }

    private EditBox makeBox(int x, int y, int w, int h, int initialValue)
    {
        EditBox box = new EditBox(this.font, x, y, w, h, Component.empty());
        box.setValue(Integer.toString(initialValue));
        box.setFilter(s -> s.isEmpty() || s.matches("\\d{0,3}"));
        this.addRenderableWidget(box);

        return box;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground(gfx, mouseX, mouseY, partialTicks);
        super.render(gfx, mouseX, mouseY, partialTicks);

        gfx.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);

        Component anchorInfo = Component.translatable(
                "colony.gui.scarecrow.position",
                anchorPos.getX(), anchorPos.getY(), anchorPos.getZ());
        gfx.drawCenteredString(this.font, anchorInfo, this.width / 2, 30, 0xAAAAAA);

        gfx.drawCenteredString(this.font, Component.translatable("colony.gui.scarecrow.direction.north"),
                this.width / 2, 40, 0xFFFFFF);
        gfx.drawCenteredString(this.font, Component.translatable("colony.gui.scarecrow.direction.south"),
                this.width / 2, 145, 0xFFFFFF);
        gfx.drawCenteredString(this.font, Component.translatable("colony.gui.scarecrow.direction.west"),
                this.width / 2 - 80, 100, 0xFFFFFF);
        gfx.drawCenteredString(this.font, Component.translatable("colony.gui.scarecrow.direction.east"),
                this.width / 2 + 80, 100, 0xFFFFFF);

        Component cropLine = assignedCropLabel();
        gfx.drawCenteredString(this.font, cropLine, this.width / 2, 165, 0xFFFFFF);

        Component linkLine = linkedBuildingLabel();
        gfx.drawCenteredString(this.font, linkLine, this.width / 2, 180, 0xFFFFFF);
    }

    private Component assignedCropLabel()
    {
        ScarecrowBlockEntity be = scarecrow();

        if (be == null)
        {
            return Component.translatable("colony.gui.scarecrow.no_crop");
        }

        return be.assignedCrop()
                .map(id -> Component.translatable(
                        "colony.gui.scarecrow.crop",
                        Component.translatable("block." + id.namespace() + "." + id.path().replace('/', '.'))))
                .orElse(Component.translatable("colony.gui.scarecrow.no_crop"));
    }

    private Component linkedBuildingLabel()
    {
        ScarecrowBlockEntity be = scarecrow();

        if (be == null || be.linkedBuilding().isEmpty())
        {
            return Component.translatable("colony.gui.scarecrow.not_linked");
        }

        return Component.translatable(
                "colony.gui.scarecrow.linked",
                be.linkedBuilding().get().toString().substring(0, 8));
    }

    private AxisAlignedZone currentZone()
    {
        ScarecrowBlockEntity be = scarecrow();

        if (be == null)
        {
            return ScarecrowBlockEntity.defaultZone(anchorPos);
        }

        return be.workZone();
    }

    private @Nullable ScarecrowBlockEntity scarecrow()
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null)
        {
            return null;
        }

        BlockEntity be = mc.level.getBlockEntity(anchorPos);

        if (be instanceof ScarecrowBlockEntity scarecrow)
        {
            return scarecrow;
        }

        return null;
    }

    private void resetZone()
    {
        if (northBox == null || southBox == null || eastBox == null
                || westBox == null || upBox == null || downBox == null)
        {
            return;
        }

        northBox.setValue(Integer.toString(ScarecrowBlockEntity.DEFAULT_RADIUS));
        southBox.setValue(Integer.toString(ScarecrowBlockEntity.DEFAULT_RADIUS));
        eastBox.setValue(Integer.toString(ScarecrowBlockEntity.DEFAULT_RADIUS));
        westBox.setValue(Integer.toString(ScarecrowBlockEntity.DEFAULT_RADIUS));
        upBox.setValue(Integer.toString(ScarecrowBlockEntity.DEFAULT_UP));
        downBox.setValue(Integer.toString(ScarecrowBlockEntity.DEFAULT_DOWN));
    }

    private void saveAndClose()
    {
        int north = parse(northBox, ScarecrowBlockEntity.DEFAULT_RADIUS);
        int south = parse(southBox, ScarecrowBlockEntity.DEFAULT_RADIUS);
        int east = parse(eastBox, ScarecrowBlockEntity.DEFAULT_RADIUS);
        int west = parse(westBox, ScarecrowBlockEntity.DEFAULT_RADIUS);
        int up = parse(upBox, ScarecrowBlockEntity.DEFAULT_UP);
        int down = parse(downBox, ScarecrowBlockEntity.DEFAULT_DOWN);

        long volume = (long) (east + west + 1) * (up + down + 1) * (north + south + 1);

        if (volume < MIN_VOLUME || volume > MAX_VOLUME)
        {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.translatable("colony.message.scarecrow.invalid_volume", MIN_VOLUME, MAX_VOLUME),
                    false);

            return;
        }

        PacketDistributor.sendToServer(new UpdateScarecrowConfigurationPayload(
                anchorPos, north, south, east, west, up, down));

        this.onClose();
    }

    private static int parse(@Nullable EditBox box, int fallback)
    {
        if (box == null)
        {
            return fallback;
        }

        try
        {
            int v = Integer.parseInt(box.getValue());

            return v < 0 ? 0 : v;
        }
        catch (NumberFormatException ignored)
        {
            return fallback;
        }
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
