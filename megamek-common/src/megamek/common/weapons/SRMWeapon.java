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
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.EquipmentType;
import megamek.common.IGame;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
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
        availRating = new int[] { EquipmentType.RATING_C,
                EquipmentType.RATING_C, EquipmentType.RATING_C };
        atClass = CLASS_SRM;
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
}
