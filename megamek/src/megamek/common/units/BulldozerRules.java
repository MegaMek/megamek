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

    private BulldozerRules() {}

    /**
     * Checks whether a vehicle may declare a Clear Rubble action ending in the given hex (TacOps): it must be a vehicle
     * with a working bulldozer, the bulldozer optional rule must be enabled, and the hex must hold rubble. Returns the
     * specific reason it is illegal so the caller can log it, or null when legal.
     *
     * @param entity the unit attempting to clear rubble
     * @param hex    the hex the clearing would take place in (the unit's final hex), or null
     * @param game   the game, for the optional-rule check
     *
     * @return a human-readable reason the clear is illegal, or null if it is legal
     */
    public static @Nullable String clearRubbleIllegalReason(Entity entity, @Nullable Hex hex, Game game) {
        if (!(entity instanceof Tank clearingTank)) {
            return "clear rubble illegal - only vehicles can mount a bulldozer";
        }
        if (!clearingTank.hasWorkingBulldozer()) {
            return "clear rubble illegal - no working bulldozer";
        }
        if (!game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_BULLDOZER)) {
            return "clear rubble illegal - bulldozer optional rule is disabled";
        }
        if (!hasClearableRubble(hex)) {
            return "clear rubble illegal - hex contains no rubble (the vehicle must end its move in the rubble hex)";
        }
        return null;
    }

    /**
     * @param hex the hex to test, or null
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
