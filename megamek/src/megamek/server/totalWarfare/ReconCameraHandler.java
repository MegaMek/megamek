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

import megamek.client.ui.Messages;
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
 * Resolves Recon Camera spots by ground units at the end of the Off-Board phase (TO:AUE p.150). The spot is rolled like
 * a TAG shot from the same unit; on a hit the unit spots the target for LRM indirect fire for the rest of the turn, and
 * its side sees the target under double-blind. Every declaration uses up the unit's camera for the turn, hit or miss.
 */
class ReconCameraHandler extends AbstractTWRuleHandler {

    private static final MMLogger LOGGER = MMLogger.create(ReconCameraHandler.class);

    static final int REPORT_CAMERA_SPOTTED = 7160;
    static final int REPORT_CAMERA_MISSED = 7161;
    static final int REPORT_CAMERA_REFUSED = 7162;

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
        String refusal = ReconCameraRules.spotRefusal(getGame(), camera, target);
        if (refusal != null) {
            Report report = new Report(REPORT_CAMERA_REFUSED);
            report.subject = camera.getId();
            report.addDesc(camera);
            report.add((target == null) ? Messages.getString("ReconCamera.unknownTarget") : target.getShortName());
            report.add(refusal);
            addReport(report);
            return;
        }

        ToHitData toHit = ReconCameraRules.spotToHit(getGame(), camera, target);
        Roll roll = rollSpot();
        boolean isHit = (toHit.getValue() != TargetRoll.AUTOMATIC_FAIL) && (roll.getIntValue() >= toHit.getValue());
        camera.setReconCameraSpotResult(isHit ? target.getId() : Entity.NONE);
        LOGGER.debug("[ReconCamera] {}: camera spot on {} - needed {} [{}], rolled {}: {}", camera.getShortName(),
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

    /**
     * Rolls 2D6 for a camera spot. Package-private so a test can fix the result.
     *
     * @return the roll
     */
    Roll rollSpot() {
        return Compute.rollD6(2);
    }
}
