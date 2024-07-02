/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */

package megamek.client.ui.swing.boardview;

import megamek.common.annotations.Nullable;
import megamek.common.strategicBattleSystems.SBFMovePath;
import megamek.common.strategicBattleSystems.SBFMoveStep;

import java.awt.*;

public class MovePathSpriteHandler extends BoardViewSpriteHandler {

    public MovePathSpriteHandler(BoardView boardView) {
        super(boardView);
    }

    /**
     * Clears the current sprites and creates new sprites for all formations.
     */
    public void update(@Nullable SBFMovePath movePath) {
        clear();
        if (movePath == null) {
            return;
        }

        SBFMoveStep previousStep = null;

        // first get the color for the vector
        Color col = Color.blue;

        for (SBFMoveStep step : movePath.getSteps()) {
//            if ((null != previousStep)
//                    && ((step.getType() == MovePath.MoveStepType.UP)
//                    || (step.getType() == MovePath.MoveStepType.DOWN)
//                    || (step.getType() == MovePath.MoveStepType.DECN))) {
//                // Mark the previous elevation change sprite hidden
//                // so that we can draw a new one in it's place without
//                // having overlap.
//                currentSprites.get(currentSprites.size() - 1).setHidden(true);
//            }

            currentSprites.add(new SBFStepSprite(boardView, step, movePath));
            previousStep = step;
        }

        boardView.addSprites(currentSprites);
    }

    @Override
    public void initialize() {

    }

    @Override
    public void dispose() {

    }
}
