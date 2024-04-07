/*
 * MegaMek - Copyright (C) 2004-2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.prototypes;

import megamek.common.Game;
import megamek.common.Mounted;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.PrototypeCLUltraWeaponHandler;
import megamek.common.weapons.autocannons.UACWeapon;
import megamek.server.gameManager.GameManager;

/**
 * @author Andrew Hunter
 * @since Sept 29, 2004
 */
public abstract class CLPrototypeUACWeapon extends UACWeapon {
    private static final long serialVersionUID = 8905222321912752035L;

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.Game)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
                                              GameManager manager) {
        Mounted weapon = game.getEntity(waa.getEntityId()).getEquipment(waa.getWeaponId());
        if (weapon.curMode().equals("Ultra")) {
            return new PrototypeCLUltraWeaponHandler(toHit, waa, game, manager);
        }
        return super.getCorrectHandler(toHit, waa, game, manager);
    }
}
