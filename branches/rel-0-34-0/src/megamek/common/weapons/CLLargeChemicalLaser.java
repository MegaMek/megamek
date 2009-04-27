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
public class CLLargeChemicalLaser extends AmmoWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 322396740172378519L;

    public CLLargeChemicalLaser() {
        this.techLevel = TechConstants.T_CLAN_EXPERIMENTAL;
        this.name = "Large Chem Laser";
        this.setInternalName("CLLargeChemicalLaser");
        this.setInternalName("CLLargeChemLaser");
        this.heat = 6;
        this.damage = DAMAGE_VARIABLE;
        this.rackSize = 1;
        this.ammoType = AmmoType.T_CHEMICAL_LASER;
        this.minimumRange = WEAPON_NA;
        this.shortRange = 5;
        this.mediumRange = 10;
        this.longRange = 15;
        this.extremeRange = 20;
        this.waterShortRange = 4;
        this.waterMediumRange = 9;
        this.waterLongRange = 14;
        this.waterExtremeRange = 16;
        this.tonnage = 5.0f;
        this.criticals = 2;
        this.flags |= F_ENERGY | F_DIRECT_FIRE;
        this.bv = 99;
        this.cost = 750000;
        this.shortAV = 16;
        this.medAV = 16;
        this.maxRange = RANGE_MED;
        this.atClass = CLASS_LASER;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     *      megamek.common.actions.WeaponAttackAction, megamek.common.Game,
     *      megamek.server.Server)
     */
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new ChemicalLaserHandler(toHit, waa, game, server);
    }
}
