/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.srms;

import static megamek.common.MountedHelper.isArtemisIV;
import static megamek.common.MountedHelper.isArtemisProto;
import static megamek.common.MountedHelper.isArtemisV;

import megamek.common.AmmoType;
import megamek.common.BattleForceElement;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Mounted;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.SRMHandler;
import megamek.common.weapons.missiles.MissileWeapon;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public abstract class SRTWeapon extends MissileWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 2209880229033489588L;

    /**
     *
     */
    public SRTWeapon() {
        super();
        ammoType = AmmoType.T_SRM_TORPEDO;
        flags = flags.or(F_PROTO_WEAPON).andNot(F_AERO_WEAPON).or(F_ARTEMIS_COMPATIBLE);
    }
    
    @Override
    public double getTonnage(Entity entity, int location, double size) {
        if ((null != entity) && entity.hasETypeFlag(Entity.ETYPE_PROTOMECH)) {
            return getRackSize() * 0.25;
        } else {
            return super.getTonnage(entity, location, size);
        }
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
        return new SRMHandler(toHit, waa, game, server);
    }
    
    @Override
    public double getBattleForceDamage(int range, Mounted fcs) {
        if (range >= BattleForceElement.LONG_RANGE) {
            return 0;
        }
        if (isClan()) {
            if (rackSize == 2) {
                if (isArtemisIV(fcs) || isArtemisProto(fcs)) {
                    return 0.4;
                } else if (isArtemisV(fcs)) {
                    return 0.42;
                } else {
                    return 0.2;
                }
            } else if (rackSize == 4) {
                if (isArtemisV(fcs)) {
                    return 0.63;
                } else {
                    return 0.6;
                }
            } else {
                if (isArtemisIV(fcs) || isArtemisProto(fcs)) {
                    return 0.1;
                } else if (isArtemisV(fcs)) {
                    return 1.05;
                } else {
                    return 0.8;
                }
            }
        } else {
            if (rackSize == 2) {
                return (isArtemisIV(fcs) || isArtemisProto(fcs)) ? 0.4 : 0.2;
            } else if (rackSize == 4) {
                return 0.6;
            } else {
                return (isArtemisIV(fcs) || isArtemisProto(fcs)) ? 1 : 0.8;
            }
        }
    }
    
    @Override
    public double getBattleForceDamage(int range, int baSquadSize) {
        return super.getBattleForceDamage(range, baSquadSize) * 2;
    }
    
    @Override
    public int getBattleForceClass() {
        return BFCLASS_TORP;
    }
}
