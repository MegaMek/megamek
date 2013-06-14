/* MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons;

import megamek.common.Entity;
import megamek.common.Mounted;
import megamek.common.TechConstants;
import megamek.common.AmmoType;
import megamek.common.IGame;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Jay Lawson
 */
public class ArtilleryBayWeapon extends AmmoBayWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 8756042527483383101L;

    /**
     * 
     */
    public ArtilleryBayWeapon() {
        super();
        // tech levels are a little tricky
        this.techLevel.put(3071, TechConstants.T_ALL);
        this.flags = flags.or(F_ARTILLERY);
        this.name = "Artillery Bay";
        this.setInternalName(this.name);
        this.heat = 0;
        this.damage = DAMAGE_VARIABLE;
        this.shortRange = 6;
        this.mediumRange = 12;
        this.longRange = 20;
        this.extremeRange = 25;
        this.tonnage = 0.0f;
        this.bv = 0;
        this.cost = 0;
        this.atClass = CLASS_ARTILLERY;
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
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        Entity ae = game.getEntity(waa.getEntityId());
        AmmoType atype = new AmmoType();
        boolean useHoming = false;
        for (int wId : ae.getEquipment(waa.getWeaponId()).getBayWeapons()) {
            Mounted bayW = ae.getEquipment(wId);
            // check the currently loaded ammo
            Mounted bayWAmmo = bayW.getLinked();
            atype = (AmmoType) bayWAmmo.getType();
            if (atype.getMunitionType() == AmmoType.M_HOMING) {
                useHoming = true;
            }
        }
        if (useHoming) {
            if (game.getPhase() == IGame.Phase.PHASE_FIRING) {
                return new ArtilleryBayWeaponDirectHomingHandler(toHit, waa,
                        game, server);
            }
            return new ArtilleryBayWeaponIndirectHomingHandler(toHit, waa,
                    game, server);
        } else if (game.getPhase() == IGame.Phase.PHASE_FIRING) {
            return new ArtilleryBayWeaponDirectFireHandler(toHit, waa, game,
                    server);
        } else {
            return new ArtilleryBayWeaponIndirectFireHandler(toHit, waa, game,
                    server);
        }
    }
}
