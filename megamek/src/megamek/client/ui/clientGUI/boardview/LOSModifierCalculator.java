/*
 * Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.clientGUI.boardview;

import java.util.List;

import megamek.common.Hex;
import megamek.common.LosEffects;
import megamek.common.Player;
import megamek.common.ToHitData;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.EntityVisibilityUtils;
import megamek.common.units.Mek;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.server.SmokeCloud;

/**
 * Computes to-hit modifiers for the ruler/LOS tool without requiring actual Entity objects. Mirrors the fire phase
 * calculation from {@code ComputeTerrainMods} for terrain, attacker hex, target hex, water partial cover, and entity
 * state modifiers.
 *
 * <p>Extracted from {@code RulerDialog} to allow independent testing and reduce that class's size.</p>
 */
final class LOSModifierCalculator {

    private LOSModifierCalculator() {
        // utility class
    }

    /**
     * Computes LOS modifiers using the entity-based code path, identical to the fire phase. Uses
     * {@code LosEffects.calculateLOS(Game, Entity, Targetable)} which builds a complete AttackInfo from entity
     * properties (including infantry flags, water depth, altitude, etc.).
     *
     * @param game     the current game state
     * @param attacker the attacking entity
     * @param target   the target entity
     *
     * @return a formatted string of the to-hit modifier total and breakdown
     */
    static String computeEntityBasedModifiers(Game game, Entity attacker, Entity target) {
        LosEffects losEffects = LosEffects.calculateLOS(game, attacker, target);
        ToHitData thd = losEffects.losModifiers(game);

        if (thd.getValue() == TargetRoll.IMPOSSIBLE) {
            return thd.getDesc();
        }

        // Attacker hex terrain modifiers
        Hex attackerHex = game.getBoard().getHex(attacker.getPosition());
        if (attackerHex != null) {
            addAttackerTerrainModifiers(thd, attackerHex);
        }

        // Target hex terrain modifiers
        Hex targetHex = game.getBoard().getHex(target.getPosition());
        if (targetHex != null) {
            int targetRelHeight = target.relHeight() + 1;
            addTargetTerrainModifiers(thd, targetHex, targetRelHeight, game);
        }

        // Water partial cover for Mek targets
        if ((targetHex != null) && (target instanceof Mek)) {
            int targetRelHeight = target.relHeight() + 1;
            addWaterPartialCover(thd, losEffects, targetHex, targetRelHeight);
        }

        // Target entity state modifiers directly from the known target entity,
        // rather than picking the first entity at the hex (which may be wrong in multi-unit hexes)
        int hexDistance = attacker.getPosition().distance(target.getPosition());
        addKnownTargetEntityStateModifiers(thd, losEffects, target, hexDistance);

        String result = "";
        if (thd.getValue() != TargetRoll.IMPOSSIBLE) {
            result = thd.getValue() + " = ";
        }
        result += thd.getDesc();
        return result;
    }

    /**
     * Computes the combined to-hit modifiers for a hypothetical attack, including LOS modifiers (intervening terrain),
     * attacker hex terrain, target hex terrain, and water partial cover. Mirrors the fire phase calculation from
     * {@code ComputeTerrainMods} but without requiring actual Entity objects. Movement TMMs are excluded.
     *
     * <p>Heights are dynamically adjusted for entity state (hull-down, prone) by checking actual
     * entities at each hex, so the calculation stays accurate even if state changed after the Ruler was opened.</p>
     *
     * @param game           the current game state
     * @param attackerPos    the attacker hex coordinates
     * @param targetPos      the target hex coordinates
     * @param attackerHeight the attacker's TW height
     * @param targetHeight   the target's TW height
     * @param attackerIsMek  whether the attacker is a Mek
     * @param targetIsMek    whether the target is a Mek
     *
     * @return a formatted string of the to-hit modifier total and breakdown
     */
    static String computeFullModifiers(Game game, Coords attackerPos, Coords targetPos,
          int attackerHeight, int targetHeight, boolean attackerIsMek, boolean targetIsMek) {
        return computeFullModifiers(game, attackerPos, targetPos, attackerHeight, targetHeight,
              attackerIsMek, targetIsMek, false, false);
    }

