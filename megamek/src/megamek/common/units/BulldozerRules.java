/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common.units;

import java.util.Optional;

import megamek.client.ui.Messages;
import megamek.common.Hex;
import megamek.common.Report;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.Roll;
import megamek.logging.MMLogger;

/**
 * Static helpers for bulldozer (TacOps) rules that would otherwise add logic to large server classes. Keeps the
 * resolution code in one focused place; callers (e.g. the damage manager) invoke a single method.
 */
public final class BulldozerRules {

    private static final MMLogger LOGGER = MMLogger.create(BulldozerRules.class);

    /** Report id for "the bulldozer is destroyed" (report-messages.properties). */
    private static final int BULLDOZER_DESTROYED_REPORT = 5347;

    /** Extra turns a backhoe takes vs a bulldozer to clear a rubble hex (unofficial rule). */
    public static final int BACKHOE_CLEAR_TURN_PENALTY = 4;

    private BulldozerRules() {}

    /**
     * @param entity the unit to check (a vehicle or a Mek)
     * @param game   the game, for the optional-rule checks
     *
     * @return {@code true} if this unit clears rubble using a backhoe under the unofficial rule: it has a working
     *       backhoe, no bulldozer to use instead, and both the TacOps Bulldozers and the unofficial backhoe rules are
     *       enabled. Clearing with a backhoe is slower (see {@link #BACKHOE_CLEAR_TURN_PENALTY}).
     */
    public static boolean canBackhoeClearRubble(Entity entity, Game game) {
        boolean hasOnlyBackhoe = !entity.hasWorkingBulldozer() && entity.hasWorkingBackhoe();
        boolean unofficialBackhoeRuleEnabled =
              game.getOptions().booleanOption(OptionsConstants.UNOFFICIAL_BACKHOE_CLEARS_RUBBLE);
        boolean bulldozerRuleEnabled =
              game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_BULLDOZER);
        return hasOnlyBackhoe && unofficialBackhoeRuleEnabled && bulldozerRuleEnabled;
    }

    /**
     * @param entity the unit to check
     * @param game   the game, for the optional-rule checks
     *
     * @return {@code true} if the unit may currently clear rubble - a vehicle with a working bulldozer (with the TacOps
     *       Bulldozers rule on), or with a working backhoe (with the unofficial backhoe rule on).
     */
    public static boolean canClearRubble(Entity entity, Game game) {
        if (!(entity instanceof RubbleClearer)) {
            return false;
        }
        if (entity.hasWorkingBulldozer()) {
            return game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_BULLDOZER);
        }
        return canBackhoeClearRubble(entity, game);
    }

    /**
     * @param entity the clearing unit
     * @param game   the game, for the optional-rule checks
     *
     * @return the extra turns this vehicle's clearing takes because it uses a backhoe rather than a bulldozer
     *       (unofficial: {@link #BACKHOE_CLEAR_TURN_PENALTY}), or 0 when clearing with a bulldozer
     */
    public static int extraClearingTurns(Entity entity, Game game) {
        return canBackhoeClearRubble(entity, game) ? BACKHOE_CLEAR_TURN_PENALTY : 0;
    }

    /** A rubble clear is capped at this many turns regardless of structure type (wall/ultra treated as hardened). */
    public static final int MAX_CLEAR_TURNS = 16;

    /**
     * @param rubbleLevel the RUBBLE terrain level (structure type: 1 light .. 6 ultra)
     *
     * @return the base turns a bulldozer needs to clear that rubble (2/4/8/16 by structure type, capped at 16). TacOps.
     */
    public static int clearingTurnsFor(int rubbleLevel) {
        return Math.min(1 << rubbleLevel, MAX_CLEAR_TURNS);
    }

    /**
     * @param entity the clearing unit
     * @param hex    the rubble hex, or {@code null}
     * @param game   the game, for the optional-rule checks
     *
     * @return the total turns this unit needs to clear the hex - the base time plus any backhoe penalty - or 0 when the
     *       hex holds no rubble
     */
    public static int totalClearingTurns(Entity entity, @Nullable Hex hex, Game game) {
        if (!hasClearableRubble(hex)) {
            return 0;
        }
        return clearingTurnsFor(hex.terrainLevel(Terrains.RUBBLE)) + extraClearingTurns(entity, game);
    }

    /**
     * @param entity the clearing unit
     *
     * @return the localized name of the tool the unit clears rubble with (its bulldozer, or otherwise its backhoe), for
     *       reports and prompts
     */
    public static String clearingToolName(Entity entity) {
        return Messages.getString(entity.hasWorkingBulldozer() ? "Bulldozer.tool.bulldozer" : "Bulldozer.tool.backhoe");
    }

    /**
     * Checks whether a vehicle may declare a Clear Rubble action ending in the given hex (TacOps): it must be a vehicle
     * with a working bulldozer (or, with the unofficial rule, a backhoe), the relevant optional rule(s) must be enabled,
     * and the hex must hold rubble. Returns the specific reason it is illegal so the caller can log it, or {@code null}
     * when legal.
     *
     * @param entity the unit attempting to clear rubble
     * @param hex    the hex the clearing would take place in (the unit's final hex), or {@code null}
     * @param game   the game, for the optional-rule check
     *
     * @return a human-readable reason the clear is illegal, or {@code null} if it is legal
     */
    public static @Nullable String clearRubbleIllegalReason(Entity entity, @Nullable Hex hex, Game game) {
        if (!(entity instanceof RubbleClearer)) {
            return "clear rubble illegal - only vehicles and backhoe-equipped Meks can clear rubble";
        }
        if (!game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_BULLDOZER)) {
            return "clear rubble illegal - bulldozer optional rule is disabled";
        }
        if (!entity.hasWorkingBulldozer() && !canBackhoeClearRubble(entity, game)) {
            return "clear rubble illegal - no working bulldozer (or backhoe with the unofficial rule enabled)";
        }
        if (!hasClearableRubble(hex)) {
            return "clear rubble illegal - hex contains no rubble (the vehicle must end its move in the rubble hex)";
        }
        return null;
    }

    /**
     * @param hex the hex to test, or {@code null}
     *
     * @return {@code true} if the hex exists and holds rubble a bulldozer could clear
     */
    public static boolean hasClearableRubble(@Nullable Hex hex) {
        return (hex != null) && (hex.terrainLevel(Terrains.RUBBLE) > 0);
    }

    /**
     * @param entity the vehicle to check
     * @param game   the game, for board access
     *
     * @return {@code true} if the vehicle occupies a rubble hex or is adjacent to one, so a bulldozer could drive in
     *       and clear it (TacOps). Used to gate the Clear Rubble button.
     */
    public static boolean isInOrAdjacentToClearableRubble(Entity entity, Game game) {
        Coords position = entity.getPosition();
        if (position == null) {
            return false;
        }
        Board board = game.getBoard(entity.getBoardId());
        if (hasClearableRubble(board.getHex(position))) {
            return true;
        }
        for (int direction = 0; direction < 6; direction++) {
            if (hasClearableRubble(board.getHex(position.translated(direction)))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves the bulldozer destruction check after a vehicle location takes damage (TacOps): if the location mounts a
     * working bulldozer, rolls 2D6 and, on a result of 2, destroys that bulldozer. Other results - and locations with
     * no working bulldozer - leave it intact.
     *
     * @param tank     the vehicle whose location took damage
     * @param location the location that took damage
     *
     * @return a {@link Report} describing the destroyed bulldozer if it was destroyed, otherwise empty
     */
    public static Optional<Report> rollDestructionFromLocationDamage(Tank tank, int location) {
        if (!tank.hasWorkingMisc(MiscType.F_BULLDOZER, null, location)) {
            return Optional.empty();
        }
        Roll diceRoll = Compute.rollD6(2);
        boolean destroyed = diceRoll.getIntValue() == 2;
        LOGGER.debug("[Bulldozer] {}: damage to bulldozer location {} - destruction roll {} ({})",
              tank.getShortName(), location, diceRoll.getIntValue(), destroyed ? "destroyed" : "intact");
        if (!destroyed) {
            return Optional.empty();
        }
        for (MiscMounted mounted : tank.getMisc()) {
            if (mounted.getType().hasFlag(MiscType.F_BULLDOZER) && (mounted.getLocation() == location)
                  && !mounted.isDestroyed()) {
                mounted.setHit(true);
                mounted.setDestroyed(true);
                break;
            }
        }
        Report report = new Report(BULLDOZER_DESTROYED_REPORT);
        report.subject = tank.getId();
        report.addDesc(tank);
        report.add(diceRoll);
        return Optional.of(report);
    }
}
