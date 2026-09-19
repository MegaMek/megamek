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
import java.util.List;

import megamek.client.bot.princess.FightMemory.OddsRecord;
import megamek.common.InfantryActionDeclaration;
import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.compute.InfantryActionStrengths;
import megamek.common.game.Game;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.logging.MMLogger;

/**
 * Decides a bot's infantry vs. infantry declarations for one Pre-End Declarations turn (TO:AR pp. 169 to 172): for
 * every building the bot has a stake in, whether to start an attack, reinforce or withdraw one, or answer one with
 * infantry and crew. The odds thresholds are the ones {@link InfantryCombatHelper} derives from the behaviour
 * settings, so a bot's bravery and aggression mean here what they mean elsewhere.
 *
 * <p>The planner keeps nothing itself. What it needs from earlier rounds, how the odds of an action have moved
 * since it began, it reads from the {@link BotMemory} it is given, and it notes this round's odds there first.</p>
 *
 * <p>Shared by Princess and CASPAR, which inherits it; CASPAR changes only the settings it is given.</p>
 */
public final class InfantryActionPlanner {

    private static final MMLogger LOGGER = MMLogger.create(InfantryActionPlanner.class);

    /** The to-hit penalty a bot of the lowest bravery will let its building's crew take. */
    static final int CREW_PENALTY_CAP_CAUTIOUS = 3;
    /** The to-hit penalty a bot of the highest bravery will let its building's crew take. */
    static final int CREW_PENALTY_CAP_BRAVE = 6;
    /** Odds against the defenders at which even the whole crew cannot save the building. */
    static final double HOPELESS_ODDS = 3.0;
    /** Odds at which a defence is worth its crew: the attackers no better than even. */
    private static final double EVEN_ODDS = 1.0;
    /** The share of a building's crew an attacker assumes will be committed against it. */
    private static final double CREW_SHARE_EXPECTED = 0.5;
    private static final int BRAVERY_INDEX_STEPS = 10;
    /** How many rounds running the odds must fall before an attacker calls the fight a losing one. */
    static final int SLIDING_ROUNDS = 2;

    private InfantryActionPlanner() {}

    /**
     * The declarations to send this turn, one per building the player has a stake in that calls for one.
     *
     * @param game     the game
     * @param player   the bot
     * @param behavior the bot's behaviour settings
     * @param memory   the bot's memory; this round's odds in every running action are noted in it, and actions
     *                 that have ended are dropped from it
     *
     * @return the declarations, possibly empty
     */
    public static List<InfantryActionDeclaration> plan(Game game, Player player, BehaviorSettings behavior,
          BotMemory memory) {
        List<InfantryActionDeclaration> declarations = new ArrayList<>();
        List<Integer> runningBuildingIds = new ArrayList<>();
        for (AbstractBuildingEntity building : InfantryActionStrengths.stakes(game, player)) {
            if (InfantryActionStrengths.hasActionRunning(game, building)) {
                runningBuildingIds.add(building.getId());
                memory.rememberFightOdds(building.getId(), building.getShortName(), currentOdds(game, building));
            }
            InfantryActionDeclaration declaration = InfantryActionStrengths.defends(player, building)
                  ? planDefence(game, player, building, behavior)
                  : planAttack(game, player, building, behavior, memory.fight(building.getId()));
            if (declaration != null) {
                declarations.add(declaration);
            }
        }
        memory.forgetFightsExcept(runningBuildingIds);
        return declarations;
    }

    /** The committed strengths on both sides of a running action, as they stand now. */
    static OddsRecord currentOdds(Game game, AbstractBuildingEntity building) {
        double attackers = InfantryActionStrengths.total(InfantryActionStrengths.engaged(game, building, true), null);
        double defenders = InfantryActionStrengths.total(InfantryActionStrengths.engaged(game, building, false),
              building);
        return new OddsRecord(game.getCurrentRound(), attackers, defenders);
    }

