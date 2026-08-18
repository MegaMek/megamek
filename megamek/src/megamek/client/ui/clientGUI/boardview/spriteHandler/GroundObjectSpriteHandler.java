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
import java.util.List;
import java.util.Map;

import megamek.client.ui.clientGUI.AbstractClientGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.sprite.GroundObjectSprite;
import megamek.client.ui.clientGUI.boardview.sprite.HexFlagSprite;
import megamek.client.ui.clientGUI.boardview.sprite.Sprite;
import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.ObjectiveMarker;
import megamek.common.event.board.GameBoardChangeEvent;
import megamek.common.game.Game;
import megamek.logging.MMLogger;

public class GroundObjectSpriteHandler extends BoardViewSpriteHandler {

    private static final MMLogger LOGGER = MMLogger.create(GroundObjectSpriteHandler.class);

    // Cache the ground object list as it does not change very often
    private Map<Coords, List<ICarryable>> currentGroundObjectList;
    private final Game game;

    public GroundObjectSpriteHandler(AbstractClientGUI clientGUI, Game game) {
        super(clientGUI);
        this.game = game;
    }

    public void setGroundObjectSprites(Map<Coords, List<ICarryable>> objectCoordList) {
        clear();
        if (clientGUI.boardViews().isEmpty()) {
            return;
        }
        currentGroundObjectList = objectCoordList;
        int flagCount = 0;
        if (currentGroundObjectList != null) {
            BoardView boardView = (BoardView) clientGUI.boardViews().getFirst();
            for (Coords coords : currentGroundObjectList.keySet()) {
                for (ICarryable groundObject : currentGroundObjectList.get(coords)) {
                    if (groundObject instanceof ObjectiveMarker) {
                        flagCount++;
                    }
                    currentSprites.add(spriteFor(groundObject, coords, boardView));
                }
            }
        }

        clientGUI.boardViews().getFirst().addSprites(currentSprites);
        LOGGER.debug("[VictoryHex] Board shows {} ground object sprite(s), {} of them objective flag(s)",
              currentSprites.size(), flagCount);
    }

    /**
     * @param groundObject the ground object to create a sprite for
     * @param coords       the hex the ground object is in
     * @param boardView    the board view the sprite is displayed on
     *
     * @return a flag in the owning player's color for an objective marker, otherwise the generic cargo sprite
     */
    private Sprite spriteFor(ICarryable groundObject, Coords coords, BoardView boardView) {
        if (groundObject instanceof ObjectiveMarker marker) {
            return new HexFlagSprite(boardView, coords, ownerColor(marker));
        }
        return new GroundObjectSprite(boardView, coords);
    }

    /**
     * @param marker the objective marker to color
     *
     * @return the owning player's color, or yellow when the owner is unknown to this client
     */
    private Color ownerColor(ObjectiveMarker marker) {
        Player owner = game.getPlayer(marker.getOwnerId());
        return (owner != null) ? owner.getColour().getColour() : Color.YELLOW;
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
