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

import megamek.common.Player;
import megamek.common.Report;
import megamek.common.actions.GhostTargetAction;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeECM;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.net.packets.InvalidPacketDataException;
import megamek.common.net.packets.Packet;
import megamek.common.rolls.Roll;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Handles Standard (TO:AR p.100-102) ghost target resolution for the PRE_FIRING phase. Extracted from TWGameManager to
 * reduce its size.
 */
class GhostTargetHelper extends AbstractTWRuleHandler {

    private static final MMLogger LOGGER = MMLogger.create(GhostTargetHelper.class);

    private final List<GhostTargetAction> pendingActions = new ArrayList<>();
    private final List<Report> pendingReports = new ArrayList<>();

    GhostTargetHelper(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * Receives a ghost target action from a client during the PRE_FIRING phase (Standard mode). Validates the action
     * and stores it for resolution at the start of the FIRING phase. On validation failure, the action is silently
     * dropped and the game continues to the next turn.
     */
    void receiveGhostTargetAction(Packet packet, int connId) throws InvalidPacketDataException {
        int entityId = packet.getIntValue(0);
        int equipmentId = packet.getIntValue(1);
        int targetId = packet.getIntValue(2);

        // Validate phase
        if (!getGame().getPhase().isPreFiring()) {
            LOGGER.error("Received ghost target action in wrong phase {}", getGame().getPhase());
            return;
        }

        // Validate Standard mode is active
        if (!getGame().usesStandardGhostTargetMode()) {
            LOGGER.error("Received ghost target action but Standard mode is not active");
            return;
        }

        Entity source = getGame().getEntity(entityId);
        if (source == null) {
            LOGGER.error("Received ghost target action for invalid entity ID {}", entityId);
            return;
        }

        // Validate connection owns this entity
        Player player = getGame().getPlayer(connId);
        if ((player == null) || (source.getOwnerId() != player.getId())) {
            LOGGER.error("Connection {} does not own entity {}", connId, entityId);
            return;
        }

        Entity target = getGame().getEntity(targetId);
        if (target == null) {
            LOGGER.error("Ghost target action targets invalid entity ID {}", targetId);
            return;
        }

        // Validate target is on the board
        if (target.isOffBoard()) {
            LOGGER.error("Ghost target action rejected: target {} is off-board", targetId);
            return;
        }

        // Derive friendliness server-side (don't trust client)
        boolean targetIsFriendly = !source.isEnemyOf(target);

        // Validate target is not conventional infantry (immune)
        if (target.isConventionalInfantry()) {
            LOGGER.error("Ghost target action rejected: target {} is conventional infantry", targetId);
            return;
        }

        // Validate target is within range (6 hexes)
        if ((source.getPosition() != null) && (target.getPosition() != null)
              && (source.getPosition().distance(target.getPosition()) > 6)) {
            LOGGER.error("Ghost target action rejected: target {} is out of range", targetId);
            return;
        }

        // Reject duplicate: same entity + equipment already has a pending action this round
        for (GhostTargetAction existing : pendingActions) {
            if ((existing.getEntityId() == entityId) && (existing.getEquipmentId() == equipmentId)) {
                LOGGER.debug("Ghost target action rejected: duplicate for entity={}, equip={}", entityId, equipmentId);
                return;
            }
        }

        LOGGER.debug("Ghost target action received: source={} (id={}), equip={}, target={} (id={}), friendly={}",
              source.getDisplayName(), entityId, equipmentId,
              target.getDisplayName(), targetId, targetIsFriendly);
        pendingActions.add(
              new GhostTargetAction(entityId, equipmentId, targetId, targetIsFriendly));
    }

    /**
     * Clears all pending ghost target actions and reports. Called at the start of PRE_FIRING to prevent accumulation
     * across rounds.
     */
    void clearPending() {
        LOGGER.info("[GhostTarget] clearPending: clearing {} actions, {} reports",
              pendingActions.size(), pendingReports.size());
        pendingActions.clear();
        pendingReports.clear();
    }

    /**
     * Resolves all pending Standard ghost target actions by rolling for each and applying bonuses. Called at the end of
     * the PRE_FIRING phase. Results are stored as pending reports to be displayed at the start of the FIRING phase.
     */
    void resolveStandardGhostTargets() {
        LOGGER.info(
              "[GhostTarget] resolveStandardGhostTargets CALLED: pendingActions={}, pendingReports={}, stacktrace={}",
              pendingActions.size(),
              pendingReports.size(),
              Thread.currentThread().getStackTrace()[2]);

        // Always clear reports from any previous round
        pendingReports.clear();

        if (pendingActions.isEmpty()) {
            return;
        }

        pendingReports.add(new Report(3636, Report.PUBLIC));

        for (GhostTargetAction action : pendingActions) {
            Entity source = getGame().getEntity(action.getEntityId());
            Entity target = getGame().getEntity(action.getTargetEntityId());

            if ((source == null) || (target == null) || source.isDestroyed()
                  || target.isDestroyed() || !source.isDeployed() || !target.isDeployed()
                  || target.isOffBoard()) {
                LOGGER.debug(
                      "Ghost target action skipped: source or target invalid/destroyed/undeployed (sourceId={}, targetId={})",
                      action.getEntityId(),
                      action.getTargetEntityId());
                continue;
            }

            // Server-side rule validation: verify equipment is ghost-target-capable and in correct mode
            if (!source.hasGhostTargetEquipment()) {
                LOGGER.debug("Ghost target action skipped: {} has no ghost target equipment",
                      source.getDisplayName());
                continue;
            }

            // Validate target is not conventional infantry
            if (target.isConventionalInfantry()) {
                LOGGER.debug("Ghost target action skipped: target {} is conventional infantry",
                      target.getDisplayName());
                continue;
            }

            // Validate target is within range
            if ((source.getPosition() != null) && (target.getPosition() != null)
                  && (source.getPosition().distance(target.getPosition()) > 6)) {
                LOGGER.debug("Ghost target action skipped: target {} is out of range",
                      target.getDisplayName());
                continue;
            }

            // ECCM suppression: can't generate if entity's hex has more enemy ECM than friendly ECCM
            if (ComputeECM.isAffectedByECM(source, source.getPosition(), source.getPosition())) {
                LOGGER.debug("Ghost target suppressed by ECCM: {}", source.getDisplayName());
                Report r = new Report(3637);
                r.subject = source.getId();
                r.addDesc(source);
                pendingReports.add(r);
                continue;
            }

            // Roll: Piloting/Driving +3 (Gunnery +3 for ProtoMeks, Anti-Mek +3 for BA), no other modifiers
            // For BA, getCrew().getPiloting() returns the Anti-Mek skill per errata
            int targetNumber = source.getCrew().getPiloting() + 3;
            if (source.hasETypeFlag(Entity.ETYPE_PROTOMEK)) {
                targetNumber = source.getCrew().getGunnery() + 3;
            }

            Roll roll = Compute.rollD6(2);
            boolean success = roll.getIntValue() >= targetNumber;

            LOGGER.debug("Ghost target roll: {} -> {} needs {}+, rolled {}, {}",
                  source.getDisplayName(), target.getDisplayName(),
                  targetNumber, roll.getIntValue(), success ? "SUCCESS" : "FAILED");

            // Check special cases for successful rolls
            boolean probeImmune = success && !action.isTargetFriendly()
                  && target.hasBAP() && !target.isStealthActive();
            boolean alreadyCapped = success
                  && ((action.isTargetFriendly() && target.getGhostTargetDefensiveBonus() >= 3)
                  || (!action.isTargetFriendly() && target.getGhostTargetOffensiveBonus() >= 3));

            String ghostType = action.isTargetFriendly() ? "Defensive" : "Offensive";

            if (probeImmune) {
                // Dedicated report: success but target is immune due to active probe
                Report r = new Report(3642);
                r.subject = source.getId();
                r.addDesc(source);
                r.addDesc(target);
                r.add(ghostType);
                r.add(targetNumber);
                r.add(roll);
                r.addDesc(target);
                pendingReports.add(r);
            } else if (alreadyCapped) {
                // Dedicated report: success but target is already at +3 max
                Report r = new Report(3643);
                r.subject = source.getId();
                r.addDesc(source);
                r.addDesc(target);
                r.add(ghostType);
                r.add(targetNumber);
                r.add(roll);
                r.addDesc(target);
                pendingReports.add(r);
            } else {
                // Normal report: success or failure
                Report r = new Report(3633);
                r.subject = source.getId();
                r.addDesc(source);
                r.addDesc(target);
                r.add(ghostType);
                r.add(targetNumber);
                r.add(roll);
                r.choose(success);
                pendingReports.add(r);

                if (success) {
                    if (action.isTargetFriendly()) {
                        target.addGhostTargetDefensiveBonus(1);
                        LOGGER.debug("Ghost target defensive bonus applied to {} (now {})",
                              target.getDisplayName(), target.getGhostTargetDefensiveBonus());
                    } else {
                        target.addGhostTargetOffensiveBonus(1);
                        LOGGER.debug("Ghost target offensive bonus applied to {} (now {})",
                              target.getDisplayName(), target.getGhostTargetOffensiveBonus());
                    }
                    gameManager.entityUpdate(target.getId());
                }
            }

            // Stealth Armor + Angel ECM: generating entity suffers +1 to own attacks
            if (success && source.isStealthActive()) {
                MiscMounted equipment = source.getMisc(action.getEquipmentId());
                if ((equipment != null) && equipment.getType().hasFlag(MiscType.F_ANGEL_ECM)) {
                    source.addGhostTargetOffensiveBonus(1);
                    gameManager.entityUpdate(source.getId());
                    Report stealthReport = new Report(3638);
                    stealthReport.subject = source.getId();
                    stealthReport.addDesc(source);
                    pendingReports.add(stealthReport);
                }
            }
        }

        pendingActions.clear();
    }

    /**
     * Adds any pending ghost target reports to the Weapon Attack Phase report section. Called at the start of the
     * FIRING phase end processing, after the phase header.
     */
    void addGhostTargetReports() {
        LOGGER.info("[GhostTarget] addGhostTargetReports CALLED: {} reports pending",
              pendingReports.size());
        if (!pendingReports.isEmpty()) {
            for (Report report : pendingReports) {
                addReport(report);
            }
            addNewLines();
            pendingReports.clear();
        }
    }
}