    /**
     * Computes the combined to-hit modifiers for a hypothetical attack, with altitude unit support.
     */
    static String computeFullModifiers(Game game, Coords attackerPos, Coords targetPos,
          int attackerHeight, int targetHeight, boolean attackerIsMek, boolean targetIsMek,
          boolean attackerIsAltitude, boolean targetIsAltitude) {
        return computeFullModifiers(game, attackerPos, targetPos, attackerHeight, targetHeight,
              attackerIsMek, targetIsMek, attackerIsAltitude, targetIsAltitude, null);
    }

    /**
     * Computes the combined to-hit modifiers for a hypothetical attack, with altitude unit support and double-blind
     * visibility filtering. When {@code localPlayer} is non-null, enemy entity state (prone, immobile, hull-down,
     * stuck) is only revealed for units the local player has fully seen.
     */
    static String computeFullModifiers(Game game, Coords attackerPos, Coords targetPos,
          int attackerHeight, int targetHeight, boolean attackerIsMek, boolean targetIsMek,
          boolean attackerIsAltitude, boolean targetIsAltitude, @Nullable Player localPlayer) {
        // LosEffects needs the physical (non-hull-down) heights to correctly detect partial
        // cover, matching the real game where Mek.height() doesn't change for hull-down.
        // The hull-down modifier (+2) is applied separately via addTargetEntityStateModifiers.
        int losAttackerHeight = attackerHeight;
        int losTargetHeight = targetHeight;
        if (attackerIsMek && isMekHullDownAt(game, attackerPos, localPlayer)) {
            losAttackerHeight += 1;
        }
        if (targetIsMek && isMekHullDownAt(game, targetPos, localPlayer)) {
            losTargetHeight += 1;
        }

        LosEffects.AttackInfo attackInfo = buildAttackInfo(game, attackerPos, targetPos,
              losAttackerHeight, losTargetHeight, attackerIsMek, targetIsMek,
              attackerIsAltitude, targetIsAltitude);
        LosEffects losEffects = LosEffects.calculateLos(game, attackInfo);
        ToHitData thd = losEffects.losModifiers(game);

        // If LOS is blocked, no point adding terrain modifiers
        if (thd.getValue() == TargetRoll.IMPOSSIBLE) {
            return thd.getDesc();
        }

        // Attacker hex terrain modifiers (matching Compute.getAttackerTerrainModifier)
        Hex attackerHex = game.getBoard().getHex(attackerPos);
        if (attackerHex != null) {
            addAttackerTerrainModifiers(thd, attackerHex);
        }

        // Target hex terrain modifiers (matching Compute.getTargetTerrainModifier)
        Hex targetHex = game.getBoard().getHex(targetPos);
        if (targetHex != null) {
            addTargetTerrainModifiers(thd, targetHex, targetHeight, game);
        }

        // Water partial cover (matching ComputeTerrainMods lines 167-180)
        if ((targetHex != null) && targetIsMek) {
            addWaterPartialCover(thd, losEffects, targetHex, targetHeight);
        }

        // Target entity state modifiers (prone, immobile, hull down, stuck) from visible
        // entities on the board at the target hex (filtered by double-blind visibility)
        int hexDistance = attackerPos.distance(targetPos);
        addTargetEntityStateModifiers(thd, losEffects, game, targetPos, hexDistance, localPlayer);

        String result = "";
        if (thd.getValue() != TargetRoll.IMPOSSIBLE) {
            result = thd.getValue() + " = ";
        }
        result += thd.getDesc();
        return result;
    }

    /**
     * Builds an AttackInfo for the Ruler tool, matching the calculation in {@code LosEffects.calculateLOS()}.
     *
     * <p>The height parameters (h1, h2) are TW unit heights from the height text fields,
     * auto-populated from entity state (e.g., Mek = 2, hull-down Mek = 1, VTOL at elev 5 = 6). These are converted to
     * code-internal absHeight by subtracting 1 (TW to code conversion) and adding the hex ground level.</p>
     */
    static LosEffects.AttackInfo buildAttackInfo(Game game, Coords c1, Coords c2, int h1, int h2,
          boolean attackerIsMek, boolean targetIsMek) {
        return buildAttackInfo(game, c1, c2, h1, h2, attackerIsMek, targetIsMek, false, false);
    }

