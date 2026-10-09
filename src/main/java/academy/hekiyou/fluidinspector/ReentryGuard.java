package academy.hekiyou.fluidinspector;

public class ReentryGuard {

    private final ThreadLocal<Integer> depth = ThreadLocal.withInitial(() -> 0);

    public void enter() {
        depth.set(depth.get() + 1);
    }

    public boolean exit() {
        int val = depth.get() - 1;
        depth.set(val);
        return val == 0;
    }

    public int depth() {
        return depth.get();
    }

}
