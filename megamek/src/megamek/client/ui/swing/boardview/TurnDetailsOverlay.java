/*
 * MegaMek - Copyright (C) 2023 - The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.client.ui.swing.boardview;

import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.GUIPreferences;
import megamek.common.Game;

import java.util.ArrayList;
import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;

/**
 * An overlay for the Boardview that displays info about the users turn, such as list of users Move or Attack commands
 * for the current game situation
 *
 *
 */
public class TurnDetailsOverlay extends AbstractBoardViewOverlay {
    /**
     * An overlay for the Boardview that shows details about a players turn
     */
    public TurnDetailsOverlay(Game game, ClientGUI cg) {
        super(game, cg);
    }


    /** @return an ArrayList of all text lines to be shown. */
    @Override
    protected List<String> assembleTextLines() {
        List<String> result = new ArrayList<>();
        return result;
    }

    @Override
    protected void setVisibilityGUIPreference(boolean value) {
        GUIP.setValue(GUIPreferences.SHOW_PLANETARYCONDITIONS_OVERLAY, value);
    }
    @Override
    protected boolean getVisibilityGUIPreference() {
        return GUIP.getShowPlanetaryConditionsOverlay();
    }
    @Override
    protected Color getTextColorGUIPreference() {
        return GUIP.getPlanetaryConditionsColorText();
    }

    @Override
    protected int getDistTop(Rectangle clipBounds, int overlayHeight) {
        return clipBounds.height - (overlayHeight + 100);
    }

    @Override
    protected int getDistSide(Rectangle clipBounds, int overlayWidth) {
        return clipBounds.width - (overlayWidth + 100);
    }
}
