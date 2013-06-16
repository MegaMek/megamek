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
public abstract class LRMWeapon extends MissileWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 8755275511561446251L;

    public LRMWeapon() {
        super();
        ammoType = AmmoType.T_LRM;
        setModes(new String[] { "", "Indirect" });
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        atClass = CLASS_LRM;
        availRating = new int[] { EquipmentType.RATING_C,
                EquipmentType.RATING_C, EquipmentType.RATING_C };
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
            return new LRMFragHandler(toHit, waa, game, server);
        }
        if (atype.getMunitionType() == AmmoType.M_ANTI_TSM) {
            return new LRMAntiTSMHandler(toHit, waa, game, server);
        }
        if ((atype.getMunitionType() == AmmoType.M_THUNDER)
                || (atype.getMunitionType() == AmmoType.M_THUNDER_ACTIVE)
                || (atype.getMunitionType() == AmmoType.M_THUNDER_AUGMENTED)
                || (atype.getMunitionType() == AmmoType.M_THUNDER_INFERNO)
                || (atype.getMunitionType() == AmmoType.M_THUNDER_VIBRABOMB)) {
            return new LRMScatterableHandler(toHit, waa, game, server);
        }
        if (atype.getMunitionType() == AmmoType.M_SWARM) {
            return new LRMSwarmHandler(toHit, waa, game, server);
        }
        if (atype.getMunitionType() == AmmoType.M_SWARM_I) {
            return new LRMSwarmIHandler(toHit, waa, game, server);
        }
        if (atype.getMunitionType() == AmmoType.M_DEAD_FIRE) {
            return new LRMDeadFireHandler(toHit, waa, game, server);
        }
        if (atype.getMunitionType() == AmmoType.M_FOLLOW_THE_LEADER) {
            return new LRMFollowTheLeaderHandler(toHit, waa, game, server);
        }
        if (atype.getMunitionType() == AmmoType.M_SMOKE_WARHEAD) {
            return new LRMSmokeWarheadHandler(toHit, waa, game, server);
        }
        return new LRMHandler(toHit, waa, game, server);
    }
}
