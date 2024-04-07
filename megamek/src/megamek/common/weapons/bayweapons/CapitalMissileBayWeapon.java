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
import megamek.common.weapons.CapitalMissileBayHandler;
import megamek.common.weapons.CapitalMissileBearingsOnlyHandler;
import megamek.server.gameManager.*;

/**
 * @author Jay Lawson
 * @since Sep 25, 2004
 */
public class CapitalMissileBayWeapon extends AmmoBayWeapon {
    private static final long serialVersionUID = 8756042527483383101L;
    
    //There's no RAW minimum, but it can't be 0...
    public static final int CAPITAL_MISSILE_MIN_VELOCITY = 1;
    //This is the default flight speed, RAW
    public static final int CAPITAL_MISSILE_DEFAULT_VELOCITY = 50;
    //And this is useful at long bearings-only ranges, just for improved playability
    public static final int CAPITAL_MISSILE_MAX_VELOCITY = 500;

    public CapitalMissileBayWeapon() {
        super();
        // tech levels are a little tricky
        this.name = "Capital Missile Bay";
        this.setInternalName(EquipmentTypeLookup.CAPITAL_MISSILE_BAY);
        this.heat = 0;
        this.damage = DAMAGE_VARIABLE;
        this.shortRange = 12;
        this.mediumRange = 24;
        this.longRange = 40;
        this.extremeRange = 50;
        this.tonnage = 0.0;
        this.bv = 0;
        this.cost = 0;
        this.flags = flags.or(F_MISSILE);
        this.atClass = CLASS_CAPITAL_MISSILE;
        this.capital = true;
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
        Mounted weapon = game.getEntity(waa.getEntityId()).getEquipment(waa.getWeaponId());
        Entity attacker = game.getEntity(waa.getEntityId());
        int rangeToTarget = attacker.getPosition().distance(waa.getTarget(game).getPosition());
        if (weapon.isInBearingsOnlyMode()
                && rangeToTarget >= RangeType.RANGE_BEARINGS_ONLY_MINIMUM) {
            return new CapitalMissileBearingsOnlyHandler(toHit, waa, game, manager);
        } else {    
            return new CapitalMissileBayHandler(toHit, waa, game, manager);
        }
    }
    
    @Override
    public int getBattleForceClass() {
        return BFCLASS_CAPITAL_MISSILE;
    }
}
