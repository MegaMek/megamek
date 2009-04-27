/*
 * MegaMek -
 * Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
 * 
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.client;

import java.util.Timer;
import java.util.TimerTask;

/**
 * a singleton class (I hate singletons) to act as a central point for things
 * requiring timer services in clients.
 * 
 * note: acts as a daemon thread so will
 * exit when other threads have exited.
 */
public class TimerSingleton {
    protected static TimerSingleton inst;

    public static synchronized TimerSingleton getInstance() {
        if (inst == null)
            inst = new TimerSingleton();
        return inst;
    }

    // -------------------------
    protected Timer t;

    public TimerSingleton() {
        t = new Timer(true);
    }

    public void schedule(TimerTask tt, long delay, long interval) {
        t.schedule(tt, delay, interval);
    }
}
