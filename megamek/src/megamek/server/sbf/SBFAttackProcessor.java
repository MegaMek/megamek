/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.server.sbf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import megamek.common.Player;
import megamek.common.actions.EntityAction;
import megamek.common.actions.sbf.SBFStandardUnitAttack;
import megamek.common.alphaStrike.ASRange;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFToHitData;
import megamek.logging.MMLogger;

record SBFAttackProcessor(SBFGameManager gameManager) implements SBFGameManagerHelper {
    private static final MMLogger logger = MMLogger.create(SBFAttackProcessor.class);

    boolean processAttacks(List<EntityAction> submittedActions, SBFFormation formation) {
        Optional<List<SBFStandardUnitAttack>> authoritativeActions = validateAndRebuild(submittedActions, formation);
        if (authoritativeActions.isEmpty()) {
            return false;
        }

        authoritativeActions.get().forEach(game()::addAction);
        formation.setDone(true);
        gameManager.sendUnitUpdate(formation);
        gameManager.sendPendingActions();
        gameManager.endCurrentTurn(formation);
        return true;
    }

    private Optional<List<SBFStandardUnitAttack>> validateAndRebuild(List<EntityAction> submittedActions,
          SBFFormation formation) {
        if (!game().getPhase().isFiring()) {
            logger.error("Server got attacks packet in wrong phase!");
            return Optional.empty();
        } else if (formation.hasSprintedThisTurn()) {
            logger.error("Sprinted formation cannot attack!");
            return Optional.empty();
        } else if (formation.isDone()) {
            logger.error("Formation already done!");
            return Optional.empty();
        }

        Player actingPlayer = game().getPlayer(formation.getOwnerId());
        if (actingPlayer == null) {
            logger.error("Attacking formation has no owner!");
            return Optional.empty();
        }

        Set<Integer> usedUnitNumbers = new HashSet<>();
        game().getActionsVector().stream()
              .filter(SBFStandardUnitAttack.class::isInstance)
              .map(SBFStandardUnitAttack.class::cast)
              .filter(action -> action.getEntityId() == formation.getId())
              .map(SBFStandardUnitAttack::getUnitNumber)
              .forEach(usedUnitNumbers::add);

        Set<Integer> targetIds = new HashSet<>(SBFToHitData.targetsOfFormation(formation, game()));
        List<SBFStandardUnitAttack> authoritativeActions = new ArrayList<>(submittedActions.size());
        for (EntityAction submittedAction : submittedActions) {
            if (!(submittedAction instanceof SBFStandardUnitAttack attack)
                  || (attack.getEntityId() != formation.getId())
                  || !attack.isDataValid(game())
                  || !usedUnitNumbers.add(attack.getUnitNumber())) {
                logger.error("Invalid or duplicate SBF standard attack submission!");
                return Optional.empty();
            }

            SBFFormation target = game().getFormation(attack.getTargetId()).orElse(null);
            if ((target == null) || (game().getPlayer(target.getOwnerId()) == null)
                  || !game().areHostile(target, actingPlayer)
                  || !game().isVisible(actingPlayer.getId(), target.getId())) {
                logger.error("Invalid, friendly, or hidden SBF attack target!");
                return Optional.empty();
            }

            Optional<ASRange> effectiveRange = SBFToHitData.effectiveRange(formation, target, attack.getRange());
            if (effectiveRange.isEmpty()) {
                logger.error("SBF standard attack has no legal range!");
                return Optional.empty();
            }

            authoritativeActions.add(new SBFStandardUnitAttack(formation.getId(), attack.getUnitNumber(),
                  target.getId(), effectiveRange.get()));
            targetIds.add(target.getId());
        }

        if (targetIds.size() > 2) {
            logger.error("Formation targeting too many targets!");
            return Optional.empty();
        }
        return Optional.of(authoritativeActions);
    }
}
