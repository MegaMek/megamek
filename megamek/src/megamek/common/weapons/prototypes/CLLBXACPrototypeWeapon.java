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
package megamek.common.weapons.prototypes;

import megamek.common.AmmoType;
import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.ACWeaponHandler;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.CLLBXPrototypeHandler;
import megamek.common.weapons.autocannons.LBXACWeapon;
import megamek.server.GameManager;

/**
 * @author Andrew Hunter
 * @since Oct 14, 2004
 */
public abstract class CLLBXACPrototypeWeapon extends LBXACWeapon {

    private static final long serialVersionUID = -1702237743474540150L;

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.Game,
     * megamek.server.Server)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, Game game, GameManager manager) {
        AmmoType atype = (AmmoType) game.getEntity(waa.getEntityId())
                .getEquipment(waa.getWeaponId()).getLinked().getType();
        if (atype.getMunitionType().contains(AmmoType.Munitions.M_CLUSTER)) {
            return new CLLBXPrototypeHandler(toHit, waa, game, manager);
        }
        return new ACWeaponHandler(toHit, waa, game, manager);
    }
}