    /**
     * The attacker's declaration for one enemy building: start with every unit inside when the odds beat the
     * initiation threshold, withdraw the force when they fall below the withdrawal threshold or have slid below
     * the initiation threshold and are still falling, reinforce with units that entered since when the odds with
     * them beat the reinforcement target, otherwise nothing.
     *
     * @param fight what the bot remembers of this action from earlier rounds, or {@code null} for nothing
     *
     * @return the declaration, or {@code null} for none
     */
    static @Nullable InfantryActionDeclaration planAttack(Game game, Player player, AbstractBuildingEntity building,
          BehaviorSettings behavior, @Nullable FightMemory fight) {
        double initiationThreshold = InfantryCombatHelper.calculateInitiationThreshold(behavior.getBraveryValue());
        List<Infantry> newcomers = InfantryActionStrengths.unengagedFriendlyInfantryInside(game, player, building);
        List<Integer> newcomerIds = ids(newcomers);

        if (!InfantryActionStrengths.hasActionRunning(game, building)) {
            if (newcomers.isEmpty()) {
                return null;
            }
            if (isAlreadyTaken(game, player, building)) {
                LOGGER.debug("[InfantryAction] {} leaves {} alone: no crew and no enemy infantry are left inside to "
                      + "fight", player.getName(), building.getShortName());
                return null;
            }
            double attackers = InfantryActionStrengths.total(newcomers, null);
            double defenders = expectedDefence(game, player, building);
            double odds = odds(attackers, defenders);
            boolean worthIt = odds >= initiationThreshold;
            LOGGER.info("[InfantryAction] {} weighs an attack on {}: {} against {}, odds {} against threshold {}: {}",
                  player.getName(), building.getShortName(), number(attackers), number(defenders), number(odds),
                  number(initiationThreshold), worthIt ? "attack" : "wait");
            return worthIt ? InfantryActionDeclaration.attacking(player.getId(), building.getId(), newcomerIds,
                  false) : null;
        }

        double attackers = InfantryActionStrengths.total(InfantryActionStrengths.engaged(game, building, true), null);
        double defenders = InfantryActionStrengths.total(InfantryActionStrengths.engaged(game, building, false),
              building);
        double odds = odds(attackers, defenders);
        double withdrawalThreshold = InfantryCombatHelper.calculateWithdrawalThreshold(initiationThreshold);
        if (odds < withdrawalThreshold) {
            LOGGER.info("[InfantryAction] {} withdraws from {}: odds {} below {}", player.getName(),
                  building.getShortName(), number(odds), number(withdrawalThreshold));
            return InfantryActionDeclaration.attacking(player.getId(), building.getId(), List.of(), true);
        }
        if ((fight != null) && isLosingSlide(odds, initiationThreshold, fight)) {
            LOGGER.info("[InfantryAction] {} withdraws from {}: odds {} have fallen {} rounds running from {} at the "
                        + "start, and are below the {} it would take to start this fight today", player.getName(),
                  building.getShortName(), number(odds), SLIDING_ROUNDS, number(fight.firstRecord().odds()),
                  number(initiationThreshold));
            return InfantryActionDeclaration.attacking(player.getId(), building.getId(), List.of(), true);
        }
        if (newcomers.isEmpty()) {
            LOGGER.info("[InfantryAction] round {}: {} fights on in {}: odds {}, it would leave below {}; {} round(s) "
                        + "remembered", game.getCurrentRound(), player.getName(), building.getShortName(), number(odds),
                  number(withdrawalThreshold), (fight == null) ? 0 : fight.roundsNoted());
            return null;
        }
        double reinforcementTarget = InfantryCombatHelper.calculateReinforcementTargetRatio(initiationThreshold,
              behavior.getHyperAggressionValue());
        double oddsWithThem = odds(attackers + InfantryActionStrengths.total(newcomers, null), defenders);
        boolean worthIt = oddsWithThem >= reinforcementTarget;
        LOGGER.info("[InfantryAction] {} weighs reinforcing {}: odds {} would become {} against target {}: {}",
              player.getName(), building.getShortName(), number(odds), number(oddsWithThem),
              number(reinforcementTarget), worthIt ? "reinforce" : "hold them out");
        return worthIt ? InfantryActionDeclaration.attacking(player.getId(), building.getId(), newcomerIds, false)
              : null;
    }

    /**
     * A fight the bot would not start today and that keeps getting worse. The withdrawal threshold alone lets an
     * attack bleed for many rounds in the band between it and the initiation threshold; the trend is what tells a
     * bad run of dice from a fight that is being lost.
     */
    private static boolean isLosingSlide(double odds, double initiationThreshold, FightMemory fight) {
        boolean wouldNotStartToday = odds < initiationThreshold;
        boolean keepsFalling = fight.oddsHaveFallenFor(SLIDING_ROUNDS);
        return wouldNotStartToday && keepsFalling;
    }

