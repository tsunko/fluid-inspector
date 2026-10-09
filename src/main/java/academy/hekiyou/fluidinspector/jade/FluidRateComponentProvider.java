package academy.hekiyou.fluidinspector.jade;

import academy.hekiyou.fluidinspector.FluidInspectorMod;
import academy.hekiyou.fluidinspector.Stats;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

public enum FluidRateComponentProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    INSTANCE;

    private static final String FLUID_RATE_TAG = "fluid_rate";
    private static final String FILL_RATE_TAG = "fill_rate";
    private static final String DRAIN_RATE_TAG = "drain_rate";

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockEntity entity = accessor.getBlockEntity();
        if (entity == null) return;

        CompoundTag tag = (CompoundTag)accessor.getServerData().get(FLUID_RATE_TAG);
        if (tag != null) {
            tooltip.add(
                    Component.translatable("fluid_inspector_tooltip")
                            .append(" ")
                            .append(
                                    Component.literal("↑").withStyle(ChatFormatting.GREEN)
                            )
                            .append(Component.translatable("rate_tooltip", tag.getDouble(FILL_RATE_TAG)))
                            .append(" ")
                            .append(
                                    Component.literal("↓").withStyle(ChatFormatting.RED)
                            )
                            .append(Component.translatable("rate_tooltip", tag.getDouble(DRAIN_RATE_TAG)))
            );
        }
    }

    @Override
    public void appendServerData(CompoundTag root, BlockAccessor accessor) {
        BlockEntity entity = accessor.getBlockEntity();
        if (entity == null) return;

        entity.getCapability(ForgeCapabilities.FLUID_HANDLER, accessor.getSide()).ifPresent(handler -> {
            Stats.Entry entry = FluidInspectorMod.stats().getEntry(entity, handler);
            Stats.FlowRate rate = entry.calculateFlowRate(accessor.getLevel().getGameTime());

            CompoundTag tag = new CompoundTag();
            tag.putDouble(FILL_RATE_TAG, round(rate.fill()));
            tag.putDouble(DRAIN_RATE_TAG, round(rate.drain()));

            root.put(FLUID_RATE_TAG, tag);
        });
    }

    @Override
    public ResourceLocation getUid() {
        return FluidInspectorMod.RESOURCE_LOC;
    }

    private double round(double in) {
        return Math.floor(in * 100) / 100;
    }

}
