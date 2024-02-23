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
package megamek.common.weapons.bayweapons;

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.BayWeaponHandler;
import megamek.common.weapons.Weapon;
import megamek.server.GameManager;
import megamek.server.Server;

/**
 * This is my attempt to get weapon bays treated as normal weapons rather than the current hack in
 * place
 * @author Jay Lawson
 * @since Sep 24, 2004
 */
public abstract class BayWeapon extends Weapon {
    private static final long serialVersionUID = -1787970217528405766L;

    public BayWeapon() {
        super();
        // Tech progression for Small Craft or DropShip, using primitive for production and standard
        // for common.
        techAdvancement = new TechAdvancement(TECH_BASE_ALL)
                .setAdvancement(DATE_ES, 2200, 2400).setProductionFactions(F_TA)
                .setTechRating(RATING_D).setAvailability(RATING_C, RATING_E, RATING_D, RATING_C)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    @Override
    public AttackHandler fire(WeaponAttackAction waa, Game game, GameManager manager) {
        // Just in case. Often necessary when/if multiple ammo weapons are
        // fired; if this line not present
        // then when one ammo slots run dry the rest silently don't fire.
        return super.fire(waa, game, manager);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.Game)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, Game game, GameManager manager) {
        return new BayWeaponHandler(toHit, waa, game, manager);
    }

    @Override
    public int getMaxRange(Mounted weapon) {
        return this.getMaxRange(weapon, null);
    }

    @Override
    public int getMaxRange(Mounted weapon, Mounted ammo) {
        int mrange = RANGE_SHORT;
        Entity ae = weapon.getEntity();
        if (null != ae) {
            for (int wId : weapon.getBayWeapons()) {
                Mounted bayW = ae.getEquipment(wId);
                WeaponType bayWType = (WeaponType) bayW.getType();
                if (bayWType.getMaxRange(bayW) > mrange) {
                    mrange = bayWType.getMaxRange(bayW);
                }
            }
        }
        return mrange;
    }
}