    /**
     * Builds an AttackInfo for the Ruler tool, matching the calculation in {@code LosEffects.calculateLOS()}.
     *
     * <p>The height parameters (h1, h2) are TW unit heights from the height spinners,
     * or altitude values for airborne aerospace units. These are converted to code-internal absHeight via
     * {@link LOSHeightCalculation#toAbsoluteHeight}.</p>
     */
    static LosEffects.AttackInfo buildAttackInfo(Game game, Coords c1, Coords c2, int h1, int h2,
          boolean attackerIsMek, boolean targetIsMek,
          boolean attackerIsAltitude, boolean targetIsAltitude) {
        LosEffects.AttackInfo attackInfo = new LosEffects.AttackInfo();
        attackInfo.attackPos = c1;
        attackInfo.targetPos = c2;
        attackInfo.attackerIsMek = attackerIsMek;
        attackInfo.targetIsMek = targetIsMek;

        // attackHeight/targetHeight = intrinsic unit height (how tall, not position)
        // Mek.height() = 1 (code 0-indexed; TW = 2 levels), non-Mek = 0 (TW = 1 level)
        attackInfo.attackHeight = attackerIsMek ? 1 : 0;
        attackInfo.targetHeight = targetIsMek ? 1 : 0;

        Hex attackerHex = game.getBoard().getHex(c1);
        Hex targetHex = game.getBoard().getHex(c2);

        // h1/h2 are TW heights (1-indexed) or altitude for aero units. Use toAbsoluteHeight to convert.
        attackInfo.attackAbsHeight = LOSHeightCalculation.toAbsoluteHeight(
              h1, attackerHex.getLevel(), attackerIsAltitude);
        attackInfo.targetAbsHeight = LOSHeightCalculation.toAbsoluteHeight(
              h2, targetHex.getLevel(), targetIsAltitude);

        // Set water state flags (matching LosEffects.calculateLOS entity-based logic)
        boolean attackerHasWater = attackerHex.containsTerrain(Terrains.WATER)
              && (attackerHex.depth() > 0);
        boolean targetHasWater = targetHex.containsTerrain(Terrains.WATER)
              && (targetHex.depth() > 0);

        attackInfo.attUnderWater = attackerHasWater
              && (attackInfo.attackAbsHeight < attackerHex.getLevel());
        attackInfo.attInWater = attackerHasWater
              && (attackInfo.attackAbsHeight == attackerHex.getLevel());
        attackInfo.attOnLand = !(attackInfo.attUnderWater || attackInfo.attInWater);

        attackInfo.targetUnderWater = targetHasWater
              && (attackInfo.targetAbsHeight < targetHex.getLevel());
        attackInfo.targetInWater = targetHasWater
              && (attackInfo.targetAbsHeight == targetHex.getLevel());
        attackInfo.targetOnLand = !(attackInfo.targetUnderWater || attackInfo.targetInWater);

        return attackInfo;
    }

    /**
     * Holds the LOS modifier results for all three rule modes (Standard, Diagrammed, Dead Zone), computed from both
     * attacker and target perspectives.
     *
     * @param standardAttacker   attacker POV result under Standard LOS rules
     * @param standardTarget     target POV result under Standard LOS rules
     * @param diagrammedAttacker attacker POV result under Diagrammed LOS rules
     * @param diagrammedTarget   target POV result under Diagrammed LOS rules
     * @param deadZoneAttacker   attacker POV result under Dead Zone LOS rules
     * @param deadZoneTarget     target POV result under Dead Zone LOS rules
     */
    record LOSComparison(String standardAttacker, String standardTarget,
          String diagrammedAttacker, String diagrammedTarget,
          String deadZoneAttacker, String deadZoneTarget) {
    }

    /**
     * Computes LOS modifiers under all three rule modes (Standard, Diagrammed, Dead Zone) by temporarily toggling game
     * options. Safe to call on the EDT since the ruler is a UI-only tool and no game logic runs concurrently during
     * computation.
     *
     * @param game               the current game state
     * @param attackerPos        the attacker hex coordinates
     * @param targetPos          the target hex coordinates
     * @param attackerHeight     the attacker's TW height
     * @param targetHeight       the target's TW height
     * @param attackerIsMek      whether the attacker is a Mek
     * @param targetIsMek        whether the target is a Mek
     * @param attackerIsAltitude whether the attacker uses altitude (aerospace)
     * @param targetIsAltitude   whether the target uses altitude (aerospace)
     *
     * @return a record containing all six result strings (attacker/target for each mode)
     */
    static LOSComparison computeAllModes(Game game, Coords attackerPos, Coords targetPos,
          int attackerHeight, int targetHeight, boolean attackerIsMek, boolean targetIsMek,
          boolean attackerIsAltitude, boolean targetIsAltitude) {
        return computeAllModes(game, attackerPos, targetPos, attackerHeight, targetHeight,
              attackerIsMek, targetIsMek, attackerIsAltitude, targetIsAltitude, null);
    }

