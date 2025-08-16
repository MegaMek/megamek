/*
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.clientGUI.boardview.spriteHandler.sbf;

import megamek.client.SBFClient;
import megamek.client.ui.clientGUI.AbstractClientGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.sprite.Sprite;
import megamek.client.ui.clientGUI.boardview.sprite.sbf.SBFFormationSprite;
import megamek.client.ui.clientGUI.boardview.sprite.sbf.SBFPlaceHolderSprite;
import megamek.client.ui.clientGUI.boardview.spriteHandler.BoardViewSpriteHandler;
import megamek.common.annotations.Nullable;
import megamek.common.event.GameEvent;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFGame;
import megamek.common.strategicBattleSystems.SBFUnitPlaceHolder;

public class SBFFormationSpriteHandler extends BoardViewSpriteHandler {

    private final SBFGame game;

    public SBFFormationSpriteHandler(AbstractClientGUI clientGUI, SBFClient client) {
        super(clientGUI);
        game = client.getGame();
    }

    /**
     * Clears the current sprites and creates new sprites for all formations.
     */
    public void update() {
        clear();
        game.getInGameObjects().stream()
              .filter(SBFFormation.class::isInstance)
              .filter(f -> ((SBFFormation) f).getPosition() != null)
              .map(f -> new SBFFormationSprite((BoardView) clientGUI.boardViews().get(0),
                    (SBFFormation) f,
                    game.getPlayer(f.getOwnerId()),
                    game))
              .forEach(currentSprites::add);

        game.getInGameObjects().stream()
              .filter(SBFUnitPlaceHolder.class::isInstance)
              .filter(f -> ((SBFUnitPlaceHolder) f).getPosition() != null)
              .map(f -> new SBFPlaceHolderSprite((BoardView) clientGUI.boardViews().get(0),
                    (SBFUnitPlaceHolder) f,
                    game.getPlayer(f.getOwnerId()),
                    game))
              .forEach(currentSprites::add);

        clientGUI.boardViews().get(0).addSprites(currentSprites);
    }

    /**
     * Sets the given formation as the selected formation and renews the sprites accordingly. The previously selected
     * formation, if any, will no longer be selected. When the given formation is null, all formations will be
     * deselected.
     *
     * @param formation The formation to draw as selected, or null for no selection
     */
    public void setSelectedFormation(@Nullable SBFFormation formation) {
        for (Sprite sprite : currentSprites) {
            if (sprite instanceof SBFFormationSprite formationSprite) {
                formationSprite.setSelected(formationSprite.getFormation().equals(formation));
            }
        }
        clientGUI.boardViews().get(0).repaint();
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
    public void gameUnitChange(GameEvent event) {
        super.gameUnitChange(event);
        update();
    }

}
