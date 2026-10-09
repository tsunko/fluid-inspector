package academy.hekiyou.fluidinspector;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class IFluidHandlerHooks {

    private static final ReentryGuard FILL_GUARD = new ReentryGuard();
    private static final ReentryGuard DRAIN_GUARD = new ReentryGuard();

    public static void beginFill(Object handler, FluidStack resource, IFluidHandler.FluidAction action) {
        FILL_GUARD.enter();
    }

    public static void endFill(int filled, Object handler, FluidStack resource, IFluidHandler.FluidAction action) {
        if (FILL_GUARD.exit() && action.execute()) {
            BlockEntity block = GetCapabilityHook.getContext();
            if (handler instanceof IFluidHandler fluidHandler && block != null && block.getLevel() != null) {
                FluidInspectorMod.stats().fill(block, fluidHandler, block.getLevel().getGameTime(), filled);
            }
        }
    }

    public static void beginDrainStack(Object handler, FluidStack resource, IFluidHandler.FluidAction action) {
        DRAIN_GUARD.enter();
    }

    public static void endDrainStack(FluidStack drained, Object handler, FluidStack resource, IFluidHandler.FluidAction action) {
        if (DRAIN_GUARD.exit() && action.execute()) {
            BlockEntity block = GetCapabilityHook.getContext();
            if (handler instanceof IFluidHandler fluidHandler && block != null && block.getLevel() != null) {
                FluidInspectorMod.stats().drain(block, fluidHandler, block.getLevel().getGameTime(), drained.getAmount());
            }
        }
    }

    public static void beginDrainInt(Object handler, int maxAmount, IFluidHandler.FluidAction action) {
        DRAIN_GUARD.enter();
    }

    public static void endDrainInt(FluidStack drained, Object handler, int maxAmount, IFluidHandler.FluidAction action) {
        if (DRAIN_GUARD.exit() && action.execute()) {
            BlockEntity block = GetCapabilityHook.getContext();
            if (handler instanceof IFluidHandler fluidHandler && block != null && block.getLevel() != null) {
                FluidInspectorMod.stats().drain(block, fluidHandler, block.getLevel().getGameTime(), drained.getAmount());
            }
        }
    }

}