    /**
     * Computes LOS comparison across all three mutually-exclusive rule modes, with double-blind visibility filtering
     * for entity state modifiers.
     */
    static LOSComparison computeAllModes(Game game, Coords attackerPos, Coords targetPos,
          int attackerHeight, int targetHeight, boolean attackerIsMek, boolean targetIsMek,
          boolean attackerIsAltitude, boolean targetIsAltitude, @Nullable Player localPlayer) {
        IOption losOption = game.getOptions().getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS1);
        IOption deadZoneOption = game.getOptions().getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_DEAD_ZONES);
        boolean originalLos = losOption.booleanValue();
        boolean originalDeadZone = deadZoneOption.booleanValue();

        try {
            // Standard: both off
            losOption.setValue(false);
            deadZoneOption.setValue(false);
            String standardAttacker = computeFullModifiers(game, attackerPos, targetPos,
                  attackerHeight, targetHeight, attackerIsMek, targetIsMek,
                  attackerIsAltitude, targetIsAltitude, localPlayer);
            String standardTarget = computeFullModifiers(game, targetPos, attackerPos,
                  targetHeight, attackerHeight, targetIsMek, attackerIsMek,
                  targetIsAltitude, attackerIsAltitude, localPlayer);

            // Diagrammed: LOS1 on, Dead Zone off
            losOption.setValue(true);
            deadZoneOption.setValue(false);
            String diagrammedAttacker = computeFullModifiers(game, attackerPos, targetPos,
                  attackerHeight, targetHeight, attackerIsMek, targetIsMek,
                  attackerIsAltitude, targetIsAltitude, localPlayer);
            String diagrammedTarget = computeFullModifiers(game, targetPos, attackerPos,
                  targetHeight, attackerHeight, targetIsMek, attackerIsMek,
                  targetIsAltitude, attackerIsAltitude, localPlayer);

            // Dead Zone: LOS1 off, Dead Zone on
            losOption.setValue(false);
            deadZoneOption.setValue(true);
            String deadZoneAttacker = computeFullModifiers(game, attackerPos, targetPos,
                  attackerHeight, targetHeight, attackerIsMek, targetIsMek,
                  attackerIsAltitude, targetIsAltitude, localPlayer);
            String deadZoneTarget = computeFullModifiers(game, targetPos, attackerPos,
                  targetHeight, attackerHeight, targetIsMek, attackerIsMek,
                  targetIsAltitude, attackerIsAltitude, localPlayer);

            return new LOSComparison(standardAttacker, standardTarget,
                  diagrammedAttacker, diagrammedTarget,
                  deadZoneAttacker, deadZoneTarget);
        } finally {
            losOption.setValue(originalLos);
            deadZoneOption.setValue(originalDeadZone);
        }
    }

    /**
     * Checks if a Mek at the given hex is hull-down.
     *
     * @param game   the current game state
     * @param hexPos the hex coordinates to check
     *
     * @return true if a Mek at the hex is hull-down
     */
    static boolean isMekHullDownAt(Game game, Coords hexPos) {
        return isMekHullDownAt(game, hexPos, null);
    }

    /**
     * Checks if a Mek at the given hex is hull-down, respecting the local player's visibility under double-blind rules.
     * If {@code localPlayer} is null, no visibility filtering is applied.
     */
    static boolean isMekHullDownAt(Game game, Coords hexPos, @Nullable Player localPlayer) {
        List<Entity> entities = game.getEntitiesVector(hexPos);
        for (Entity entity : entities) {
            if (!isVisibleToLocalPlayer(game, entity, localPlayer)) {
                continue;
            }
            if ((entity instanceof Mek) && entity.isHullDown()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the entity is fully visible (not a sensor return) to the local player. Returns true when
     * {@code localPlayer} is null (no filtering requested).
     */
    private static boolean isVisibleToLocalPlayer(Game game, Entity entity, @Nullable Player localPlayer) {
        if (localPlayer == null) {
            return true;
        }
        if (!EntityVisibilityUtils.detectedOrHasVisual(localPlayer, game, entity)) {
            return false;
        }
        return !EntityVisibilityUtils.onlyDetectedBySensors(localPlayer, entity);
    }

    /**
     * Adds attacker hex terrain modifiers. Mirrors {@code Compute.getAttackerTerrainModifier()}.
     */
    private static void addAttackerTerrainModifiers(ToHitData thd, Hex attackerHex) {
        int screenLevel = attackerHex.terrainLevel(Terrains.SCREEN);
        if (screenLevel > 0) {
            thd.addModifier(screenLevel + 1, "attacker in screen(s)");
        }
    }

    /**
     * Adds target hex terrain modifiers. Mirrors {@code Compute.getTargetTerrainModifier()} for the subset of modifiers
     * computable without an Entity object.
     *
     * @param thd             the to-hit data to append modifiers to
     * @param targetHex       the target's hex
     * @param targetRelHeight the target's relative height (elevation + unit height)
     * @param game            the current game state (for game options)
     */
    private static void addTargetTerrainModifiers(ToHitData thd, Hex targetHex,
          int targetRelHeight, Game game) {
        // Woods/Jungle in target hex
        boolean hasWoods = targetHex.containsTerrain(Terrains.WOODS)
              || targetHex.containsTerrain(Terrains.JUNGLE);
        int foliageElev = targetHex.terrainLevel(Terrains.FOLIAGE_ELEV);
        if (foliageElev == Terrain.LEVEL_NONE) {
            foliageElev = 0;
        }

        // Target is above woods if relHeight + 1 > foliage_elev
        boolean isAboveWoods = !hasWoods || (targetRelHeight + 1 > foliageElev);

        if (!isAboveWoods
              && !game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_WOODS_COVER)) {
            int woodsLevel = targetHex.terrainLevel(Terrains.WOODS);
            int jungleLevel = targetHex.terrainLevel(Terrains.JUNGLE);
            String foliageType = "woods";
            int effectiveLevel = woodsLevel;
            if (jungleLevel > woodsLevel) {
                effectiveLevel = jungleLevel;
                foliageType = "jungle";
            }
            if (effectiveLevel == 1) {
                thd.addModifier(1, "target in light " + foliageType);
            } else if (effectiveLevel >= 2) {
                thd.addModifier(effectiveLevel, "target in heavy " + foliageType);
            }
        }

        // Smoke in target hex
        boolean isAboveSmoke = (targetRelHeight + 1 > 2)
              || !targetHex.containsTerrain(Terrains.SMOKE);
        if (!isAboveSmoke) {
            int smokeLevel = targetHex.terrainLevel(Terrains.SMOKE);
            switch (smokeLevel) {
                case SmokeCloud.SMOKE_LIGHT:
                case SmokeCloud.SMOKE_LI_LIGHT:
                case SmokeCloud.SMOKE_LI_HEAVY:
                case SmokeCloud.SMOKE_CHAFF_LIGHT:
                case SmokeCloud.SMOKE_GREEN:
                    thd.addModifier(1, "target in light smoke");
                    break;
                case SmokeCloud.SMOKE_HEAVY:
                    thd.addModifier(2, "target in heavy smoke");
                    break;
                default:
                    break;
            }
        }

        // Erupting geyser
        if (targetHex.terrainLevel(Terrains.GEYSER) == 2) {
            thd.addModifier(2, "target in erupting geyser");
        }

        // Heavy industrial zone (target not above structures)
        if (targetHex.containsTerrain(Terrains.INDUSTRIAL)) {
            int ceiling = targetHex.ceiling();
            if (targetRelHeight <= ceiling) {
                thd.addModifier(1, "target in heavy industrial zone");
            }
        }

        // Screen in target hex
        int screenLevel = targetHex.terrainLevel(Terrains.SCREEN);
        if (screenLevel > 0) {
            thd.addModifier(screenLevel + 1, "target in screen(s)");
        }
    }

    /**
     * Adds water partial cover for a Mek target standing in depth 1 water. Mirrors {@code ComputeTerrainMods} lines
     * 167-180. In the fire phase, water partial cover is OR'd into existing target cover. The +1 modifier comes from
     * {@code losModifiers()} if any partial cover is already set from terrain. We only add it here when water is the
     * sole source of partial cover.
     *
     * @param thd             the to-hit data to append modifiers to
     * @param losEffects      the LOS effects (checked for existing terrain partial cover)
     * @param targetHex       the target's hex
     * @param targetRelHeight the target's relative height (elevation + unit height)
     */
    private static void addWaterPartialCover(ToHitData thd, LosEffects losEffects,
          Hex targetHex, int targetRelHeight) {
        if (!targetHex.containsTerrain(Terrains.WATER)) {
            return;
        }

        int waterDepth = targetHex.terrainLevel(Terrains.WATER);
        if (waterDepth == Terrain.LEVEL_NONE) {
            return;
        }

        // ComputeTerrainMods checks: waterLevel == 1, targEl == 0, height > 0
        // targEl = entity.relHeight() = elevation + height
        // For a Mek (height=1) in depth 1 water: elevation=-1, relHeight=0
        // targetIsMek is guaranteed by the caller's guard (Mek has height > 0)
        if ((waterDepth == 1) && (targetRelHeight == 0)) {
            boolean terrainCoverAlreadyApplied = losEffects.getTargetCover() != LosEffects.COVER_NONE;
            if (!terrainCoverAlreadyApplied) {
                thd.addModifier(1, "target has partial cover (water)");
            }
        }
    }

    /**
     * Adds target entity state modifiers (prone, immobile, hull down, stuck) from a known target entity. Used by the
     * entity-based path where we already know which entity is the target, avoiding the wrong-entity bug in multi-unit
     * hexes. These mirror the fire phase modifiers from {@code ComputeTargetToHitMods} and {@code ComputeTerrainMods}.
     *
     * @param thd          the to-hit data to append modifiers to
     * @param losEffects   the LOS effects (used for hull down partial cover check)
     * @param targetEntity the target entity to check state on
     * @param distance     the hex distance between attacker and target
     */
    private static void addKnownTargetEntityStateModifiers(ToHitData thd, LosEffects losEffects,
          Entity targetEntity, int distance) {
        // Prone: -2 if adjacent (distance <= 1), +1 at range (distance > 1)
        if (targetEntity.isProne()) {
            if (distance <= 1) {
                thd.addModifier(-2, "target prone (adjacent)");
            } else {
                thd.addModifier(1, "target prone (range)");
            }
        }

        // Immobile: -4
        if (targetEntity.isImmobile()) {
            thd.addModifier(-4, "target immobile");
        }

        // Hull Down: +2 for Meks with partial cover
        if (targetEntity.isHullDown() && (targetEntity instanceof Mek)) {
            if (losEffects.getTargetCover() > LosEffects.COVER_NONE) {
                thd.addModifier(2, "target hull down");
            }
        }

        // Stuck in swamp: -2
        if (targetEntity.isStuck()) {
            thd.addModifier(-2, "target stuck in swamp");
        }
    }

    /**
     * Adds target entity state modifiers from the first entity found at the target hex. Used by the manual path where
     * no specific target entity is known.
     */
    private static void addTargetEntityStateModifiers(ToHitData thd, LosEffects losEffects,
          Game game, Coords targetPos, int distance) {
        addTargetEntityStateModifiers(thd, losEffects, game, targetPos, distance, null);
    }

    /**
     * Adds target entity state modifiers, respecting the local player's visibility under double-blind rules.
     * Entities the player cannot see (or only detects as sensor returns) are skipped to avoid revealing their
     * state (prone, immobile, hull-down, stuck) through the ruler's modifier text.
     *
     * @param localPlayer the local player for visibility filtering, or null for no filtering
     */
    static void addTargetEntityStateModifiers(ToHitData thd, LosEffects losEffects,
          Game game, Coords targetPos, int distance, @Nullable Player localPlayer) {
        List<Entity> entitiesAtTarget = game.getEntitiesVector(targetPos);
        for (Entity entity : entitiesAtTarget) {
            if (isVisibleToLocalPlayer(game, entity, localPlayer)) {
                addKnownTargetEntityStateModifiers(thd, losEffects, entity, distance);
                return;
            }
        }
    }
}
