/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
 */
package megamek.ai.dataset;

import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.INarcPod;
import megamek.common.UnitRole;
import megamek.common.actions.AbstractAttackAction;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.AimingMode;

/**
 * Represents an attack action by a unit.
 * @author Luana Coppio
 */
public record UnitAttackAction(
      int round, int entityId, int playerId, String type, UnitRole role, int x, int y, int facing,
      int targetPlayerId, int targetId, String targetType, UnitRole targetRole, int targetX, int targetY, int targetFacing,
      int aimingLocation, AimingMode aimingMode, int weaponId, int ammoId, boolean airToAir, boolean airToGround,
      boolean groundToGround, boolean groundToAir, double toHit, int turnsToHit, int spotterId) {

    public static UnitAttackAction fromAttackAction(AbstractAttackAction attackAction, Game game) {
        int currentRound = game.getCurrentRound();
        int entityId = -1;
        int playerId = -1;
        String type = "UNKNOWN";
        UnitRole role = UnitRole.NONE;
        int x = -1;
        int y = -1;
        int facing = -1;
        int targetPlayerId = -1;
        int targetId = -1;
        String targetType = "UNKNOWN";
        UnitRole targetRole = UnitRole.NONE;
        int targetX = -1;
        int targetY = -1;
        int targetFacing = -1;
        int aimingLoc = -1;
        AimingMode aimingMode = AimingMode.NONE;
        int weaponId = -1;
        int ammoId = -1;
        boolean ata = false;
        boolean atg = false;
        boolean gtg = false;
        boolean gta = false;
        double toHit = 0;
        int turnsToHit = 0;
        int spotterId = -1;

        var attacker = attackAction.getEntity(game);
        if (attacker != null) {
            entityId = attacker.getId();
            playerId = attacker.getOwnerId();
            type = attacker.getClass().getSimpleName();
            role = attacker.getRole() == null ? UnitRole.NONE : attacker.getRole();
            if (attacker.isDeployed() && attacker.getPosition() != null) {
                x = attacker.getPosition().getX();
                y = attacker.getPosition().getY();
            }
            facing = attacker.getFacing();
        }

        var target = attackAction.getTarget(game);
        if (target != null) {
            targetId = target.getId();
            targetType = target.getClass().getSimpleName();
            if (!(target instanceof INarcPod) && target.getPosition() != null) {
                targetX = target.getPosition().getX();
                targetY = target.getPosition().getY();
            }
            if (target instanceof Entity entity) {
                targetPlayerId = entity.getOwnerId();
                targetRole = entity.getRole() == null ? UnitRole.NONE : entity.getRole();
                targetFacing = entity.getFacing();
            }
        }

        if (attackAction instanceof ArtilleryAttackAction artilleryAttackAction) {
            if (!artilleryAttackAction.getSpotterIds().isEmpty()) {
                spotterId = artilleryAttackAction.getSpotterIds().get(0);
            }
            turnsToHit = artilleryAttackAction.getTurnsTilHit();
            toHit = artilleryAttackAction.toHit(game).getValue();
            ammoId = artilleryAttackAction.getAmmoId();
        } else if (attackAction instanceof WeaponAttackAction weaponAttackAction) {
            toHit = weaponAttackAction.toHit(game).getValue();
            aimingLoc = weaponAttackAction.getAimedLocation();
            aimingMode = weaponAttackAction.getAimingMode();
            ammoId = weaponAttackAction.getAmmoId();
            ata = weaponAttackAction.isAirToAir(game);
            atg = weaponAttackAction.isAirToGround(game);
            gta = weaponAttackAction.isGroundToAir(game);
            gtg = !ata && !gta && !atg;
            weaponId = weaponAttackAction.getWeaponId();
        }

        return new UnitAttackAction(
              currentRound, entityId, playerId, type, role, x, y, facing,
              targetPlayerId, targetId, targetType, targetRole, targetX, targetY, targetFacing,
              aimingLoc, aimingMode, weaponId, ammoId, ata, atg, gtg, gta, toHit, turnsToHit, spotterId
        );
    }
}
