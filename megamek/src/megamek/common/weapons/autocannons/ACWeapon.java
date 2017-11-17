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
 * Created on Sep 25, 2004
 *
 */
package megamek.common.weapons.autocannons;

import megamek.common.AmmoType;
import megamek.common.BattleForceElement;
import megamek.common.IGame;
import megamek.common.Mounted;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.ACAPHandler;
import megamek.common.weapons.ACCaselessHandler;
import megamek.common.weapons.ACFlakHandler;
import megamek.common.weapons.ACFlechetteHandler;
import megamek.common.weapons.ACIncendiaryHandler;
import megamek.common.weapons.ACTracerHandler;
import megamek.common.weapons.ACWeaponHandler;
import megamek.common.weapons.AmmoWeapon;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.RapidfireACWeaponHandler;
import megamek.server.Server;

/**
 * @author Andrew Hunter N.B. This class is overriden for AC/2, AC/5, AC/10,
 *         AC/10, NOT ultras/LB/RAC. (No difference between ACWeapon and
 *         AmmoWeapon except the ability to use special ammos (precision, AP,
 *         etc.) )
 */
public abstract class ACWeapon extends AmmoWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 1537808266032711407L;

    public ACWeapon() {
        super();

        flags = flags.or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_MECH_WEAPON)
                .or(F_AERO_WEAPON).or(F_TANK_WEAPON);
        ammoType = AmmoType.T_AC;
        explosive = true; // when firing incendiary ammo
        atClass = CLASS_AC;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.IGame,
     * megamek.server.Server)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, IGame game, Server server) {

        AmmoType atype = (AmmoType) game.getEntity(waa.getEntityId()).getEquipment(waa.getWeaponId()).getLinked()
                .getType();

        Mounted weapon = game.getEntity(waa.getEntityId()).getEquipment(waa.getWeaponId());

        if (weapon.curMode().equals("Rapid")) {
            RapidfireACWeaponHandler ah = new RapidfireACWeaponHandler(toHit, waa, game, server);
            if (weapon.isKindRapidFire()) {
                ah.setKindRapidFire(true);
            } else {
                ah.setKindRapidFire(false);
            }
            return ah;
        }
        if (atype.getMunitionType() == AmmoType.M_ARMOR_PIERCING) {
            return new ACAPHandler(toHit, waa, game, server);
        }

        if (atype.getMunitionType() == AmmoType.M_FLECHETTE) {
            return new ACFlechetteHandler(toHit, waa, game, server);
        }

        if (atype.getMunitionType() == AmmoType.M_INCENDIARY_AC) {
            return new ACIncendiaryHandler(toHit, waa, game, server);
        }

        if (atype.getMunitionType() == AmmoType.M_TRACER) {
            return new ACTracerHandler(toHit, waa, game, server);
        }

        if (atype.getMunitionType() == AmmoType.M_FLAK) {
            return new ACFlakHandler(toHit, waa, game, server);
        }
        
        if (atype.getMunitionType() == AmmoType.M_CASELESS) {
            return new ACCaselessHandler (toHit, waa, game, server);
        }

        return new ACWeaponHandler(toHit, waa, game, server);

    }

    @Override
    public int getDamage() {
        int dmg = super.getDamage();
        if ((dmg != 5) && (dmg != 2)) {
            return dmg;
        }
        GameOptions options = getGameOptions();
        if (options == null) {
            return dmg;
        }
        if (options.getOption(OptionsConstants.ADVCOMBAT_INCREASED_AC_DMG).booleanValue()) {
            dmg++;
        }
        return dmg;
    }

    public double getBattleForceDamage(int range) {
        double damage = 0;
        if (range <= getLongRange()) {
            damage = getRackSize();
            if (range == BattleForceElement.SHORT_RANGE && getMinimumRange() > 0) {
                damage = adjustBattleForceDamageForMinRange(damage);
            }
        }
        return damage / 10.0;
    }

    @Override
    public int getBattleForceClass() {
        return BFCLASS_AC;
    }
}
