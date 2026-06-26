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

import java.util.Vector;

import megamek.common.Hex;
import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.board.BridgeConstruction;
import megamek.common.board.Coords;
import megamek.common.equipment.BridgeLayerLogic;
import megamek.common.equipment.BridgeLayerState;
import megamek.common.equipment.MiscMounted;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.logging.MMLogger;

/**
 * Resolves the END-phase placement of bridges declared by Bridge-Layer (AVLB) equipment, TM p.242 / TW. A unit declares
 * a deployment during the Pre-End declarations phase; it must then remain stationary, and this handler places the
 * folding bridge in the hex directly in front of the unit at the end of the following turn. Extracted from
 * {@link TWGameManager} so that large class does not also carry the bridgelayer rules;
 * {@link TWGameManager#checkDeployBridges()} delegates here once per END phase.
 *
 * @author Claude Code (Opus 4.8)
 */
class AvlbDeployPhaseHandler extends AbstractTWRuleHandler {

    private static final MMLogger LOGGER = MMLogger.create(AvlbDeployPhaseHandler.class);

    AvlbDeployPhaseHandler(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * Validates an incoming Bridge-Layer (AVLB) deploy-declaration packet and, if it is well-formed, records the
     * declaration. The unit must exist, be owned by the sending player, and the referenced equipment must be a
     * bridgelayer. Kept here (rather than in {@link TWGameManager}) so the bridgelayer rules stay out of that very
     * large class; the manager only extracts the packet fields and delegates.
     *
     * @param entityId        the declaring unit's id (from the packet)
     * @param equipmentNumber the equipment index of the chosen bridgelayer mount (from the packet)
     * @param connIndex       the connection that sent the packet, validated against the unit's owner
     */
    void receiveDeployDeclaration(int entityId, int equipmentNumber, int connIndex) {
        Entity entity = getGame().getEntity(entityId);
        if (entity == null) {
            LOGGER.error("[AVLB] deploy-bridge packet for unknown unit #{}", entityId);
            return;
        }
        if (connIndex != entity.getOwnerId()) {
            LOGGER.error("[AVLB] player #{} tried to deploy a bridge with a unit owned by player #{}", connIndex,
                  entity.getOwnerId());
            return;
        }
        if (!(entity.getEquipment(equipmentNumber) instanceof MiscMounted bridgeLayer)
              || (bridgeLayer.getBridgeLayerState() == null)) {
            LOGGER.warn("[AVLB] deploy-bridge packet for {} referenced equipment #{} that is not a bridgelayer",
                  entity.getShortName(), equipmentNumber);
            return;
        }
        declareDeploy(entity, bridgeLayer);
    }

    /**
     * Records a Pre-End deployment declaration for a unit (TM p.242 / TW): the controlling player declares during the
     * Pre-End declarations phase, and the bridge is then placed at the end of the following turn if the unit stays
     * stationary. Validates eligibility, sets the pending state on the bridgelayer mount (target hex directly in front,
     * along the unit's facing), broadcasts the unit so clients show the in-progress indicator, and reports the
     * declaration.
     *
     * @param entity      the unit declaring the deployment
     * @param bridgeLayer the bridgelayer mount being deployed (the player's choice on a multi-bridge unit)
     */
    void declareDeploy(Entity entity, MiscMounted bridgeLayer) {
        if (!BridgeLayerLogic.canDeclareBridgeDeploy(entity, getGame())) {
            // canDeclareBridgeDeploy logs the specific reason at debug; nothing more to do.
            return;
        }
        BridgeLayerState bridgeState = bridgeLayer.getBridgeLayerState();
        if ((bridgeState == null) || bridgeState.isDeployed() || bridgeState.isDeployMechanismDisabled()
              || (bridgeState.getCurrentCF() <= 0)) {
            LOGGER.warn("[AVLB] {}: deploy declaration ignored - the chosen equipment is not a deployable bridgelayer",
                  entity.getShortName());
            return;
        }
        Coords target = BridgeLayerLogic.getBridgeLayerTargetCoords(entity);
        if (target == null) {
            return;
        }
        int exits = BridgeLayerLogic.getBridgeLayerExits(entity);
        bridgeState.startDeploy(target, exits, getGame().getCurrentRound());
        // Sync the unit so all clients show the in-progress (partial bridge) indicator on the target hex.
        gameManager.entityUpdate(entity.getId());
        LOGGER.info("[AVLB] {} declares a bridge deployment (bridge at location {}): target {}, exits bitmask {}; the "
                    + "bridge is placed at the end of the next turn if the unit stays stationary", entity.getShortName(),
              bridgeLayer.getLocation(), target, exits);
        Report report = new Report(4291);
        report.subject = entity.getId();
        report.addDesc(entity);
        addReport(report);
    }

    /**
     * End-phase check for every unit with a pending bridgelayer deployment: once a full stationary turn has elapsed
     * since the declaration, places the bridge if the unit stayed put and the site is still valid, or cancels the
     * deployment otherwise. TM p.242 / TW.
     */
    void checkDeployBridges() {
        for (Entity entity : getGame().getEntitiesVector()) {
            MiscMounted bridgeLayer = BridgeLayerLogic.getPendingDeployBridgeLayer(entity);
            if (bridgeLayer == null) {
                continue;
            }
            BridgeLayerState bridgeState = bridgeLayer.getBridgeLayerState();
            // The declaration turn just records intent; the unit must spend the *following* turn stationary, so the
            // bridge is not placed until a later round than the one it was declared in.
            if (bridgeState.getDeployDeclaredTurn() >= getGame().getCurrentRound()) {
                continue;
            }
            // A unit destroyed before placement, or whose deploy mechanism was crit-disabled or whose carried bridge
            // was shot away after the declaration, cannot complete the deployment.
            if (entity.isDestroyed() || entity.isDoomed() || bridgeState.isDeployMechanismDisabled()
                  || (bridgeState.getCurrentCF() <= 0)) {
                LOGGER.info("[AVLB] {} cannot complete its bridge deployment (destroyed or mechanism disabled); "
                      + "deployment cancelled", entity.getShortName());
                bridgeState.clearPendingDeploy();
                continue;
            }
            // RAW: the unit must remain stationary during the turn the bridge is placed. Any change of position cancels
            // the deployment (the half-extended bridge is retracted).
            if (entity.delta_distance != 0) {
                LOGGER.info("[AVLB] {} moved ({} hexes) instead of staying stationary; bridge deployment cancelled",
                      entity.getShortName(), entity.delta_distance);
                bridgeState.clearPendingDeploy();
                Report report = new Report(4293);
                report.subject = entity.getId();
                report.addDesc(entity);
                addReport(report);
                continue;
            }
            finishBridgeDeploy(entity, bridgeLayer, bridgeState);
        }
    }

    /**
     * Places a declared bridgelayer bridge on the board, TM p.242 / TW: lays the folding bridge in the hex in front of
     * the unit along its facing, registers it as a structure, marks the equipment spent, and updates the clients. If
     * the site became invalid while the unit waited (e.g. a structure appeared in the hex or a bank changed), the
     * deployment is cancelled instead.
     *
     * @param entity      the unit completing its deployment
     * @param bridgeLayer the bridgelayer mount
     * @param bridgeState the mount's carried-bridge state
     */
    private void finishBridgeDeploy(Entity entity, MiscMounted bridgeLayer, BridgeLayerState bridgeState) {
        Coords target = bridgeState.getDeployTarget();
        int boardId = entity.getBoardId();
        Board board = getGame().getBoard(boardId);
        int exits = bridgeState.getDeployExits();
        if ((target == null) || !BridgeConstruction.isValidBridgeSite(board, target, exits)) {
            LOGGER.info("[AVLB] {} cannot place its bridge: {} is no longer a valid bridge site (exits bitmask {}); "
                  + "deployment cancelled", entity.getShortName(), target, exits);
            bridgeState.clearPendingDeploy();
            Report report = new Report(4294);
            report.subject = entity.getId();
            report.addDesc(entity);
            addReport(report);
            return;
        }

        // A dry target is only a legal site as a gap between two elevated hexes, so both banks must be rims above it
        // (a water target instead only needs one adjacent land/bridge, already checked above). Re-checked here in case
        // the terrain changed while the unit waited. TM p.242 / TW.
        Hex targetHex = board.getHex(target);
        if ((targetHex != null) && !BridgeConstruction.isOverWater(targetHex)) {
            Hex nearBank = (entity.getPosition() == null) ? null : board.getHex(entity.getPosition());
            Hex farBank = board.getHex(target.translated(entity.getFacing()));
            boolean spansTwoElevatedHexes = (nearBank != null) && (farBank != null)
                  && BridgeConstruction.isAnchoringBank(nearBank, targetHex)
                  && BridgeConstruction.isAnchoringBank(farBank, targetHex);
            if (!spansTwoElevatedHexes) {
                LOGGER.info("[AVLB] {} cannot place its bridge: {} is dry ground, not a gap between two elevated "
                      + "hexes; deployment cancelled", entity.getShortName(), target);
                bridgeState.clearPendingDeploy();
                Report report = new Report(4294);
                report.subject = entity.getId();
                report.addDesc(entity);
                addReport(report);
                return;
            }
        }

        int bridgeType = BridgeLayerState.terrainBridgeType(bridgeLayer.getType());
        // A bridge deployed over water rides on flotation devices and supports units up to twice its current CF;
        // placing it at double CF makes the standard load-collapse (load > CF) give that 2x capacity. A bridge laid
        // across a land gap has no flotation, so it is placed at its plain CF (TM p.242 / TW).
        boolean isOverWater = BridgeConstruction.isOverWater(board.getHex(target));
        int constructionFactor = isOverWater ? (bridgeState.getCurrentCF() * 2) : bridgeState.getCurrentCF();
        IBuilding bridge = BridgeConstruction.placeBridge(board, target, exits, bridgeType, constructionFactor);
        bridgeState.setDeployed(true);
        bridgeState.clearPendingDeploy();
        LOGGER.info("[AVLB] {} deploys a type-{} bridge at {} (CF {}, over water: {}, exits bitmask {})",
              entity.getShortName(), bridgeType, target, constructionFactor, isOverWater, exits);
        gameManager.sendChangedHex(target, boardId);
        Vector<IBuilding> newBridges = new Vector<>();
        newBridges.add(bridge);
        gameManager.sendNewBuildings(newBridges);

        // If the unit carries more than one bridgelayer, name which one was deployed (its mounted-location side);
        // otherwise the plain "its bridge" wording reads better. The deployed bridge is public board terrain (the
        // changed hex is broadcast to everyone), so the report is public.
        long bridgeLayerCount = entity.getMisc().stream().filter(misc -> misc.getBridgeLayerState() != null).count();
        Report report;
        if (bridgeLayerCount > 1) {
            report = new Report(4299, Report.PUBLIC);
            report.subject = entity.getId();
            report.addDesc(entity);
            report.add(entity.getLocationName(bridgeLayer.getLocation()));
            report.add(constructionFactor);
            report.add(target.getBoardNum());
        } else {
            report = new Report(4292, Report.PUBLIC);
            report.subject = entity.getId();
            report.addDesc(entity);
            report.add(constructionFactor);
            report.add(target.getBoardNum());
        }
        addReport(report);
        // Also surface the placement as a kill-feed style toast (mirroring other end-phase board changes such as the
        // climb-completed and building-collapse pop-ups), so players notice a bridge appearing - especially an
        // opponent's - immediately, instead of only finding it buried in the End Phase report.
        Vector<Report> deployToast = new Vector<>();
        deployToast.add(report);
        gameManager.send(gameManager.createSpecialReportPacket(deployToast));
    }
}
