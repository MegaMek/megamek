package megamek.client;
import java.util.*;

/**
 *  a singleton class (I hate singletons) to act as a central
 *  point for things requiring timer services in clients. 
 *
 *  note: acts as a daemon thread so will exit when other threads have
 *  exited.
 */
public class TimerSingleton {
    protected static TimerSingleton inst;

    public static synchronized TimerSingleton getInstance() {
        if (inst==null)
            inst=new TimerSingleton();
        return inst;
    }
    //-------------------------
    protected Timer t;
    public TimerSingleton() {
        t=new Timer(true);
    }
    public void schedule(TimerTask tt,long delay,long interval) {
        t.schedule(tt,delay,interval);
    }
}
