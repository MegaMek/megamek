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

import megamek.common.Coords;
import java.util.List;

public class GroundObjectSpriteHandler extends BoardViewSpriteHandler {

    // Cache the warn list; thus, when CF warning is turned on the sprites can easily be created
    private Iterable<Coords> currentGroundObjectList;

    public GroundObjectSpriteHandler(BoardView boardView) {
        super(boardView);
    }

    public void setGroundObjectSprites(Iterable<Coords> objectCoordList) {
        clear();
        currentGroundObjectList = objectCoordList;
        if (currentGroundObjectList != null) {
        	for (Coords coords : currentGroundObjectList) {
        		CollapseWarningSprite cws = new CollapseWarningSprite(boardView, coords);
        		currentSprites.add(cws);
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
    }

    @Override
    public void dispose() {
        clear();
    }
}
