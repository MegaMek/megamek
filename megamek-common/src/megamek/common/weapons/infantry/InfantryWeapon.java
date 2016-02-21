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
 * Created on Sep 24, 2004
 *
 */
package megamek.common.weapons.infantry;

import megamek.common.AmmoType;
import megamek.common.IGame;
import megamek.common.Mounted;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.Weapon;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public abstract class InfantryWeapon extends Weapon {
    /**
     *
     */
    private static final long serialVersionUID = -4437093890717853422L;

    protected double infantryDamage;
    protected int infantryRange;
    protected int crew;


    public InfantryWeapon() {
        super();
        damage = DAMAGE_VARIABLE;
        flags = flags.or(F_INFANTRY);
        ammoType = AmmoType.T_NA;
        shortRange = 0;
        mediumRange = 0;
        longRange = 0;
        extremeRange = 0;
        heat = 0;
        tonnage = 0.0f;
        criticals = 0;
        infantryDamage = 0;
        crew = 1;
        infantryRange = 0;
        infDamageClass = WEAPON_NA;
    }

    public double getInfantryDamage() {
        return infantryDamage;
    }

    public int getInfantryRange() {
        return infantryRange;
    }

    public int getCrew() {
        return crew;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.IGame)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, IGame game, Server server) {
        Mounted m = game.getEntity(waa.getEntityId()).getEquipment(waa.getWeaponId());
        if(null != m && m.curMode().equals("Heat")) {
            return new InfantryHeatWeaponHandler(toHit, waa, game, server);
        }
        return new InfantryWeaponHandler(toHit, waa, game, server);
    }

}
