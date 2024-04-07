/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.weapons;

import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.GamePhase;
import megamek.server.gameManager.GameManager;

/**
 * @author Sebastian Brocks
 * @since Sep 24, 2004
 */
public class ArtilleryWeaponDirectHomingHandler extends ArtilleryWeaponIndirectHomingHandler {
    private static final long serialVersionUID = 7543333005465757983L;

    /**
     * @param t
     * @param w
     * @param g
     */
    public ArtilleryWeaponDirectHomingHandler(ToHitData t, WeaponAttackAction w, Game g, GameManager m) {
        super(t, w, g, m);

    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.AttackHandler#cares(int)
     */
    @Override
    public boolean cares(GamePhase phase) {
        return phase.isFiring();
    }
}
