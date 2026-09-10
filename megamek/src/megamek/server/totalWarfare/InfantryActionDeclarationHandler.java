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
package megamek.server.totalWarfare;

import java.util.ArrayList;
import java.util.List;

import megamek.common.InfantryActionDeclaration;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.actions.InfantryCombatAction;
import megamek.common.actions.InitiateInfantryCombatAction;
import megamek.common.compute.InfantryActionStrengths;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.InfantryActionTracker.InfantryAction;

/**
 * Books the declarations of an infantry vs. infantry action (TO:AR pp. 169 to 172). A player declares once per
 * turn per building in the Pre-End Declarations phase: an attacker commits units or withdraws the force, a defender
 * commits units and crew. Neither side's declaration is reported until the End Phase roll, which is the book's
 * simultaneous reveal. The resolution itself is {@link InfantryActionResolutionHandler}.
 */
class InfantryActionDeclarationHandler extends AbstractTWRuleHandler {
    private static final MMLogger LOGGER = MMLogger.create(InfantryActionDeclarationHandler.class);

    private static final int NO_DEFENDERS = 5645;

    private final InfantryActionTracker tracker;

    InfantryActionDeclarationHandler(TWGameManager gameManager, InfantryActionTracker tracker) {
        super(gameManager);
        this.tracker = tracker;
    }

    /**
     * Applies a player's declaration from the Pre-End Declarations phase.
     *
     * @param declaration the declaration
     * @param connId      the connection it came from, which must be the declaring player's
     */
    void declare(InfantryActionDeclaration declaration, int connId) {
        Player player = getGame().getPlayer(declaration.playerId());
        if ((player == null) || (player.getId() != connId)) {
            LOGGER.warn("[InfantryAction] declaration for player {} from connection {} refused",
                  declaration.playerId(), connId);
            return;
        }
        if (!(getGame().getEntity(declaration.buildingId()) instanceof AbstractBuildingEntity building)) {
            LOGGER.warn("[InfantryAction] {} declared for {}, which is not a building", player.getName(),
                  declaration.buildingId());
            return;
        }
        boolean defends = InfantryActionStrengths.defends(player, building);
        LOGGER.info("[InfantryAction] {} declares for {} as {}: units {}, crew {}, withdraw {}", player.getName(),
              building.getShortName(), defends ? "defender" : "attacker", declaration.committedUnitIds(),
              declaration.committedCrew(), declaration.withdraw());
        if (defends) {
            declareDefence(player, building, declaration);
        } else {
            declareAttack(player, building, declaration);
        }
    }

    /**
     * Applies a bot's declaration, which still arrives as an action: a unit starting or joining an action, or a
     * withdrawal.
     *
     * @param action the action
     */
    void process(InfantryCombatAction action) {
        Entity entity = getGame().getEntity(action.getEntityId());
        Entity targetEntity = getGame().getEntity(action.getTargetId());
        if (!(entity instanceof Infantry unit)) {
            LOGGER.debug("[InfantryAction] action from {} ignored: not infantry", action.getEntityId());
            return;
        }
        if (!(targetEntity instanceof AbstractBuildingEntity building)) {
            LOGGER.debug("[InfantryAction] action by {} ignored: target {} is not a building", unit.getShortName(),
                  action.getTargetId());
            return;
        }
        if (action.isWithdrawing()) {
            withdraw(unit.getOwner(), building);
            return;
        }
        List<Integer> committed = new ArrayList<>();
        committed.add(unit.getId());
        if (action instanceof InitiateInfantryCombatAction initiation) {
            committed.addAll(initiation.getCommittedUnitIds());
        }
        if (InfantryActionStrengths.defends(unit.getOwner(), building)) {
            declareDefence(unit.getOwner(), building, InfantryActionDeclaration.defending(unit.getOwnerId(),
                  building.getId(), committed, 0));
        } else {
            declareAttack(unit.getOwner(), building, InfantryActionDeclaration.attacking(unit.getOwnerId(),
                  building.getId(), committed, false));
        }
    }

    // ---------------------------------------------------------------- the attacker

    private void declareAttack(Player player, AbstractBuildingEntity building,
          InfantryActionDeclaration declaration) {
        if (declaration.withdraw()) {
            withdraw(player, building);
            return;
        }
        List<Infantry> units = committableUnits(player, building, declaration.committedUnitIds());
        if (units.isEmpty()) {
            LOGGER.debug("[InfantryAction] {} committed nothing to the attack on {}", player.getName(),
                  building.getShortName());
            return;
        }
        InfantryAction combat = tracker.getCombat(building.getId());
        if (combat == null) {
            initiate(player, building, units);
        } else {
            for (Infantry unit : units) {
                tracker.addReinforcement(building.getId(), unit, true);
                LOGGER.info("[InfantryAction] {} joins the attack on {}", unit.getShortName(),
                      building.getShortName());
            }
        }
    }

