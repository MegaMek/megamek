/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common.weapons.other;

import megamek.common.AmmoType;
import megamek.common.IGame;
import megamek.common.TechAdvancement;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.MissileWeapon;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class ISC3RemoteSensorLauncher extends MissileWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -6850419038862085767L;

    /**
     *
     */
    public ISC3RemoteSensorLauncher() {
        super();
        name = "C3 Remote Sensor Launcher";
        setInternalName("ISC3RemoteSensorLauncher");
        addLookupName("C3RemoteSensorLauncher");
        flags = flags.or(F_NO_FIRES);
        ammoType = AmmoType.T_C3_REMOTE_SENSOR;
        cost = 400000;
        criticals = 3;
        tankslots = 1;
        tonnage = 4;
        rackSize = 1;
        damage = 0;
        bv = 30;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        flags = flags.or(F_MECH_WEAPON).or(F_TANK_WEAPON);
        // suppveeslots = 3;
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        techAdvancement.setISAdvancement(3072, 3093, DATE_NONE);
        techAdvancement.setTechRating(RATING_E);
        techAdvancement.setAvailability( new int[] { RATING_X, RATING_X, RATING_F, RATING_X });
    }

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
            WeaponAttackAction waa, IGame game, Server server) {
        return super.getCorrectHandler(toHit, waa, game, server);
        // FIXME: Implement handler
    }
}
