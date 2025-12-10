/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.weapons.srms;

import static megamek.common.game.IGame.LOGGER;

import java.io.Serial;

import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.units.Entity;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.MissileMineClearanceHandler;
import megamek.common.weapons.handlers.srm.SRMARADHandler;
import megamek.common.weapons.handlers.srm.SRMAXHandler;
import megamek.common.weapons.handlers.srm.SRMAntiTSMHandler;
import megamek.common.weapons.handlers.srm.SRMDeadFireHandler;
import megamek.common.weapons.handlers.srm.SRMFragHandler;
import megamek.common.weapons.handlers.srm.SRMHandler;
import megamek.common.weapons.handlers.srm.SRMInfernoHandler;
import megamek.common.weapons.handlers.srm.SRMSmokeWarheadHandler;
import megamek.common.weapons.handlers.srm.SRMTandemChargeHandler;
import megamek.common.weapons.missiles.MissileWeapon;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 */
public abstract class SRMWeapon extends MissileWeapon {
    @Serial
    private static final long serialVersionUID = 3636219178276978444L;

    public SRMWeapon() {
        super();
        ammoType = AmmoType.AmmoTypeEnum.SRM;
        atClass = CLASS_SRM;
        flags = flags.or(F_PROTO_WEAPON).or(F_ARTEMIS_COMPATIBLE);
    }

    @Override
    public double getTonnage(Entity entity, int location, double size) {
        if ((null != entity) && entity.hasETypeFlag(Entity.ETYPE_PROTOMEK)) {
            return getRackSize() * 0.25;
        } else {
            return super.getTonnage(entity, location, size);
        }
    }

    @Override
    public AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
          TWGameManager manager) {
        return getSRMHandler(toHit, waa, game, manager);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> fcs) {
        return super.getBattleForceDamage(range, fcs) * 2;
    }

    @Override
    public double getBattleForceDamage(int range, int baSquadSize) {
        return super.getBattleForceDamage(range, baSquadSize) * 2;
    }

    @Override
    public int getBattleForceClass() {
        return BF_CLASS_SRM;
    }

    @Override
    public String getSortingName() {
        if (sortingName != null) {
            return sortingName;
        } else {
            String oneShotTag = hasFlag(F_ONE_SHOT) ? "OS " : "";
            if (name.contains("I-OS")) {
                oneShotTag = "OSI ";
            }
            return "SRM " + oneShotTag + rackSize;
        }
    }

    @Nullable
    public static AttackHandler getSRMHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
          TWGameManager manager) {
        try {
            Entity entity = game.getEntity(waa.getEntityId());

            if (entity != null) {
                AmmoType ammoType = (AmmoType) entity.getEquipment(waa.getWeaponId()).getLinked().getType();
                if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_FRAGMENTATION)) {
                    return new SRMFragHandler(toHit, waa, game, manager);
                }
                if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_AX_HEAD)) {
                    return new SRMAXHandler(toHit, waa, game, manager);
                }
                if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_ANTI_TSM)) {
                    return new SRMAntiTSMHandler(toHit, waa, game, manager);
                }
                if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_INFERNO)) {
                    return new SRMInfernoHandler(toHit, waa, game, manager);
                }
                if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_DEAD_FIRE)) {
                    return new SRMDeadFireHandler(toHit, waa, game, manager);
                }
                if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_TANDEM_CHARGE)) {
                    return new SRMTandemChargeHandler(toHit, waa, game, manager);
                }
                if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_SMOKE_WARHEAD)) {
                    return new SRMSmokeWarheadHandler(toHit, waa, game, manager);
                }
                if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_MINE_CLEARANCE)) {
                    return new MissileMineClearanceHandler(toHit, waa, game, manager);
                }
                if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_ARAD)) {
                    return new SRMARADHandler(toHit, waa, game, manager);
                }
            }

            return new SRMHandler(toHit, waa, game, manager);
        } catch (EntityLoadingException ignored) {
            LOGGER.warn("Get SRN Handler - Attach Handler Received Null Entity.");
        }
        return null;

    }
}
