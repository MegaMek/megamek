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
package megamek.common.weapons.missiles;

import megamek.common.AmmoType;
import megamek.common.BattleForceElement;
import megamek.common.Compute;
import megamek.common.Game;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.LRMAntiTSMHandler;
import megamek.common.weapons.LRMDeadFireHandler;
import megamek.common.weapons.LRMFollowTheLeaderHandler;
import megamek.common.weapons.LRMFragHandler;
import megamek.common.weapons.LRMHandler;
import megamek.common.weapons.LRMScatterableHandler;
import megamek.common.weapons.LRMSmokeWarheadHandler;
import megamek.common.weapons.LRMSwarmHandler;
import megamek.common.weapons.LRMSwarmIHandler;
import megamek.common.weapons.SRMAXHandler;
import megamek.common.weapons.SRMAntiTSMHandler;
import megamek.common.weapons.SRMDeadFireHandler;
import megamek.common.weapons.SRMFragHandler;
import megamek.common.weapons.SRMHandler;
import megamek.common.weapons.SRMInfernoHandler;
import megamek.common.weapons.SRMSmokeWarheadHandler;
import megamek.common.weapons.SRMTandemChargeHandler;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public abstract class MMLWeapon extends MissileWeapon {

    private static final long serialVersionUID = 110779423352325731L;

    /**
     * 
     */
    public MMLWeapon() {
        super();
        this.ammoType = AmmoType.T_MML;
        this.atClass = CLASS_MML;
        flags = flags.or(F_ARTEMIS_COMPATIBLE);
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
        if (atype.hasFlag(AmmoType.F_MML_LRM)) {
            if (atype.getMunitionType() == AmmoType.M_FRAGMENTATION) {
                return new LRMFragHandler(toHit, waa, game, server);
            }
            if (atype.getMunitionType() == AmmoType.M_ANTI_TSM) {
                return new LRMAntiTSMHandler(toHit, waa, game, server);
            }
            if (atype.getMunitionType() == AmmoType.M_THUNDER
                    || atype.getMunitionType() == AmmoType.M_THUNDER_ACTIVE
                    || atype.getMunitionType() == AmmoType.M_THUNDER_AUGMENTED
                    || atype.getMunitionType() == AmmoType.M_THUNDER_INFERNO
                    || atype.getMunitionType() == AmmoType.M_THUNDER_VIBRABOMB) {
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

        return new SRMHandler(toHit, waa, game, server);
    }
    
    
    @Override
    public double getBattleForceDamage(int range, Mounted fcs) {
        int clusterRoll = 7;
        if (fcs != null && fcs.getType() instanceof MiscType) {
            if (((MiscType)fcs.getType()).hasFlag(MiscType.F_ARTEMIS)) {
                clusterRoll = 9;
            } else if (((MiscType)fcs.getType()).hasFlag(MiscType.F_ARTEMIS_PROTO)) {
                clusterRoll = 8;
            } else if (((MiscType)fcs.getType()).hasFlag(MiscType.F_ARTEMIS_V)) {
                clusterRoll = 10;
            }
        }
        double damage = Compute.calculateClusterHitTableAmount(clusterRoll, getRackSize());
        if (range == BattleForceElement.SHORT_RANGE && getMinimumRange() > 0) {
            damage = adjustBattleForceDamageForMinRange(damage);
        }
        if (range < BattleForceElement.MEDIUM_RANGE) {
            damage *= 2;
        } else if (range < BattleForceElement.LONG_RANGE) {
            damage *= 1.5;
        }
        return damage / 10.0;
    }
    
    @Override
    public int getBattleForceClass() {
        return BFCLASS_MML;
    }
    
    @Override
    public boolean hasIndirectFire() {
        return true;
    }
    
    @Override
    public void adaptToGameOptions(GameOptions gOp) {
        super.adaptToGameOptions(gOp);

        // Indirect Fire
        if (gOp.booleanOption(OptionsConstants.BASE_INDIRECT_FIRE)) {
            addMode("");
            addMode("Indirect");
        } else {
            removeMode("");
            removeMode("Indirect");
        }
    }
}
