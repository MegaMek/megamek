/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common.equipment;

import megamek.MMConstants;
import megamek.common.compute.Compute;
import megamek.common.units.Entity;

/**
 * EMP Mine Effects Table lookup based on unit type.
 * <p>
 * Thresholds per Tactical Operations: Advanced Rules (Experimental):
 * </p>
 * <ul>
 *   <li>BattleMek: 2-6 No Effect / 7-8 Interference / 9+ Shutdown</li>
 *   <li>IndustrialMek: 2-5 No Effect / 6-7 Interference / 8+ Shutdown</li>
 *   <li>ProtoMek: 2-5 No Effect / 6-8 Interference / 9+ Shutdown</li>
 *   <li>Battle Armor: 2-5 No Effect / 6-7 Interference / 8+ Shutdown</li>
 *   <li>Combat Vehicles: 2-5 No Effect / 6-7 Interference / 8+ Shutdown</li>
 *   <li>Support Vehicles: 2-4 No Effect / 5-6 Interference / 7+ Shutdown</li>
 *   <li>Aerospace/Small Craft: 2-6 No Effect / 7-8 Interference / 9+ Shutdown</li>
 *   <li>Conventional Fighters: 2-5 No Effect / 6-7 Interference / 8+ Shutdown</li>
 * </ul>
 *
 * @param noEffectMax     Maximum roll value for "No Effect" result
 * @param interferenceMax Maximum roll value for "Interference" result (above this is Shutdown)
 */
public record EMPMineEffectsTable(int noEffectMax, int interferenceMax) {

    // Pre-defined thresholds for each unit type
    private static final EMPMineEffectsTable BATTLE_MEK = new EMPMineEffectsTable(6, 8);
    private static final EMPMineEffectsTable INDUSTRIAL_MEK = new EMPMineEffectsTable(5, 7);
    private static final EMPMineEffectsTable PROTO_MEK = new EMPMineEffectsTable(5, 8);
    private static final EMPMineEffectsTable BATTLE_ARMOR = new EMPMineEffectsTable(5, 7);
    private static final EMPMineEffectsTable COMBAT_VEHICLE = new EMPMineEffectsTable(5, 7);
    private static final EMPMineEffectsTable SUPPORT_VEHICLE = new EMPMineEffectsTable(4, 6);
    private static final EMPMineEffectsTable AEROSPACE = new EMPMineEffectsTable(6, 8);
    private static final EMPMineEffectsTable CONVENTIONAL_FIGHTER = new EMPMineEffectsTable(5, 7);

    /**
     * Gets the appropriate effects table for the given entity.
     *
     * @param entity The entity to look up thresholds for
     *
     * @return The EMPMineEffectsTable with appropriate thresholds
     */
    public static EMPMineEffectsTable getTableFor(Entity entity) {
        if (entity.isMek()) {
            if (entity.isIndustrialMek()) {
                return INDUSTRIAL_MEK;
            }
            return BATTLE_MEK;
        }

        if (entity.isProtoMek()) {
            return PROTO_MEK;
        }

        if (entity.isBattleArmor()) {
            return BATTLE_ARMOR;
        }

        if (entity.isSupportVehicle()) {
            return SUPPORT_VEHICLE;
        }

        if (entity.isVehicle()) {
            return COMBAT_VEHICLE;
        }

        if (entity.isConventionalFighter()) {
            return CONVENTIONAL_FIGHTER;
        }

        if (entity.isAero()) {
            return AEROSPACE;
        }

        // Default to combat vehicle thresholds for unknown types
        return COMBAT_VEHICLE;
    }

    /**
     * Determines the EMP effect based on the roll value.
     *
     * @param rollValue The 2D6 roll result (with modifiers applied)
     *
     * @return The effect constant: {@link MMConstants#EMP_EFFECT_NONE}, {@link MMConstants#EMP_EFFECT_INTERFERENCE}, or
     *       {@link MMConstants#EMP_EFFECT_SHUTDOWN}
     */
    public int determineEffect(int rollValue) {
        if (rollValue <= noEffectMax) {
            return MMConstants.EMP_EFFECT_NONE;
        } else if (rollValue <= interferenceMax) {
            return MMConstants.EMP_EFFECT_INTERFERENCE;
        } else {
            return MMConstants.EMP_EFFECT_SHUTDOWN;
        }
    }

    /**
     * Rolls for EMP effect and returns the full result.
     *
     * @param entity The entity being affected
     *
     * @return EMPEffectResult containing the roll, modifier, effect, and duration
     */
    public static EMPEffectResult rollForEffect(Entity entity) {
        EMPMineEffectsTable table = getTableFor(entity);
        int modifier = entity.hasDroneOs() ? 2 : 0;
        int baseRoll = Compute.d6(2);
        int modifiedRoll = baseRoll + modifier;

        int effect = table.determineEffect(modifiedRoll);

        if (effect == MMConstants.EMP_EFFECT_NONE) {
            return EMPEffectResult.noEffect(modifiedRoll, modifier);
        }

        int durationTurns = Compute.d6();

        return switch (effect) {
            case MMConstants.EMP_EFFECT_INTERFERENCE ->
                  EMPEffectResult.interference(durationTurns, modifiedRoll, modifier);
            case MMConstants.EMP_EFFECT_SHUTDOWN -> EMPEffectResult.shutdown(durationTurns, modifiedRoll, modifier);
            default -> EMPEffectResult.noEffect(modifiedRoll, modifier);
        };
    }
}
