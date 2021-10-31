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

import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.Mounted;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.MissileMineClearanceHandler;
import megamek.common.weapons.SRMAXHandler;
import megamek.common.weapons.SRMAntiTSMHandler;
import megamek.common.weapons.SRMDeadFireHandler;
import megamek.common.weapons.SRMFragHandler;
import megamek.common.weapons.SRMHandler;
import megamek.common.weapons.SRMInfernoHandler;
import megamek.common.weapons.SRMSmokeWarheadHandler;
import megamek.common.weapons.SRMTandemChargeHandler;
import megamek.common.weapons.missiles.MissileWeapon;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public abstract class SRMWeapon extends MissileWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 3636219178276978444L;

    /**
     *
     */
    public SRMWeapon() {
        super();
        ammoType = AmmoType.T_SRM;
        atClass = CLASS_SRM;
        flags = flags.or(F_PROTO_WEAPON).or(F_ARTEMIS_COMPATIBLE);
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
            WeaponAttackAction waa, Game game, Server server) {
        AmmoType atype = (AmmoType) game.getEntity(waa.getEntityId())
                .getEquipment(waa.getWeaponId()).getLinked().getType();
        if (atype.getMunitionType() == AmmoType.M_FRAGMENTATION) {
            return new SRMFragHandler(toHit, waa, game, server);
        }
        if (atype.getMunitionType() == AmmoType.M_AX_HEAD) {
            return new SRMAXHandler(toHit, waa, game, server);
        }
        if (atype.getMunitionType() == AmmoType.M_ANTI_TSM) {
            return new SRMAntiTSMHandler(toHit, waa, game, server);
        }
        if (atype.getMunitionType() == AmmoType.M_INFERNO) {
            return new SRMInfernoHandler(toHit, waa, game, server);
        }
        if (atype.getMunitionType() == AmmoType.M_DEAD_FIRE) {
            return new SRMDeadFireHandler(toHit, waa, game, server);
        }
        if (atype.getMunitionType() == AmmoType.M_TANDEM_CHARGE) {
            return new SRMTandemChargeHandler(toHit, waa, game, server);
        }
        if (atype.getMunitionType() == AmmoType.M_SMOKE_WARHEAD) {
            return new SRMSmokeWarheadHandler(toHit, waa, game, server);
        }
        if (atype.getMunitionType() == AmmoType.M_MINE_CLEARANCE) {
            return new MissileMineClearanceHandler(toHit, waa, game, server);
        }
        return new SRMHandler(toHit, waa, game, server);

    }
    
    @Override
    public double getBattleForceDamage(int range, Mounted fcs) {
        return super.getBattleForceDamage(range, fcs) * 2;
    }
    
    @Override
    public double getBattleForceDamage(int range, int baSquadSize) {
        return super.getBattleForceDamage(range, baSquadSize) * 2;
    }
    
    @Override
    public int getBattleForceClass() {
        return BFCLASS_SRM;
    }
}
