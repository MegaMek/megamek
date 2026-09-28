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

import megamek.client.ui.Messages;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.ReconCameraSpotAction;
import megamek.common.compute.Compute;
import megamek.common.rolls.Roll;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.ReconCameraRules;
import megamek.logging.MMLogger;

/**
 * Resolves the Recon Camera on the server (TO:AUE p.150). Camera spots are rolled at the end of the Off-Board phase; on a
 * hit the unit spots the target for LRM indirect fire and artillery for the rest of the turn, and its side sees the
 * target under double-blind. Aerospace units flying with the camera set to Reveal get their reveal rolls at the end of
 * the Movement phase, against every hostile hidden unit below their flight. Either use takes the camera for the turn.
 */
class ReconCameraHandler extends AbstractTWRuleHandler {

    private static final MMLogger LOGGER = MMLogger.create(ReconCameraHandler.class);

    static final int REPORT_CAMERA_SPOTTED = 7160;
    static final int REPORT_CAMERA_MISSED = 7161;
    static final int REPORT_CAMERA_REFUSED = 7162;
    static final int REPORT_CAMERA_REVEALED = 7163;

    ReconCameraHandler(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * Rolls a declared camera spot and records the result on the unit.
     *
     * @param camera the unit with the camera
     * @param action the declared spot
     */
    void resolveSpot(Entity camera, ReconCameraSpotAction action) {
        if (!getGame().getPhase().isOffboard()) {
            LOGGER.warn("[ReconCamera] {}: camera spot ignored - declared in the {} phase, not the Off-Board phase",
                  camera.getShortName(), getGame().getPhase());
            return;
        }
        Entity target = getGame().getEntity(action.getTargetId());
        if (target == null) {
            LOGGER.warn("[ReconCamera] {}: camera spot ignored - target {} is no longer in the game",
                  camera.getShortName(), action.getTargetId());
            return;
        }
        String refusal = ReconCameraRules.spotRefusal(getGame(), camera, target);
        if (refusal != null) {
            Report report = new Report(REPORT_CAMERA_REFUSED);
            report.subject = camera.getId();
            report.addDesc(camera);
            report.add(target.getShortName());
            report.add(refusal);
            addReport(report);
            return;
        }

        ToHitData toHit = ReconCameraRules.spotToHit(getGame(), camera, target);
        Roll roll = rollSpot();
        boolean isHit = (toHit.getValue() != TargetRoll.AUTOMATIC_FAIL) && (roll.getIntValue() >= toHit.getValue());
        camera.setReconCameraSpotResult(isHit ? target.getId() : Entity.NONE);
        if (isHit) {
            markSpotted(camera, target);
        }
        LOGGER.info("[ReconCamera] {}: camera spot on {} - needed {} [{}], rolled {}: {}", camera.getShortName(),
              target.getShortName(), toHit.getValue(), toHit.getDesc(), roll.getIntValue(), isHit ? "hit" : "miss");

        Report report = new Report(isHit ? REPORT_CAMERA_SPOTTED : REPORT_CAMERA_MISSED);
        report.subject = camera.getId();
        report.addDesc(camera);
        report.add(target.getShortName());
        report.add(toHit.getValue());
        report.add(toHit.getDesc());
        report.add(roll.getIntValue());
        addReport(report);

        // under double-blind the camera's side now sees the target; refresh who sees what before the reports go out
        if (isHit && gameManager.doBlind()) {
            gameManager.updateVisibilityIndicator(null);
        }
    }

    private void markSpotted(Entity camera, Entity target) {
        List<Integer> viewerIds = new ArrayList<>();
        for (Player player : getGame().getPlayersList()) {
            if (ReconCameraRules.isOnCameraSide(camera, player)) {
                viewerIds.add(player.getId());
            }
        }
        target.addReconCameraSpot(camera.getDisplayName(), viewerIds);
    }

    /**
     * Gives every aerospace unit that flew with its camera set to Reveal this turn its reveal rolls: each hostile hidden
     * unit below its flight path is revealed when its owner rolls the target number or more. Called at the end of the
     * Movement phase.
     */
    void revealHiddenUnits() {
        for (Entity camera : getGame().getEntitiesVector()) {
            if (ReconCameraRules.canRevealHiddenUnits(camera)) {
                revealBelow(camera);
            }
        }
    }

    private void revealBelow(Entity camera) {
        // trying to reveal uses the camera for the turn: no spot and no other attack
        camera.setReconCameraSpotResult(Entity.NONE);
        List<Entity> hiddenUnits = ReconCameraRules.hiddenUnitsBelowFlightPath(getGame(), camera);
        int revealed = 0;
        for (Entity hiddenUnit : hiddenUnits) {
            if (rollToReveal(camera, hiddenUnit)) {
                revealed++;
            }
        }
        LOGGER.info("[ReconCamera] {}: flew in Reveal mode over {} hostile hidden unit(s), {} revealed",
              camera.getShortName(), hiddenUnits.size(), revealed);
    }

    private boolean rollToReveal(Entity camera, Entity hiddenUnit) {
        ToHitData targetNumber = ReconCameraRules.revealTargetNumber(getGame(), hiddenUnit);
        Roll roll = rollReveal();
        boolean isRevealed = roll.getIntValue() >= targetNumber.getValue();
        LOGGER.trace("[ReconCamera] reveal roll for {}: needed {}, rolled {}", hiddenUnit.getShortName(),
              targetNumber.getValue(), roll.getIntValue());
        if (isRevealed) {
            hiddenUnit.setHidden(false);
            gameManager.entityUpdate(hiddenUnit.getId());
            Report report = new Report(REPORT_CAMERA_REVEALED);
            report.subject = camera.getId();
            report.addDesc(camera);
            report.add(hiddenUnit.getShortName());
            report.add(hiddenUnit.getPosition().getBoardNum());
            report.add(targetNumber.getValue());
            report.add(targetNumber.getDesc());
            report.add(roll.getIntValue());
            addReport(report);
        } else {
            // Only the hidden unit's owner may learn of the roll, or the camera side would know something is there. A
            // report cannot do that: without double-blind every player gets every report, and with it the others get
            // an obscured copy. A private server message reaches the owner alone.
            gameManager.sendServerChat(hiddenUnit.getOwnerId(), Messages.getString("ReconCamera.stayedHidden",
                  hiddenUnit.getShortName(), camera.getShortName(), targetNumber.getValue(), targetNumber.getDesc(),
                  roll.getIntValue()));
        }
        return isRevealed;
    }

    /**
     * Rolls 2D6 for a hidden unit under a camera. Package-private so a test can fix the result.
     *
     * @return the roll
     */
    Roll rollReveal() {
        return Compute.rollD6(2);
    }

    /**
     * Rolls 2D6 for a camera spot. Package-private so a test can fix the result.
     *
     * @return the roll
     */
    Roll rollSpot() {
        return Compute.rollD6(2);
    }
}
