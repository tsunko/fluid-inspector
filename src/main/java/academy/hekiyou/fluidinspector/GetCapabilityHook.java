package academy.hekiyou.fluidinspector;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GetCapabilityHook {

    private static final Set<Class<?>> NOT_BLOCKENTITY_PROVIDERS = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final ThreadLocal<WeakReference<BlockEntity>> context = new ThreadLocal<>();

    public static void getCapabilityHook(Object provider, Capability<?> cap, Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            if (provider instanceof BlockEntity block) {
                context.set(new WeakReference<>(block));
            } else {
                if (!NOT_BLOCKENTITY_PROVIDERS.contains(provider.getClass())) {
                    FluidInspectorMod.LOGGER.info("Don't know how to derive from {}", provider.getClass().getName());
                    NOT_BLOCKENTITY_PROVIDERS.add(provider.getClass());
                }
            }
        }
    }

    public static BlockEntity getContext() {
        if (context.get() != null) {
            return context.get().get();
        } else {
            return null;
        }
    }

}
