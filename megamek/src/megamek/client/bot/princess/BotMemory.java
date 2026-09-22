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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import megamek.client.bot.princess.FightMemory.OddsRecord;
import megamek.client.bot.princess.UnitBehavior.BehaviorType;
import megamek.client.bot.princess.UnitMemory.MoveRecord;
import megamek.common.annotations.Nullable;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.logging.MMLogger;

/**
 * Everything a bot has learned or decided that it will need again on a later turn, in one place.
 *
 * <p>Princess and CASPAR decide most things from the board as it stands, but some answers are not on the board:
 * where the enemy has been, which of my units were shot at while running away, which building a platoon set out
 * for two rounds ago. Those used to be loose fields on {@link Princess}. They live here so that a new feature that
 * needs to remember something has an obvious home, and so that "what did this unit do last turn" has one place to
 * be asked.</p>
 *
 * <p>What belongs here is what the bot <em>learned or decided</em>. What it was <em>told</em> (hold position, fall
 * back, a strategic target) stays with the orders on {@link Princess}, and working caches that are thrown away
 * every phase stay in {@link FireControlState} and {@link PathRankerState}. A fact that a human player or the
 * server would also need does not belong in a bot's memory at all; it belongs in the game.</p>
 *
 * <p>Princess owns the memory and CASPAR inherits it. It is written from the bot's own turn handling only, never
 * from the precognition thread.</p>
 */
public class BotMemory {
    private static final MMLogger LOGGER = MMLogger.create(BotMemory.class);

    private UnitBehavior unitBehaviorTracker = new UnitBehavior();
    private EnemyTracker enemyTracker;
    private SwarmContext swarmContext;
    private List<HeatMap> enemyHeatMaps = new ArrayList<>();
    private HeatMap friendlyHeatMap;

    private final Set<Integer> crippledUnitIds = new HashSet<>();
    // Read while firing plans are built and written at the end of the turn, so it keeps the concurrent set it had
    // as a field on Princess.
    private final Set<Integer> attackedWhileFleeingIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Integer> scootingUnitIds = new HashSet<>();

    private final Map<Integer, UnitMemory> unitMemories = new HashMap<>();
    private final Map<Integer, FightMemory> fightMemories = new HashMap<>();

    // region What each of my units is doing this round, and the waypoints it is following

    public UnitBehavior getUnitBehaviorTracker() {
        return unitBehaviorTracker;
    }

    /** Starts the behaviour tracker afresh, as at the start of a game. */
    void resetUnitBehaviorTracker() {
        unitBehaviorTracker = new UnitBehavior();
    }

    // endregion

    // region What is known about the enemy and about where my own force is

    /**
     * @return the running threat assessment of the enemy, or {@code null} until the bot has initialized
     */
    public @Nullable EnemyTracker getEnemyTracker() {
        return enemyTracker;
    }

    void setEnemyTracker(EnemyTracker enemyTracker) {
        this.enemyTracker = enemyTracker;
    }

    /**
     * @return where my force is centred and how it is clustered, or {@code null} until the bot has initialized
     */
    public @Nullable SwarmContext getSwarmContext() {
        return swarmContext;
    }

    void setSwarmContext(SwarmContext swarmContext) {
        this.swarmContext = swarmContext;
    }

    /**
     * @return one heat map per opposing team, tracking where that team's units have been; empty until the bot has
     *       initialized
     */
    public List<HeatMap> getEnemyHeatMaps() {
        return enemyHeatMaps;
    }

    void setEnemyHeatMaps(List<HeatMap> enemyHeatMaps) {
        this.enemyHeatMaps = enemyHeatMaps;
    }

    /**
     * @return the heat map of where my own team has been, or {@code null} until the bot has initialized
     */
    public @Nullable HeatMap getFriendlyHeatMap() {
        return friendlyHeatMap;
    }

    void setFriendlyHeatMap(HeatMap friendlyHeatMap) {
        this.friendlyHeatMap = friendlyHeatMap;
    }

    // endregion

    // region Forced withdrawal: who is crippled, and who was shot at while running

    /**
     * Replaces the list of my units that are crippled and so withdrawing under the forced withdrawal rule.
     *
     * @param unitIds the ids of every unit of mine that is crippled now
     */
    void setCrippledUnits(Collection<Integer> unitIds) {
        crippledUnitIds.clear();
        crippledUnitIds.addAll(unitIds);
    }

    public boolean isCrippled(int unitId) {
        return crippledUnitIds.contains(unitId);
    }

    /**
     * @return a snapshot of my crippled units that later changes to the memory do not touch
     */
    public Set<Integer> crippledUnitIds() {
        return Set.copyOf(crippledUnitIds);
    }

