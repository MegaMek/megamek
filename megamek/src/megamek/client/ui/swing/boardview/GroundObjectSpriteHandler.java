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

import java.util.List;
import java.util.Map;

import megamek.common.Coords;
import megamek.common.Game;
import megamek.common.ICarryable;
import megamek.common.event.GameBoardChangeEvent;

public class GroundObjectSpriteHandler extends BoardViewSpriteHandler {

    // Cache the ground object list as it does not change very often
    private Map<Coords, List<ICarryable>> currentGroundObjectList;
    private final Game game;

    public GroundObjectSpriteHandler(BoardView boardView, Game game) {
        super(boardView);
        this.game = game;
    }

    public void setGroundObjectSprites(Map<Coords, List<ICarryable>> objectCoordList) {
        clear();
        currentGroundObjectList = objectCoordList;
        if (currentGroundObjectList != null) {
        	for (Coords coords : currentGroundObjectList.keySet()) {
        		for (ICarryable groundObject : currentGroundObjectList.get(coords)) {
	        		GroundObjectSprite gos = new GroundObjectSprite(boardView, coords);
	        		currentSprites.add(gos);
        		}
        	}
        }

        boardView.addSprites(currentSprites);
    }

    @Override
    public void clear() {
        super.clear();
        currentGroundObjectList = null;
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

    @Override
    public void gameBoardChanged(GameBoardChangeEvent e) {
    	setGroundObjectSprites(game.getGroundObjects());
    }
}
