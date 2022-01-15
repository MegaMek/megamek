/* MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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
import megamek.common.weapons.TeleMissileHandler;
import megamek.common.weapons.Weapon;
import megamek.server.Server;

/**
 * @author Jay Lawson
 * @since Sep 25, 2004
 */
public class AR10BayWeapon extends AmmoBayWeapon {
    private static final long serialVersionUID = 8756042527483383101L;

    public AR10BayWeapon() {
        super();
        // tech levels are a little tricky
        this.name = "AR10 Bay";
        this.setInternalName(EquipmentTypeLookup.AR10_BAY);
        addLookupName("ISAR10Bay");
        addLookupName("CLAR10Bay");
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
        this.maxRange = RANGE_SHORT;
        this.atClass = CLASS_AR10;
        this.capital = true;
        rulesRefs = "210,TM";
        techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_D, RATING_F, RATING_E, RATING_D)
                .setISAdvancement(2540, 2550, 3055, 2950, 3051)
                .setISApproximate(true, false, false, true, false)
                .setClanAdvancement(2540, 2550, 3055, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH)
                .setReintroductionFactions(F_FS, F_LC);
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
                                              Server server) {
        Mounted weapon = game.getEntity(waa.getEntityId()).getEquipment(waa.getWeaponId());
        Entity attacker = game.getEntity(waa.getEntityId());
        int rangeToTarget = attacker.getPosition().distance(waa.getTarget(game).getPosition());
        if (weapon.isInBearingsOnlyMode() && rangeToTarget >= RangeType.RANGE_BEARINGS_ONLY_MINIMUM) {
            return new CapitalMissileBearingsOnlyHandler(toHit, waa, game, server);
        } else if (weapon.curMode().equals(Weapon.MODE_CAP_MISSILE_TELE_OPERATED)) {
            return new TeleMissileHandler(toHit, waa, game, server);
        } else {  
            return new CapitalMissileBayHandler(toHit, waa, game, server);
        }
    }
    
    @Override
    public int getBattleForceClass() {
        return BFCLASS_CAPITAL_MISSILE;
    }
}
