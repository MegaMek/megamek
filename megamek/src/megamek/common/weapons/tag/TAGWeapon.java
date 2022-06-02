/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.tag;

import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.TAGHandler;
import megamek.common.weapons.Weapon;
import megamek.server.GameManager;

/**
 * @author Sebastian Brocks
 * @since Sep 7, 2005
 */
public abstract class TAGWeapon extends Weapon {
    private static final long serialVersionUID = 6794299593713032006L;

    public TAGWeapon() {
        super();
        flags = flags.or(F_MECH_WEAPON).or(F_TANK_WEAPON).or(F_AERO_WEAPON)
                .or(F_TAG).or(F_NO_FIRES);
        setModes(new String[] { "1-shot", "2-shot", "3-shot", "4-shot" });
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
                                              GameManager manager) {
        return new TAGHandler(toHit, waa, game, manager);
    }
}
