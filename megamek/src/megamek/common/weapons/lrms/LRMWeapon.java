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

package megamek.common.weapons.lrms;

import static megamek.common.game.IGame.LOGGER;

import java.io.Serial;

import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.AmmoType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.IGameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.MissileMineClearanceHandler;
import megamek.common.weapons.handlers.lrm.*;
import megamek.common.weapons.missiles.MissileWeapon;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 */
public abstract class LRMWeapon extends MissileWeapon {

    @Serial
    private static final long serialVersionUID = 8755275511561446251L;

    public LRMWeapon() {
        super();
        ammoType = AmmoType.AmmoTypeEnum.LRM;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        atClass = CLASS_LRM;
        flags = flags.or(F_PROTO_WEAPON).or(F_ARTEMIS_COMPATIBLE);
    }


    @Override
    public double getTonnage(Entity entity, int location, double size) {
        if ((null != entity) && entity.hasETypeFlag(Entity.ETYPE_PROTOMEK)) {
            return getRackSize() * 0.2;
        } else {
            return super.getTonnage(entity, location, size);
        }
    }

    @Override
    public AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
          TWGameManager manager) {
        return getLRMHandler(toHit, waa, game, manager);
    }

    @Override
    public int getBattleForceClass() {
        return BF_CLASS_LRM;
    }

    @Override
    public boolean hasIndirectFire() {
        return true;
    }

    @Override
    public void adaptToGameOptions(IGameOptions gameOptions) {
        super.adaptToGameOptions(gameOptions);

        // Indirect Fire
        if (gameOptions.booleanOption(OptionsConstants.BASE_INDIRECT_FIRE)) {
            addMode("");
            addMode("Indirect");
        } else {
            removeMode("");
            removeMode("Indirect");
        }
    }

    @Override
    public String getSortingName() {
        if (sortingName != null) {
            return sortingName;
        } else {
            String oneShotTag = hasFlag(F_ONE_SHOT) ? "OS " : "";
            if (name.contains("I-OS")) {
                oneShotTag = "XIOS ";
            }
            return "LRM " + oneShotTag + ((rackSize < 10) ? "0" + rackSize : rackSize);
        }
    }

    @Nullable
    public static AttackHandler getLRMHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
          TWGameManager manager) {
        try {
            AmmoType atype = (AmmoType) game.getEntity(waa.getEntityId())
                  .getEquipment(waa.getWeaponId())
                  .getLinked()
                  .getType();
            if (atype.getMunitionType().contains(AmmoType.Munitions.M_FRAGMENTATION)) {
                return new LRMFragHandler(toHit, waa, game, manager);
            }
            if (atype.getMunitionType().contains(AmmoType.Munitions.M_ANTI_TSM)) {
                return new LRMAntiTSMHandler(toHit, waa, game, manager);
            }
            if ((atype.getMunitionType().contains(AmmoType.Munitions.M_THUNDER))
                  || (atype.getMunitionType().contains(AmmoType.Munitions.M_THUNDER_ACTIVE))
                  || (atype.getMunitionType().contains(AmmoType.Munitions.M_THUNDER_AUGMENTED))
                  || (atype.getMunitionType().contains(AmmoType.Munitions.M_THUNDER_INFERNO))
                  || (atype.getMunitionType().contains(AmmoType.Munitions.M_THUNDER_VIBRABOMB))) {
                return new LRMScatterableHandler(toHit, waa, game, manager);
            }
            if (atype.getMunitionType().contains(AmmoType.Munitions.M_SWARM)) {
                return new LRMSwarmHandler(toHit, waa, game, manager);
            }
            if (atype.getMunitionType().contains(AmmoType.Munitions.M_SWARM_I)) {
                return new LRMSwarmIHandler(toHit, waa, game, manager);
            }
            if (atype.getMunitionType().contains(AmmoType.Munitions.M_DEAD_FIRE)) {
                return new LRMDeadFireHandler(toHit, waa, game, manager);
            }
            if (atype.getMunitionType().contains(AmmoType.Munitions.M_FOLLOW_THE_LEADER)) {
                return new LRMFollowTheLeaderHandler(toHit, waa, game, manager);
            }
            if (atype.getMunitionType().contains(AmmoType.Munitions.M_SMOKE_WARHEAD)) {
                return new LRMSmokeWarheadHandler(toHit, waa, game, manager);
            }
            if (atype.getMunitionType().contains(AmmoType.Munitions.M_MINE_CLEARANCE)) {
                return new MissileMineClearanceHandler(toHit, waa, game, manager);
            }
            if (atype.getMunitionType().contains(AmmoType.Munitions.M_ARAD)) {
                return new LRMARADHandler(toHit, waa, game, manager);
            }
            // Note: Incendiary mixed is handled via LRMHandler.isIncendiaryMixed()
            return new LRMHandler(toHit, waa, game, manager);
        } catch (EntityLoadingException ignored) {
            LOGGER.warn("Get LRM Handler - Attach Handler Received Null Entity.");
        }
        return null;
    }
}
