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
package megamek.common.weapons.mortars;

import megamek.common.AmmoType;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.HexTarget;
import megamek.common.Mounted;
import megamek.common.SimpleTechLevel;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AmmoWeapon;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.VGLWeaponHandler;
import megamek.server.GameManager;

/**
 * @author Sebastian Brocks
 * @since Sep 24, 2004
 */
public abstract class VehicularGrenadeLauncherWeapon extends AmmoWeapon {
    private static final long serialVersionUID = 3343394645568467135L;

    public VehicularGrenadeLauncherWeapon() {
        super();
      
        heat = 1;
        damage = 0;
        ammoType = AmmoType.T_VGL;
        rackSize = 1;
        minimumRange = 0;
        shortRange = 1;
        mediumRange = 1;
        longRange = 1;
        extremeRange = 1;
        tonnage = 0.5;
        criticals = 1;
        flags = flags.or(F_MECH_WEAPON).or(F_PROTO_WEAPON).or(F_TANK_WEAPON).or(F_AERO_WEAPON)
                .or(F_BALLISTIC).or(F_ONESHOT).or(F_VGL);
        explosive = false;
        bv = 15;
        cost = 10000;
        rulesRefs = "315, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_C)
                .setAvailability(RATING_D, RATING_E, RATING_F, RATING_E)
                .setISAdvancement(DATE_NONE, DATE_PS, 3080, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setClanAdvancement(DATE_NONE, DATE_PS, 3080, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

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
        return new VGLWeaponHandler(toHit, waa, game, manager);
    }
    
    public static Targetable getTargetHex(Mounted weapon, int weaponID) {
        Entity owner = weapon.getEntity();
        int facing;
        
        if (owner.isSecondaryArcWeapon(weaponID)) {
            facing = owner.getSecondaryFacing();
        } else {
            facing = owner.getFacing();
        }
        
        facing = (facing + weapon.getFacing()) % 6;
        
        // attempt to target first the "correct" automatic coordinates.
        Coords c = owner.getPosition().translated(facing);
        if (owner.getGame().getBoard().contains(c)) {
            return new HexTarget(c, Targetable.TYPE_HEX_CLEAR);
        }
        
        // then one hex clockwise
        c = owner.getPosition().translated((facing + 1) % 6);
        if (owner.getGame().getBoard().contains(c)) {
            return new HexTarget(c, Targetable.TYPE_HEX_CLEAR);
        }
        
        // then one hex counterclockwise
        c = owner.getPosition().translated((facing - 1) % 6);
        if (owner.getGame().getBoard().contains(c)) {
            return new HexTarget(c, Targetable.TYPE_HEX_CLEAR);
        }
        
        // default to the "correct" coordinates even though they're off board
        c = owner.getPosition().translated(facing);
        return new HexTarget(c, Targetable.TYPE_HEX_CLEAR);
    }
}
