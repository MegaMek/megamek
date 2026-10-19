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
package megamek.client.bot.princess;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.annotation.Nullable;
import megamek.common.Player;
import megamek.common.force.Force;
import megamek.common.game.Game;
import megamek.common.orders.LanceRole;
import megamek.common.orders.LanceRoles;
import megamek.common.units.Entity;
import megamek.common.util.UnitIdListParser;

/**
 * Works out which units a player can pick as a bot's priority targets, and how they are grouped: by owner, then by
 * force, with units in no force last. This is the logic behind the priority target picker, kept apart from Swing so it
 * can be tested.
 *
 * <p>The units come from the game the client holds, which already leaves out anything blind drop or double blind
 * hides from this player, so the picker cannot become a way to look at hidden forces.</p>
 */
public final class PriorityTargetChoices {

    /** What a unit's force is doing under unit orders, which the picker tags. */
    public enum ForceRole {
        NONE,
        CONVOY,
        ESCORT
    }

    /**
     * One unit the player can pick.
     *
     * @param unitId       the unit's ID
     * @param unitName     the name shown, chassis and model
     * @param ownerId      the owning player's ID
     * @param ownerName    the owning player's name
     * @param team         the owning player's team
     * @param forceId      the unit's force, or {@link Force#NO_FORCE}
     * @param forceName    the force's name, or an empty string for a unit in no force
     * @param forceRole    whether the force is a convoy or an escort under unit orders
     * @param isEnemyOfBot {@code true} when the unit is an enemy of the bot being set up; always {@code true} when the
     *                     bot is not known yet
     * @param isAlwaysTarget {@code true} for an enemy convoy unit, which the bot targets at priority 1 whatever the
     *                     player picks
     */
    public record Candidate(int unitId, String unitName, int ownerId, String ownerName, int team, int forceId,
          String forceName, ForceRole forceRole, boolean isEnemyOfBot, boolean isAlwaysTarget) {

        /**
         * @return {@code true} when the unit belongs to a force
         */
        public boolean hasForce() {
            return forceId != Force.NO_FORCE;
        }
    }

    private PriorityTargetChoices() {}

    /**
     * Lists every unit in the game as a candidate, sorted by owner, then force (units in no force last), then ID.
     *
     * @param game the game as this client sees it
     * @param bot  the bot being set up, or {@code null} for a bot that does not exist yet, whose enemies are unknown
     *
     * @return the candidates; never {@code null}
     */
    public static List<Candidate> candidatesFor(Game game, @Nullable Player bot) {
        List<Candidate> candidates = new ArrayList<>();
        for (Entity unit : game.getEntitiesVector()) {
            Player owner = game.getPlayer(unit.getOwnerId());
            if (owner == null) {
                continue;
            }
            boolean isEnemyOfBot = (bot == null) || bot.isEnemyOf(owner);
            ForceRole forceRole = forceRoleOf(unit);
            boolean isAlwaysTarget = (bot != null) && isEnemyOfBot && (forceRole == ForceRole.CONVOY);
            int forceId = unit.getForceId();
            Force force = (forceId == Force.NO_FORCE) ? null : game.getForces().getForce(forceId);
            String forceName = (force == null) ? "" : force.getName();
            if (force == null) {
                forceId = Force.NO_FORCE;
            }
            candidates.add(new Candidate(unit.getId(), unitName(unit), owner.getId(), owner.getName(), owner.getTeam(),
                  forceId, forceName, forceRole, isEnemyOfBot, isAlwaysTarget));
        }
        candidates.sort(Comparator.comparingInt(Candidate::ownerId)
              .thenComparing(candidate -> !candidate.hasForce())
              .thenComparing(Candidate::forceName)
              .thenComparingInt(Candidate::unitId));
        return candidates;
    }

    /**
     * Lists the units the bot would shoot at: every unit whose owner is an enemy of the bot. In a three-way battle that
     * is both other sides; with no teams it is everyone else. A bot not yet in the game has no team, so every unit is
     * listed. The bot's own units and its teammates' are left out, as it never fires at them.
     *
     * @param game the game as this client sees it
     * @param bot  the bot being set up, or {@code null} for a bot that does not exist yet
     *
     * @return the units it can be given as priority targets, sorted as {@link #candidatesFor(Game, Player)} sorts them
     */
    public static List<Candidate> targetsFor(Game game, @Nullable Player bot) {
        List<Candidate> targets = new ArrayList<>();
        for (Candidate candidate : candidatesFor(game, bot)) {
            if (candidate.isEnemyOfBot()) {
                targets.add(candidate);
            }
        }
        return targets;
    }

    /**
     * Whether a candidate matches what the player typed in the search box. A search made only of digits, commas and
     * dashes is read as an ID list such as {@code 3-7, 10}; anything else matches the unit's name or model, ignoring
     * case.
     *
     * @param candidate the unit
     * @param search    the typed text; blank matches everything
     *
     * @return {@code true} when the unit should be shown
     */
    public static boolean matchesSearch(Candidate candidate, String search) {
        return !matchingSearch(List.of(candidate), search).isEmpty();
    }

    /**
     * The candidates that match what the player typed, by the rules of {@link #matchesSearch}. The search is read
     * once for the whole list, so an ID list is parsed once rather than once per unit.
     *
     * @param candidates the units to filter, in display order
     * @param search     the typed text; blank matches everything
     *
     * @return the matching units, in the same order
     */
    public static List<Candidate> matchingSearch(List<Candidate> candidates, String search) {
        String trimmed = search.trim();
        if (trimmed.isEmpty()) {
            return new ArrayList<>(candidates);
        }
        List<Candidate> matches = new ArrayList<>();
        if (isIdSearch(trimmed)) {
            Set<Integer> unitIds = UnitIdListParser.parse(trimmed).unitIds();
            for (Candidate candidate : candidates) {
                if (unitIds.contains(candidate.unitId())) {
                    matches.add(candidate);
                }
            }
            return matches;
        }
        String lowerCaseSearch = trimmed.toLowerCase(Locale.ROOT);
        for (Candidate candidate : candidates) {
            if (candidate.unitName().toLowerCase(Locale.ROOT).contains(lowerCaseSearch)) {
                matches.add(candidate);
            }
        }
        return matches;
    }

    /**
     * @param search trimmed search text
     *
     * @return {@code true} when the text is an ID list rather than a name
     */
    public static boolean isIdSearch(String search) {
        return !search.isEmpty() && search.chars().allMatch(PriorityTargetChoices::isIdListCharacter);
    }

    /**
     * @param character one character of the search text
     *
     * @return {@code true} for a character an ID list such as {@code 3-7, 10} is made of
     */
    private static boolean isIdListCharacter(int character) {
        boolean isSeparator = (character == ',') || (character == '-');
        return Character.isDigit(character) || isSeparator || Character.isWhitespace(character);
    }

    private static String unitName(Entity unit) {
        String model = unit.getModel();
        return ((model == null) || model.isBlank()) ? unit.getChassis() : (unit.getChassis() + ' ' + model);
    }

    private static ForceRole forceRoleOf(Entity unit) {
        LanceRole role = LanceRoles.effectiveRole(unit);
        if (role == null) {
            return ForceRole.NONE;
        } else if (role.isConvoy()) {
            return ForceRole.CONVOY;
        } else if (role.isEscort()) {
            return ForceRole.ESCORT;
        }
        return ForceRole.NONE;
    }
}