    /**
     * The defender's declaration for one of its buildings under attack: every infantry unit inside, and the
     * smallest crew count that brings the attackers to even odds within the to-hit penalty the bot's bravery will
     * bear. Under the house rule that allows it, the infantry withdraw instead when even the whole crew would leave
     * the odds hopeless. A building nobody has attacked yet gets nothing: the bot waits for the attack.
     *
     * @return the declaration, or {@code null} for none
     */
    static @Nullable InfantryActionDeclaration planDefence(Game game, Player player,
          AbstractBuildingEntity building, BehaviorSettings behavior) {
        if (!InfantryActionStrengths.hasActionRunning(game, building)) {
            LOGGER.debug("[InfantryAction] {} declares nothing for {}: enemy infantry are inside but have not attacked",
                  player.getName(), building.getShortName());
            return null;
        }
        List<Infantry> insideUncommitted = InfantryActionStrengths.unengagedFriendlyInfantryInside(game, player,
              building);
        double attackers = InfantryActionStrengths.total(InfantryActionStrengths.engaged(game, building, true), null);
        double defendersNow = InfantryActionStrengths.total(InfantryActionStrengths.engaged(game, building, false),
              building) + InfantryActionStrengths.total(insideUncommitted, building);

        int committedAlready = building.getCommittedCrew();
        int available = InfantryActionStrengths.crewAvailableToCommit(building);
        double allCrewPoints = crewPointsAdded(building, committedAlready, available);
        boolean hopeless = odds(attackers, defendersNow + allCrewPoints) > HOPELESS_ODDS;
        if (hopeless && InfantryActionStrengths.canWithdrawDefence(game, player, building)) {
            LOGGER.info("[InfantryAction] {} withdraws the defence of {}: odds against it beyond {} even with every "
                  + "crew member", player.getName(), building.getShortName(), number(HOPELESS_ODDS));
            return InfantryActionDeclaration.defending(player.getId(), building.getId(), List.of(), 0, true);
        }

        int crewToCommit = crewToCommit(building, attackers, defendersNow, behavior);
        boolean anything = !insideUncommitted.isEmpty() || (crewToCommit > 0);
        LOGGER.info("[InfantryAction] {} defends {}: {} unit(s) and {} crew against {} Marine Points{}",
              player.getName(), building.getShortName(), insideUncommitted.size(), crewToCommit, number(attackers),
              anything ? "" : "; nothing left to commit");
        return anything ? InfantryActionDeclaration.defending(player.getId(), building.getId(),
              ids(insideUncommitted), crewToCommit) : null;
    }

    /**
     * The smallest number of crew that brings the attackers down to even odds, within the penalty cap; when none
     * does, the most the cap allows, since a lost building costs more than a penalty to fire.
     */
    static int crewToCommit(AbstractBuildingEntity building, double attackers, double defendersNow,
          BehaviorSettings behavior) {
        int committedAlready = building.getCommittedCrew();
        int available = InfantryActionStrengths.crewAvailableToCommit(building);
        int penaltyCap = Math.max(crewPenaltyCap(behavior), InfantryActionStrengths.crewHitsIfCommitted(building, 0));
        if (odds(attackers, defendersNow) <= EVEN_ODDS) {
            return 0;
        }
        int mostWithinCap = 0;
        for (int crew = 1; crew <= available; crew++) {
            if (InfantryActionStrengths.crewHitsIfCommitted(building, crew) > penaltyCap) {
                break;
            }
            mostWithinCap = crew;
            double defenders = defendersNow + crewPointsAdded(building, committedAlready, crew);
            if (odds(attackers, defenders) <= EVEN_ODDS) {
                return crew;
            }
        }
        return mostWithinCap;
    }

    /**
     * A building with nobody left to fight for it: its crew are gone, as they are once it has been captured, and no
     * enemy infantry stand inside. Units that stay in a building they have just taken would otherwise attack it
     * again every round and be told every round that it fell.
     */
    static boolean isAlreadyTaken(Game game, Player player, AbstractBuildingEntity building) {
        boolean hasNoCrewLeft = building.getCrew().getCurrentSize() <= 0;
        boolean hasNoEnemyInfantryInside = InfantryActionStrengths.enemyInfantryInside(game, player, building)
              .isEmpty();
        return hasNoCrewLeft && hasNoEnemyInfantryInside;
    }

    /** From {@code +3} at the lowest bravery to {@code +6} at the highest, in steps of the bravery index. */
    static int crewPenaltyCap(BehaviorSettings behavior) {
        int index = Math.max(0, Math.min(BRAVERY_INDEX_STEPS, behavior.getBraveryIndex()));
        int range = CREW_PENALTY_CAP_BRAVE - CREW_PENALTY_CAP_CAUTIOUS;
        return CREW_PENALTY_CAP_CAUTIOUS + Math.round((float) index * range / BRAVERY_INDEX_STEPS);
    }

    /** What an attacker expects to face: the enemy infantry inside and half the crew, with the building modifier. */
    private static double expectedDefence(Game game, Player player, AbstractBuildingEntity building) {
        double infantry = InfantryActionStrengths.total(InfantryActionStrengths.enemyInfantryInside(game, player,
              building), building);
        int crewExpected = (int) Math.ceil(building.getCrew().getCurrentSize() * CREW_SHARE_EXPECTED);
        return infantry + InfantryActionStrengths.crewPointsIfCommitted(building, crewExpected);
    }

    /** The Marine Points that committing more crew adds on top of those already committed. */
    private static double crewPointsAdded(AbstractBuildingEntity building, int committedAlready, int more) {
        return InfantryActionStrengths.crewPointsIfCommitted(building, committedAlready + more)
              - InfantryActionStrengths.crewPointsIfCommitted(building, committedAlready);
    }

    /** Attackers over defenders; a defence of nothing makes the odds as good as they can be. */
    private static double odds(double attackers, double defenders) {
        if (defenders <= 0) {
            return Double.MAX_VALUE;
        }
        return attackers / defenders;
    }

    private static List<Integer> ids(List<? extends Entity> units) {
        List<Integer> ids = new ArrayList<>();
        for (Entity unit : units) {
            ids.add(unit.getId());
        }
        return ids;
    }

    private static String number(double value) {
        return String.format("%.2f", value);
    }
}