    /**
     * Notes that one of my units was attacked while it was already withdrawing, which frees it to return fire for
     * the rest of the game.
     *
     * @param unitId the unit that was attacked
     *
     * @return {@code true} if this is news, {@code false} if it was already known
     */
    boolean rememberAttackedWhileFleeing(int unitId) {
        return attackedWhileFleeingIds.add(unitId);
    }

    public boolean wasAttackedWhileFleeing(int unitId) {
        return attackedWhileFleeingIds.contains(unitId);
    }

    // endregion

    // region Shoot and scoot: who has started for the fallback hex and must keep going

    void rememberScooting(int unitId) {
        scootingUnitIds.add(unitId);
    }

    void forgetScooting(int unitId) {
        scootingUnitIds.remove(unitId);
    }

    void forgetAllScooting() {
        scootingUnitIds.clear();
    }

    public boolean isScooting(int unitId) {
        return scootingUnitIds.contains(unitId);
    }

    // endregion

    // region What each of my units did in earlier rounds

    /**
     * @param unitId the unit to look up
     *
     * @return the memory page for this unit, started empty if the unit has none yet
     */
    public UnitMemory unit(int unitId) {
        return unitMemories.computeIfAbsent(unitId, UnitMemory::new);
    }

    /**
     * Notes the move the bot has just chosen for one of its units.
     *
     * @param path     the chosen path
     * @param round    the current game round
     * @param behavior what the bot had the unit doing when it chose the path, or {@code null} if it never worked
     *                 that out this round
     */
    void rememberMove(MovePath path, int round, @Nullable BehaviorType behavior) {
        // The hex the unit stands in, not the path's own start: a path reports the hex of its first step, which
        // is already one hex on for a walking unit and nothing at all for a unit that stays put.
        MoveRecord move = new MoveRecord(round, path.getEntity().getPosition(), path.getFinalCoords(), behavior);
        unit(path.getEntity().getId()).rememberMove(move);
        LOGGER.debug("[BotMemory] round {}: {} moved {} to {} while {}", round, path.getEntity().getDisplayName(),
              move.from(), move.to(), behavior);
    }

    /**
     * Drops the pages of units that are no longer in the game, so the memory does not grow with the casualty list.
     *
     * @param game the game to check each remembered unit against
     */
    void forgetUnitsNoLongerInGame(Game game) {
        int pagesBefore = unitMemories.size();
        unitMemories.keySet().removeIf(unitId -> game.getEntity(unitId) == null);
        int pagesDropped = pagesBefore - unitMemories.size();
        if (pagesDropped > 0) {
            LOGGER.debug("[BotMemory] forgot {} unit(s) no longer in the game; {} remembered", pagesDropped,
                  unitMemories.size());
        }
    }

    // endregion

    // region Infantry vs. infantry actions in progress

    /**
     * @param buildingId the building being fought over
     *
     * @return what is remembered of the action in that building, or {@code null} if the bot has noted none
     */
    public @Nullable FightMemory fight(int buildingId) {
        return fightMemories.get(buildingId);
    }

    /**
     * Notes this round's strengths in a running action, starting the page for it if this is the first look.
     *
     * @param buildingId   the building being fought over
     * @param buildingName the name of the building, for the log
     * @param record       the strengths as the bot sees them this round
     */
    void rememberFightOdds(int buildingId, String buildingName, OddsRecord record) {
        // In the round an action starts the bot looks before the defender has declared, so the defence reads as
        // nothing and the odds as limitless. Noting that would make the first real figure count as a fall.
        boolean isBothSidesCommitted = (record.attackerPoints() > 0) && (record.defenderPoints() > 0);
        if (!isBothSidesCommitted) {
            LOGGER.debug("[BotMemory] round {}: action in {} not noted yet: {} against {}, one side has not "
                  + "committed", record.round(), buildingName, record.attackerPoints(), record.defenderPoints());
            return;
        }
        FightMemory fight = fightMemories.computeIfAbsent(buildingId, FightMemory::new);
        fight.rememberOdds(record);
        LOGGER.debug("[BotMemory] round {}: action in {} stands at {} against {}; {} round(s) noted",
              record.round(), buildingName, record.attackerPoints(), record.defenderPoints(), fight.roundsNoted());
    }

    /**
     * Drops the pages of actions that have ended, so a second assault on the same building starts a fresh page
     * instead of inheriting the trend of the first.
     *
     * @param runningBuildingIds the buildings that still have an action the bot has a stake in
     */
    void forgetFightsExcept(Collection<Integer> runningBuildingIds) {
        int pagesBefore = fightMemories.size();
        fightMemories.keySet().retainAll(runningBuildingIds);
        int pagesDropped = pagesBefore - fightMemories.size();
        if (pagesDropped > 0) {
            LOGGER.debug("[BotMemory] forgot {} finished action(s); {} still running", pagesDropped,
                  fightMemories.size());
        }
    }

    // endregion
}
