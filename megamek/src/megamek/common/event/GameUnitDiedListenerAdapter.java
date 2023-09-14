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
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.Weapon;
import megamek.server.GameManager;

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
    private final Entity attacker;
    // How was it done?
    private final WeaponAttackAction action;

    public GameUnitDiedListenerAdapter(Entity attacker, WeaponAttackAction action){
        this.attacker = attacker;
        this.action = action;
    }
    @Override
    public void gameUnitDied(GameUnitDiedEvent e) {
        GameManager gm = (GameManager) e.getSource();
        Entity en = (Entity) e.getEntity();
        // If our attacker's weapon action originally targeted this entity, take credit
        if (action.getOriginalTargetId() == en.getId()){
            gm.creditKill(en, attacker);
        }
    }
}
