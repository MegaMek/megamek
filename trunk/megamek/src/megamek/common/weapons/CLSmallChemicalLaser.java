/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
/*
 * Created on May 29, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.IGame;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Jason Tighe
 */
public class CLSmallChemicalLaser extends AmmoWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 322396740172378519L;

    public CLSmallChemicalLaser() {
        techLevel = TechConstants.T_CLAN_EXPERIMENTAL;
        name = "Small Chem Laser";
        setInternalName("CLSmallChemicalLaser");
        setInternalName("CLSmallChemLaser");
        heat = 1;
        damage = DAMAGE_VARIABLE;
        rackSize = 3;
        ammoType = AmmoType.T_CHEMICAL_LASER;
        minimumRange = WEAPON_NA;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        waterShortRange = 1;
        waterMediumRange = 2;
        waterLongRange = 3;
        waterExtremeRange = 3;
        tonnage = 0.5f;
        criticals = 1;
        flags = flags.or(F_DIRECT_FIRE).or(F_ENERGY);
        bv = 7;
        cost = 100000;
        shortAV = 3;
        maxRange = RANGE_SHORT;
        atClass = CLASS_LASER;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     *      megamek.common.actions.WeaponAttackAction, megamek.common.Game,
     *      megamek.server.Server)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new ChemicalLaserHandler(toHit, waa, game, server);
    }
}
