/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.clientGUI.boardview;

import java.awt.Graphics2D;

import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.Hex;

/**
 * HexDrawPlugins can be given to a BoardView and will be called whenever the BoardView draws any of its hexes to the
 * hex image cache. In this way the draw process can be adapted to different situations (board editor, preview, game)
 * without changing the BoardView itself (and making it ever bigger). Also, since the results are cached, the draw
 * process can be used for many hexes without making the BoardView slow.
 * <p>
 * Note that at the moment, these plugins will always draw last, i.e. above everything else that gets drawn to the image
 * cache. As sprites get drawn separately, they will draw above plugins.
 */
public interface HexDrawPlugin {

    /**
     * Draws graphics content to the present hex image. Unlike sprites the contents drawn here will become part of the
     * cached hex images. Drawing should be done to the provided graphics2D using information from the hex, game and
     * BoardView if necessary. Note that the available area of the hex image depends on the BoardView's scale. When the
     * scale is 1, the area is the usual hex shape within a rectangle of 84x72 (HEX_W x HEX_H). At other scales, the
     * rectangle is scaled accordingly.
     *
     * @param graphics2D The {@link Graphics2D} to draw to
     * @param hex        The {@link Hex} currently drawn
     * @param game       the {@link Game}
     * @param coords     the location of the hex on the board
     * @param boardView  The {@link BoardView} that is calling this method
     *
     * @see BoardView#getScale()
     * @see megamek.client.ui.tileset.HexTileset#HEX_W
     * @see BoardView#clearHexImageCache()
     */
    void draw(Graphics2D graphics2D, Hex hex, Game game, Coords coords, BoardView boardView);
}
