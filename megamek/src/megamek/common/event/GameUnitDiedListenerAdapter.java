/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.event;
import megamek.common.Entity;

/**
 * This adapter class overrides just the GameUnitDiedEvent handling of the
 * by the <code>GameListenerAdapter</code> class.
 *
 * @see GameListenerAdapter
 * @see GameListener
 * @see GameEvent
 */
public class GameUnitDiedListenerAdapter extends GameListenerAdapter {

    // Who would claim the death as a kill?
    private Entity attacker;

    public GameUnitDiedListenerAdapter(Entity attacker){
        this.attacker = attacker;
    }
    @Override
    public void gameUnitDied(GameUnitDiedEvent e){
    }
}
