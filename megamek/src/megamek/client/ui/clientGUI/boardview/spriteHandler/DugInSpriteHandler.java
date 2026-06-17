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
package megamek.client.ui.clientGUI.boardview.spriteHandler;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.AbstractClientGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.sprite.DugInSprite;
import megamek.common.board.BoardLocation;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;

/**
 * Manages the dug-in trench overlay sprites on the board view (TO:AR p.106 Digging In). Creates and removes
 * {@link DugInSprite} instances on the hexes of infantry platoons that are digging in or dug in; the state is read from
 * the synced entities. The multi-turn fortified-hex build (sandbags) is handled separately by
 * {@link FortifyBuildSpriteHandler}.
 */
public class DugInSpriteHandler extends BoardViewSpriteHandler {

    /** Opacity of the trench overlay while the platoon is still digging in (not yet protected). */
    private static final float DIGGING_IN_ALPHA = 0.4f;

    /** Opacity of the trench overlay once the platoon is dug in (protected). */
    private static final float DUG_IN_ALPHA = 1.0f;

    private final Game game;

    public DugInSpriteHandler(AbstractClientGUI clientGUI, Game game) {
        super(clientGUI);
        this.game = game;
    }

    /**
     * Rebuilds the dug-in trench sprites from the current entity states: one trench overlay on the hex of every
     * infantry platoon that is digging in (faint) or dug in (full).
     */
    public void updateDugInSprites() {
        clear();
        if (clientGUI.boardViews().isEmpty()) {
            return;
        }
        for (Entity entity : game.getEntitiesVector()) {
            // Only show the trench once the unit is actually deployed - during the deploy phase a unit being
            // positioned has a tentative position but is not yet deployed, and the overlay would otherwise be
            // left behind on the first-clicked hex as the player re-clicks to reposition.
            if (!(entity instanceof Infantry infantry) || !entity.isDeployed() || (entity.getPosition() == null)) {
                continue;
            }
            float alpha = trenchAlphaFor(infantry);
            if (alpha <= 0f) {
                continue;
            }
            BoardLocation location = BoardLocation.of(entity.getPosition(), entity.getBoardId());
            BoardView boardView = (BoardView) clientGUI.getBoardView(location);
            if (boardView != null) {
                currentSprites.add(new DugInSprite(boardView, entity.getPosition(), alpha, trenchLabelFor(infantry)));
            }
        }
        currentSprites.forEach(sprite -> sprite.bv.addSprite(sprite));
    }

    /**
     * @return the trench overlay opacity for the platoon: full when dug in, faint while still digging in, or 0 when it
     *       is neither (including the multi-turn fortifying states, which show sandbags instead).
     */
    private static float trenchAlphaFor(Infantry infantry) {
        return switch (infantry.getDugIn()) {
            case Infantry.DUG_IN_COMPLETE -> DUG_IN_ALPHA;
            case Infantry.DUG_IN_WORKING -> DIGGING_IN_ALPHA;
            default -> 0f;
        };
    }

    /**
     * @return the status label drawn in the hex: "Dug in" once protected, "Digging in" while still working
     */
    private static String trenchLabelFor(Infantry infantry) {
        String key = (infantry.getDugIn() == Infantry.DUG_IN_COMPLETE) ? "BoardView1.dugIn" : "BoardView1.diggingIn";
        return Messages.getString(key);
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
    public void gamePhaseChange(GamePhaseChangeEvent e) {
        updateDugInSprites();
    }

    @Override
    public void gameEntityChange(GameEntityChangeEvent e) {
        updateDugInSprites();
    }
}