    /**
     * Starts an action: the building itself defends when it has crew to commit, together with every enemy infantry
     * unit its owner has already committed; the attacker's committed units attack.
     */
    private void initiate(Player player, AbstractBuildingEntity building, List<Infantry> attackers) {
        List<Entity> defenders = new ArrayList<>();
        if (InfantryActionStrengths.hasCrewToDefend(building)) {
            defenders.add(building);
        }
        for (Infantry inside : InfantryActionStrengths.enemyInfantryInside(getGame(), player, building)) {
            boolean committedToDefend = (inside.getInfantryCombatTargetId() == building.getId())
                  && !inside.isInfantryCombatAttacker();
            if (committedToDefend) {
                defenders.add(inside);
            }
        }
        if (defenders.isEmpty()) {
            Report report = new Report(NO_DEFENDERS);
            report.add(building.getDisplayName());
            report.subject = attackers.getFirst().getId();
            addReport(report);
            LOGGER.info("[InfantryAction] {} cannot start an action in {}: nobody defends it", player.getName(),
                  building.getShortName());
            return;
        }
        tracker.addCombat(building.getId(), attackers.getFirst(), defenders.getFirst());
        for (int index = 1; index < attackers.size(); index++) {
            tracker.addReinforcement(building.getId(), attackers.get(index), true);
        }
        for (int index = 1; index < defenders.size(); index++) {
            tracker.addReinforcement(building.getId(), defenders.get(index), false);
        }
        LOGGER.info("[InfantryAction] {} starts an action in {} with {} attacker(s) against {} defender(s)",
              player.getName(), building.getShortName(), attackers.size(), defenders.size());
    }

    /** The whole attacking force withdraws; the roll is still made this End Phase (TO:AR p. 172). */
    private void withdraw(Player player, AbstractBuildingEntity building) {
        InfantryAction combat = tracker.getCombat(building.getId());
        if (combat == null) {
            LOGGER.debug("[InfantryAction] {} withdraws from {}, but no action is running there",
                  player.getName(), building.getShortName());
            return;
        }
        for (int attackerId : combat.attackerIds) {
            Entity attacker = getGame().getEntity(attackerId);
            if ((attacker != null) && (attacker.getOwnerId() == player.getId())) {
                attacker.setInfantryCombatWantsWithdrawal(true);
            }
        }
        LOGGER.info("[InfantryAction] {} withdraws the attack on {}", player.getName(), building.getShortName());
    }

    // ---------------------------------------------------------------- the defender

    /**
     * The defender commits infantry and crew. Committed infantry is flagged as defending the building even before an
     * action exists, so an attacker declaring later in the same phase finds it; the crew count goes on the building.
     */
    private void declareDefence(Player player, AbstractBuildingEntity building,
          InfantryActionDeclaration declaration) {
        InfantryAction combat = tracker.getCombat(building.getId());
        for (Infantry unit : committableUnits(player, building, declaration.committedUnitIds())) {
            if (combat != null) {
                tracker.addReinforcement(building.getId(), unit, false);
            } else {
                unit.setInfantryCombatTargetId(building.getId());
                unit.setInfantryCombatAttacker(false);
            }
            LOGGER.info("[InfantryAction] {} defends {}", unit.getShortName(), building.getShortName());
        }
        if (declaration.committedCrew() > 0) {
            int committed = building.commitCrew(declaration.committedCrew());
            LOGGER.info("[InfantryAction] {} commits {} crew to the defence of {}: {} committed in all, "
                        + "crew hits while committed {}", player.getName(), committed, building.getShortName(),
                  building.getCommittedCrew(), building.getCrew().getHits());
            gameManager.entityUpdate(building.getId());
        }
    }

    // ---------------------------------------------------------------- shared

    /** The declared units that are the player's infantry, inside the building, and not already in an action. */
    private List<Infantry> committableUnits(Player player, AbstractBuildingEntity building, List<Integer> unitIds) {
        List<Infantry> units = new ArrayList<>();
        for (int unitId : unitIds) {
            Entity entity = getGame().getEntity(unitId);
            boolean ownInfantry = (entity instanceof Infantry) && (entity.getOwnerId() == player.getId());
            boolean free = (entity != null) && (entity.getInfantryCombatTargetId() == Entity.NONE);
            if (ownInfantry && free && InfantryActionStrengths.isInside(entity, building)) {
                units.add((Infantry) entity);
            } else {
                LOGGER.debug("[InfantryAction] unit {} refused for {}: not {}'s free infantry inside", unitId,
                      building.getShortName(), player.getName());
            }
        }
        return units;
    }

    /** Clears every defender's commitment that no attack followed this turn. */
    void clearUnansweredDefences() {
        for (Entity entity : new ArrayList<>(getGame().getEntitiesVector())) {
            boolean unanswered = (entity instanceof AbstractBuildingEntity building)
                  && !tracker.hasCombat(building.getId());
            if (unanswered) {
                clearUnansweredDefence((AbstractBuildingEntity) entity);
            }
        }
    }

    /**
     * Clears a defender's commitment that no attack followed, so it does not linger into the next turn.
     *
     * @param building the building
     */
    void clearUnansweredDefence(AbstractBuildingEntity building) {
        for (Entity entity : getGame().getEntitiesVector()) {
            boolean committedDefender = (entity.getInfantryCombatTargetId() == building.getId())
                  && !entity.isInfantryCombatAttacker() && (entity != building);
            if (committedDefender) {
                entity.clearInfantryCombatState();
                LOGGER.debug("[InfantryAction] {} stands down: no attack came on {}", entity.getShortName(),
                      building.getShortName());
            }
        }
        if (building.getCommittedCrew() > 0) {
            building.clearCommittedCrew();
            gameManager.entityUpdate(building.getId());
            LOGGER.debug("[InfantryAction] {} releases its committed crew: no attack came",
                  building.getShortName());
        }
    }
}
