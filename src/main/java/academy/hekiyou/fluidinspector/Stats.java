package academy.hekiyou.fluidinspector;

import com.google.common.collect.MapMaker;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.Map;

public class Stats {

    // why two maps instead of just BlockEntity -> Entry?
    // multiblock structures may have multiple BlockEntities but share a singular IFluidHandler (i.e Create fluid tank)
    private final Map<IFluidHandler, Entry> stats = new MapMaker().weakKeys().makeMap();
    private final Map<BlockEntity, IFluidHandler> entityToHandlerMap = new MapMaker().weakKeys().weakValues().makeMap();

    public void fill(BlockEntity block, IFluidHandler handler, long tick, int amount) {
        if (block != null) {
            getEntry(block, handler).onFill(tick, amount);
        }
    }

    public void drain(BlockEntity block, IFluidHandler handler, long tick, int amount) {
        if (block != null) {
            getEntry(block, handler).onDrain(tick, amount);
        }
    }

    public Entry getEntry(BlockEntity block, IFluidHandler handler) {
        if (stats.containsKey(handler)) return stats.get(handler);
        IFluidHandler existing = entityToHandlerMap.computeIfAbsent(block, b -> handler);
        return stats.computeIfAbsent(existing, h -> new Entry());
    }

    public static class Entry {
        private static final int MAX_EVENTS = 100;

        private final FluidEvent[] fill = new FluidEvent[MAX_EVENTS];
        private final FluidEvent[] drain = new FluidEvent[MAX_EVENTS];

        private int fillHead, drainHead;

        void onFill(long tick, int amount) {
            fill[fillHead] = new FluidEvent(tick, amount);
            fillHead = (fillHead + 1) % MAX_EVENTS;
        }

        void onDrain(long tick, int amount) {
            drain[drainHead] = new FluidEvent(tick, amount);
            drainHead = (drainHead + 1) % MAX_EVENTS;
        }

        public FlowRate calculateFlowRate(long tick) {
            long fillSum = 0;
            long drainSum = 0;

            for (int i=0; i < MAX_EVENTS; i++) {
                if (fill[i] != null && tick - fill[i].tick < 20) fillSum += fill[i].amount;
                if (drain[i] != null && tick - drain[i].tick < 20) drainSum += drain[i].amount;
            }

            return new FlowRate(fillSum / 20.0, drainSum / 20.0);
        }
    }

    public record FluidEvent(long tick, int amount) {}
    public record FlowRate(double fill, double drain) {}

}
