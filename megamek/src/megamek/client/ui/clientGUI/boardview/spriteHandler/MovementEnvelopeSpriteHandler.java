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
package megamek.client.ui.clientGUI.boardview.spriteHandler;

import java.awt.Color;
import java.util.Map;

import megamek.client.ui.clientGUI.AbstractClientGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.IBoardView;
import megamek.client.ui.clientGUI.boardview.sprite.MovementEnvelopeSprite;
import megamek.client.ui.panels.phaseDisplay.MovementDisplay;
import megamek.common.Coords;
import megamek.common.IGame;
import megamek.common.event.GamePhaseChangeEvent;

/**
 * This class handles the sprites shown on an attached BoardView for the movement envelope (showing the hexes where a
 * currently selected unit can move to in the movement phase).
 */
public class MovementEnvelopeSpriteHandler extends BoardViewSpriteHandler {

    private final IGame game;

    public MovementEnvelopeSpriteHandler(AbstractClientGUI clientGUI, IGame game) {
        super(clientGUI);
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

    public void setMovementEnvelope(Map<Coords, Integer> mvEnvData, int boardId, int walk, int run, int jump,
          int gear) {
        clear();

        if (mvEnvData == null) {
            return;
        }

        IBoardView iBoardView = clientGUI.getBoardView(boardId);
        if (!(iBoardView instanceof BoardView boardView)) {
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
