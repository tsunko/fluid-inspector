package academy.hekiyou.fluidinspector;

import java.util.concurrent.atomic.AtomicInteger;

public class ReentryGuard {

    private final ThreadLocal<AtomicInteger> depth = ThreadLocal.withInitial(() -> new AtomicInteger(0));

    public void enter() {
        depth.get().incrementAndGet();
    }

    public boolean exit() {
        return depth.get().decrementAndGet() == 0;
    }

    public int depth() {
        return depth.get().get();
    }

}
