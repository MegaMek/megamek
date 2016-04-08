/*
 * MMEvent.java - Simple event system helper class
 *
 * Copyright (C) 2016 MegaMek Team
 *
 * This file is part of MegaMek
 *
 * Some rights reserved. See megamek/docs/license.txt
 */

package megamek.common.event;

/**
 * Base class for all events
 */
public abstract class MMEvent {
    protected boolean cancelled = false;
    
    public MMEvent() {
    }
    
    /** @return true if the event can be cancelled (aborted) */
    public boolean isCancellable() {
        return false;
    }
    
    /** @return true if the event is cancelled */
    public boolean isCancelled() {
        return cancelled;
    }

    public void cancel() {
        if(isCancellable()) {
            cancelled = true;
        }
    }
}
