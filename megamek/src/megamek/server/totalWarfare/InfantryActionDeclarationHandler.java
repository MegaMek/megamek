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
 * Books the declarations of an infantry vs. infantry action (TO:AR pp. 169 to 172): starting one, joining one on
 * the right side, and announcing a withdrawal. The resolution itself is {@link InfantryActionResolutionHandler}.
 */
class InfantryActionDeclarationHandler extends AbstractTWRuleHandler {
    private static final MMLogger LOGGER = MMLogger.create(InfantryActionDeclarationHandler.class);

    private static final int ACTION_STARTS = 5630;
    private static final int REINFORCES_ATTACKERS = 5640;
    private static final int REINFORCES_DEFENDERS = 5641;
    private static final int NO_DEFENDERS = 5645;

    private final InfantryActionTracker tracker;
    private final InfantryActionReporter reporter;

    InfantryActionDeclarationHandler(TWGameManager gameManager, InfantryActionTracker tracker) {
        super(gameManager);
        this.tracker = tracker;
        this.reporter = new InfantryActionReporter(gameManager);
    }

    /**
     * Applies a declaration: a withdrawal, a unit joining a running action, or a new action.
     *
     * @param action the declaration
     */
    void process(InfantryCombatAction action) {
        Entity entity = getGame().getEntity(action.getEntityId());
        Entity targetEntity = getGame().getEntity(action.getTargetId());
        if (!(entity instanceof Infantry unit)) {
            LOGGER.debug("[InfantryAction] declaration from {} ignored: not infantry", action.getEntityId());
            return;
        }
        if (!(targetEntity instanceof AbstractBuildingEntity building)) {
            LOGGER.debug("[InfantryAction] declaration by {} ignored: target {} is not a building",
                  unit.getShortName(), action.getTargetId());
            return;
        }
        if (action.isWithdrawing()) {
            // The withdrawal itself is resolved with the action's next roll
            unit.setInfantryCombatWantsWithdrawal(true);
            LOGGER.debug("[InfantryAction] {} announces withdrawal from {}", unit.getShortName(),
                  building.getShortName());
            return;
        }
        InfantryAction combat = tracker.getCombat(building.getId());
        if (combat != null) {
            join(unit, building, combat);
            return;
        }
        List<Integer> committed = (action instanceof InitiateInfantryCombatAction initiation)
              ? initiation.getCommittedUnitIds() : List.of();
        initiate(unit, building, committed);
    }

    /**
     * A unit joins a running action on the side it belongs to: with the attackers when any defender is its enemy,
     * with the defenders when any attacker is (TO:AR p. 172, reinforcements).
     */
    private void join(Infantry unit, AbstractBuildingEntity building, InfantryAction combat) {
        boolean joinsAttackers = joinsAttackers(unit, combat);
        tracker.addReinforcement(building.getId(), unit, joinsAttackers);
        Report report = new Report(joinsAttackers ? REINFORCES_ATTACKERS : REINFORCES_DEFENDERS);
        report.add(building.getDisplayName());
        report.subject = unit.getId();
        addReport(report);
        reporter.reportUnitScore(unit, joinsAttackers ? null : building);
        LOGGER.debug("[InfantryAction] {} joins the {} in {}", unit.getShortName(),
              joinsAttackers ? "attackers" : "defenders", building.getShortName());
    }

    private boolean joinsAttackers(Entity unit, InfantryAction combat) {
        for (int defenderId : combat.defenderIds) {
            Entity defender = getGame().getEntity(defenderId);
            if ((defender != null) && defender.getOwner().isEnemyOf(unit.getOwner())) {
                return true;
            }
        }
        for (int attackerId : combat.attackerIds) {
            Entity attacker = getGame().getEntity(attackerId);
            if ((attacker != null) && attacker.getOwner().isEnemyOf(unit.getOwner())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Starts an action: the building's crew and every enemy infantry unit in any hex of the building defend; the
     * initiator and the friendly units it committed attack. A committed unit's own declaration turn is over, so it
     * is marked done and its turn removed.
     */
    private void initiate(Infantry initiator, AbstractBuildingEntity building, List<Integer> committedUnitIds) {
        List<Entity> defenders = new ArrayList<>();
        if (InfantryActionStrengths.hasCrewToDefend(building)) {
            defenders.add(building);
        }
        defenders.addAll(InfantryActionStrengths.enemyInfantryInside(getGame(), initiator.getOwner(), building));
        if (defenders.isEmpty()) {
            Report report = new Report(NO_DEFENDERS);
            report.add(building.getDisplayName());
            report.subject = initiator.getId();
            addReport(report);
            LOGGER.debug("[InfantryAction] {} cannot start an action in {}: nobody defends it",
                  initiator.getShortName(), building.getShortName());
            return;
        }
        tracker.addCombat(building.getId(), initiator, defenders.getFirst());
        for (int index = 1; index < defenders.size(); index++) {
            tracker.addReinforcement(building.getId(), defenders.get(index), false);
        }
        List<Entity> attackers = new ArrayList<>();
        attackers.add(initiator);
        for (int committedId : committedUnitIds) {
            Entity committed = getGame().getEntity(committedId);
            if (canBeCommitted(committed, initiator, building)) {
                tracker.addReinforcement(building.getId(), committed, true);
                attackers.add(committed);
                endDeclarationTurn(committed);
            } else {
                LOGGER.debug("[InfantryAction] committed unit {} refused: not friendly infantry inside {} and free",
                      committedId, building.getShortName());
            }
        }
        Report report = new Report(ACTION_STARTS);
        report.addDesc(building);
        report.subject = initiator.getId();
        addReport(report);
        reporter.reportSides(attackers.stream().map(Entity::getId).toList(),
              defenders.stream().map(Entity::getId).toList(), building,
              InfantryActionStrengths.roundedUp(InfantryActionStrengths.total(attackers, null)),
              InfantryActionStrengths.roundedUp(InfantryActionStrengths.total(defenders, building)));
        LOGGER.debug("[InfantryAction] {} starts an action in {} with {} attacker(s) against {} defender(s)",
              initiator.getShortName(), building.getShortName(), attackers.size(), defenders.size());
    }

    private boolean canBeCommitted(Entity committed, Infantry initiator, AbstractBuildingEntity building) {
        boolean isFriendlyInfantry = (committed instanceof Infantry)
              && !committed.getOwner().isEnemyOf(initiator.getOwner());
        boolean isFree = (committed != null) && (committed.getInfantryCombatTargetId() == Entity.NONE);
        return isFriendlyInfantry && isFree && InfantryActionStrengths.isInside(committed, building);
    }

    /** A unit committed with the initiator has nothing left to declare this phase. */
    private void endDeclarationTurn(Entity unit) {
        boolean hasTurnToSpend = getGame().getPhase().isPreEndDeclarations() && unit.isSelectableThisTurn();
        if (hasTurnToSpend) {
            unit.setDone(true);
            getGame().removeTurnFor(unit);
            gameManager.sendTurnList();
            gameManager.entityUpdate(unit.getId());
        }
    }
}
