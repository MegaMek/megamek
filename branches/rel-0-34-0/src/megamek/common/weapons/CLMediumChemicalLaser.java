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
public class CLMediumChemicalLaser extends AmmoWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 322396740172378519L;

    public CLMediumChemicalLaser() {
        this.techLevel = TechConstants.T_CLAN_EXPERIMENTAL;
        this.name = "Medium Chem Laser";
        this.setInternalName("CLMediumChemicalLaser");
        this.setInternalName("CLMediumChemLaser");
        this.heat = 2;
        this.damage = DAMAGE_VARIABLE;
        this.rackSize = 5;
        this.ammoType = AmmoType.T_CHEMICAL_LASER;
        this.minimumRange = WEAPON_NA;
        this.shortRange = 2;
        this.mediumRange = 4;
        this.longRange = 6;
        this.extremeRange = 8;
        this.waterShortRange = 1;
        this.waterMediumRange = 2;
        this.waterLongRange = 3;
        this.waterExtremeRange = 3;
        this.tonnage = 1.0f;
        this.criticals = 1;
        this.flags |= F_ENERGY | F_DIRECT_FIRE;
        this.bv = 37;
        this.cost = 300000;
        this.shortAV = 7;
        this.maxRange = RANGE_SHORT;
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
