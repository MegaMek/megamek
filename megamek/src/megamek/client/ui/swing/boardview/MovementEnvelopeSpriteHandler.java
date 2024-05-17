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

import megamek.client.ui.swing.MovementDisplay;
import megamek.common.*;
import megamek.common.event.*;

import java.util.*;
import java.awt.*;

/**
 * This class handles the sprites shown on an attached BoardView for the movement envelope (showing the hexes
 * where a currently selected unit can move to in the movement phase).
 */
public class MovementEnvelopeSpriteHandler extends BoardViewSpriteHandler {

    private final Game game;

    public MovementEnvelopeSpriteHandler(BoardView boardView, Game game) {
        super(boardView);
        this.game = game;
    }

    @Override
    public void initialize() {
        game.addGameListener(this);
    }

    @Override
    public void dispose() {
        clear();
        game.removeGameListener(this);
    }

    public void setMovementEnvelope(Map<Coords, Integer> mvEnvData, int walk, int run, int jump, int gear) {
        clear();

        if (mvEnvData == null) {
            return;
        }

        for (Coords loc : mvEnvData.keySet()) {
            Color spriteColor = null;
            int mvType = -1;
            if (gear == MovementDisplay.GEAR_JUMP || gear == MovementDisplay.GEAR_DFA) {
                if (mvEnvData.get(loc) <= jump) {
                    spriteColor = GUIP.getMoveJumpColor();
                    mvType = 1;
                }
            } else {
                if (mvEnvData.get(loc) <= walk) {
                    spriteColor = GUIP.getMoveDefaultColor();
                    mvType = 2;
                } else if (mvEnvData.get(loc) <= run) {
                    spriteColor = GUIP.getMoveRunColor();
                    mvType = 3;
                } else {
                    spriteColor = GUIP.getMoveSprintColor();
                    mvType = 4;
                }
            }

            // Next: check the adjacent hexes and find those with the same movement type,
            // send this to the Sprite so it paints only the borders of the movement type areas
            int mvAdjType;
            int edgesToPaint = 0;
            // cycle through hexes
            for (int dir = 0; dir < 6; dir++) {
                mvAdjType = 0;
                Coords adjacentHex = loc.translated(dir);
                // get the movement type
                Integer Adjmv = mvEnvData.get(adjacentHex);
                if (Adjmv != null) {
                    if (gear == MovementDisplay.GEAR_JUMP) {
                        if (Adjmv <= jump) {
                            mvAdjType = 1;
                        }
                    } else {
                        if (Adjmv <= walk) {
                            mvAdjType = 2;
                        } else if (Adjmv <= run) {
                            mvAdjType = 3;
                        } else {
                            mvAdjType = 4;
                        }
                    }
                }

                // other movement type: paint a border in this direction
                if (mvAdjType != mvType) {
                    edgesToPaint += (1 << dir);
                }
            }

            if (spriteColor != null) {
                currentSprites.add(new MovementEnvelopeSprite(boardView, spriteColor, loc, edgesToPaint));
            }
        }

        boardView.addSprites(currentSprites);
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {
        clear();
    }
}
