/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.ai.dataset;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

import megamek.common.units.Entity;
import megamek.common.game.Game;
import megamek.common.equipment.INarcPod;
import megamek.common.units.UnitRole;
import megamek.common.actions.AbstractAttackAction;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.AimingMode;

/**
 * Flexible container for unit attack data using a map-based approach with enum keys.
 *
 * @author Luana Coppio
 */
public class UnitAttack extends EntityDataMap<UnitAttack.Field> {

    /**
     * Enum defining all available unit attack fields.
     */
    public enum Field {
        ROUND,
        ENTITY_ID,
        PLAYER_ID,
        TYPE,
        ROLE,
        X,
        Y,
        FACING,
        TARGET_PLAYER_ID,
        TARGET_ID,
        TARGET_TYPE,
        TARGET_ROLE,
        TARGET_X,
        TARGET_Y,
        TARGET_FACING,
        AIMING_LOCATION,
        AIMING_MODE,
        WEAPON_ID,
        AMMO_ID,
        ATA,
        ATG,
        GTG,
        GTA,
        TO_HIT,
        TURNS_TO_HIT,
        SPOTTER_ID
    }

    /**
     * Creates an empty UnitAttackMap.
     */
    public UnitAttack() {
        super(Field.class);
    }

    /**
     * Creates a UnitAttackMap from an AbstractAttackAction.
     *
     * @param attackAction The attack action to extract data from
     * @param game         The game reference
     *
     * @return A populated UnitAttackMap
     */
    public static UnitAttack fromAttackAction(AbstractAttackAction attackAction, Game game) {
        UnitAttack map = new UnitAttack();

        // Basic game information
        map.put(Field.ROUND, game.getCurrentRound());

        // Attacker information
        Entity weaponEntity = attackAction.getEntity(game);
        Entity attacker = weaponEntity.getAttackingEntity();
        if (attacker != null) {
            map.put(Field.ENTITY_ID, attacker.getId())
                  .put(Field.PLAYER_ID, attacker.getOwnerId())
                  .put(Field.TYPE, attacker.getClass().getSimpleName())
                  .put(Field.ROLE, attacker.getRole() == null ? UnitRole.NONE : attacker.getRole());

            if (attacker.isDeployed() && attacker.getPosition() != null) {
                map.put(Field.X, attacker.getPosition().getX())
                      .put(Field.Y, attacker.getPosition().getY());
            } else {
                map.put(Field.X, -1)
                      .put(Field.Y, -1);
            }

            map.put(Field.FACING, attacker.getFacing());
        } else {
            // Default values for missing attacker
            map.put(Field.ENTITY_ID, -1)
                  .put(Field.PLAYER_ID, -1)
                  .put(Field.TYPE, "UNKNOWN")
                  .put(Field.ROLE, UnitRole.NONE)
                  .put(Field.X, -1)
                  .put(Field.Y, -1)
                  .put(Field.FACING, -1);
        }

        // Target information
        var target = attackAction.getTarget(game);
        if (target != null) {
            map.put(Field.TARGET_ID, target.getId())
                  .put(Field.TARGET_TYPE, target.getClass().getSimpleName());

            if (!(target instanceof INarcPod) && target.getPosition() != null) {
                map.put(Field.TARGET_X, target.getPosition().getX())
                      .put(Field.TARGET_Y, target.getPosition().getY());
            } else {
                map.put(Field.TARGET_X, -1)
                      .put(Field.TARGET_Y, -1);
            }

            if (target instanceof Entity entity) {
                map.put(Field.TARGET_PLAYER_ID, entity.getOwnerId())
                      .put(Field.TARGET_ROLE, firstNonNull(entity.getRole(), UnitRole.NONE))
                      .put(Field.TARGET_FACING, entity.getFacing());
            } else {
                map.put(Field.TARGET_PLAYER_ID, -1)
                      .put(Field.TARGET_ROLE, UnitRole.NONE)
                      .put(Field.TARGET_FACING, -1);
            }
        } else {
            // Default values for missing target
            map.put(Field.TARGET_ID, -1)
                  .put(Field.TARGET_TYPE, "UNKNOWN")
                  .put(Field.TARGET_X, -1)
                  .put(Field.TARGET_Y, -1)
                  .put(Field.TARGET_PLAYER_ID, -1)
                  .put(Field.TARGET_ROLE, UnitRole.NONE)
                  .put(Field.TARGET_FACING, -1);
        }

        // Default attack values
        map.put(Field.AIMING_LOCATION, -1)
              .put(Field.AIMING_MODE, AimingMode.NONE)
              .put(Field.WEAPON_ID, -1)
              .put(Field.AMMO_ID, -1)
              .put(Field.ATA, false)
              .put(Field.ATG, false)
              .put(Field.GTG, false)
              .put(Field.GTA, false)
              .put(Field.TO_HIT, 0.0)
              .put(Field.TURNS_TO_HIT, 0)
              .put(Field.SPOTTER_ID, -1);

        // Attack-specific information
        if (attackAction instanceof ArtilleryAttackAction artilleryAttackAction) {
            if (!artilleryAttackAction.getSpotterIds().isEmpty()) {
                map.put(Field.SPOTTER_ID, artilleryAttackAction.getSpotterIds().get(0));
            }
            map.put(Field.TURNS_TO_HIT, artilleryAttackAction.getTurnsTilHit())
                  .put(Field.TO_HIT, artilleryAttackAction.toHit(game).getValue())
                  .put(Field.AMMO_ID, artilleryAttackAction.getAmmoId());
        } else if (attackAction instanceof WeaponAttackAction weaponAttackAction) {
            map.put(Field.TO_HIT, weaponAttackAction.toHit(game).getValue())
                  .put(Field.AIMING_LOCATION, weaponAttackAction.getAimedLocation())
                  .put(Field.AIMING_MODE, firstNonNull(weaponAttackAction.getAimingMode(), AimingMode.NONE))
                  .put(Field.AMMO_ID, weaponAttackAction.getAmmoId())
                  .put(Field.WEAPON_ID, weaponAttackAction.getWeaponId());

            boolean ata = weaponAttackAction.isAirToAir(game);
            boolean atg = weaponAttackAction.isAirToGround(game);
            boolean gta = weaponAttackAction.isGroundToAir(game);
            boolean gtg = !ata && !gta && !atg;

            map.put(Field.ATA, ata)
                  .put(Field.ATG, atg)
                  .put(Field.GTA, gta)
                  .put(Field.GTG, gtg);
        }

        return map;
    }
}
