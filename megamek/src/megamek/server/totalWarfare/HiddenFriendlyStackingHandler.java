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
import megamek.common.compute.Compute;
import megamek.common.moves.MoveStep;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Tells a player why their unit stopped one hex short of where they sent it, when the hex it was sent to holds a hidden
 * unit of their own side. A unit may not end its move stacked on a friendly unit, hidden or not. A player normally
 * sees their side's hidden units and cannot pick such a hex, but with team vision off a teammate's hidden unit is
 * unknown to them, so the server is the only one that can catch it. The hidden unit is not named and stays hidden.
 */
class HiddenFriendlyStackingHandler extends AbstractTWRuleHandler {
    private static final MMLogger LOGGER = MMLogger.create(HiddenFriendlyStackingHandler.class);

    HiddenFriendlyStackingHandler(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * Reports a move that the server ended early because its last step would stack the unit on a hidden friendly
     * unit. Does nothing when the step was illegal for any other reason.
     *
     * @param mover    the unit that was moving
     * @param lastStep the last step of its path, which the server did not carry out
     */
    void reportMoveStoppedShort(Entity mover, MoveStep lastStep) {
        Entity blocker = Compute.stackingViolation(getGame(),
              mover,
              lastStep.getElevation(),
              lastStep.getPosition(),
              lastStep.getBoardId(),
              null,
              mover.climbMode(),
              false);
        if (blocker == null) {
            return;
        }
        if (!blocker.isHidden()) {
            LOGGER.debug("[HiddenStacking] {} at {}: illegal last step, blocker {} is not hidden",
                  mover.getShortName(), lastStep.getPosition().getBoardNum(), blocker.getShortName());
            return;
        }
        if (blocker.isEnemyOf(mover)) {
            LOGGER.debug("[HiddenStacking] {} at {}: illegal last step, hidden blocker {} is an enemy",
                  mover.getShortName(), lastStep.getPosition().getBoardNum(), blocker.getShortName());
            return;
        }
        LOGGER.info("[HiddenStacking] {} stops one hex short of {}: hidden friendly unit {} is there",
              mover.getShortName(), lastStep.getPosition().getBoardNum(), blocker.getShortName());
        gameManager.sendServerChat(mover.getOwnerId(),
              Messages.getString("MovementDisplay.stoppedShortOfHiddenUnit",
                    mover.getShortName(), lastStep.getPosition().getBoardNum()));
    }
}
