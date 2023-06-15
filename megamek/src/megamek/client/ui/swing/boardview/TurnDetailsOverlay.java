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

import megamek.client.ui.Messages;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.common.Game;

import java.awt.*;
import java.util.ArrayList;
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

    List<String> lines = new ArrayList<>();

    static String validTextColor, invalidTextColor;

    public TurnDetailsOverlay(Game game, ClientGUI cg) {
        super(game, cg, new Font(Font.MONOSPACED, Font.BOLD, 12),
                Messages.getString("TurnDetailsOverlay.heading", KeyCommandBind.getDesc(KeyCommandBind.TURN_DETAILS)) );
    }


    /** @return an ArrayList of all text lines to be shown. */
    @Override
    protected List<String> assembleTextLines() {
        return lines;
    }

    public void setLines(List<String> newLines) {
        lines.clear();
        if (newLines != null && newLines.size() != 0) {
            addHeader(lines);
            lines.addAll(newLines);
        }
        setDirty();
    }

    @Override
    protected void gameTurnOrPhaseChange() {
        if (!clientGui.getClient().isMyTurn()) {
            lines.clear();
        }
        super.gameTurnOrPhaseChange();
    }

    @Override
    protected void setVisibilityGUIPreference(boolean value) {
        GUIP.setTurnDetailsOverlay(value);
    }
    @Override
    protected boolean getVisibilityGUIPreference() {
        return GUIP.getTurnDetailsOverlay();
    }

    @Override
    protected int getDistTop(Rectangle clipBounds, int overlayHeight) {
        return clipBounds.height - (overlayHeight + 5);
    }

    @Override
    protected int getDistSide(Rectangle clipBounds, int overlayWidth) {
        return clipBounds.width - (overlayWidth + 100);
    }
}
