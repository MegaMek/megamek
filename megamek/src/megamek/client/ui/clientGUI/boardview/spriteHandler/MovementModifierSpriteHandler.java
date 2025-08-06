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

import java.util.Collection;

import megamek.client.ui.clientGUI.AbstractClientGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.sprite.MovementModifierEnvelopeSprite;
import megamek.common.Game;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.moves.MovePath;

public class MovementModifierSpriteHandler extends BoardViewSpriteHandler {

    private final Game game;

    public MovementModifierSpriteHandler(AbstractClientGUI clientGUI, Game game) {
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

    public void renewSprites(Collection<MovePath> movePaths) {
        clear();
        if (clientGUI.boardViews().isEmpty()) {
            return;
        }
        BoardView boardView = (BoardView) clientGUI.boardViews().get(0);
        movePaths.stream()
              .map(path -> new MovementModifierEnvelopeSprite(boardView, path))
              .forEach(currentSprites::add);
        movePaths.stream()
              .filter(MovePath::isMoveLegal)
              .map(path -> new MovementModifierEnvelopeSprite(boardView, path))
              .forEach(currentSprites::add);
        boardView.addSprites(currentSprites);
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {
        clear();
    }
}
