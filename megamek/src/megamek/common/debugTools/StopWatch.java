package megamek.common.debugTools;

public class StopWatch {
    private long total = 0;
    private long start = 0;
    private boolean paused = true;
    public StopWatch() {
    }

    public void clear() {
        paused = true;
        total = 0;
    }
    public void restart() {
        clear();
        paused = false;
        start = System.nanoTime();
    }

    public void unpause() {
        if (paused) {
            start = System.nanoTime();
            paused = false;
        }
    }

    public long pause() {
        if (!paused) {
            total += System.nanoTime() - start;
            paused = true;
        }
        return total;
    }

    public long getTime() {
        if (paused)
            return total;
        else
            return total + (System.nanoTime() - start);
    }

}
