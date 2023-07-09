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
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.common.preference.PreferenceChangeEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * An overlay for the Boardview that displays info about the users turn, such as list of users Move or Attack commands
 * for the current game situation
 */
public class TurnDetailsOverlay extends AbstractBoardViewOverlay {

    List<String> lines = new ArrayList<>();

    public TurnDetailsOverlay(BoardView boardView) {
        super(boardView, new Font(Font.MONOSPACED, Font.BOLD, 12));
    }

    @Override
    protected String getHeaderText() {
        return Messages.getString("TurnDetailsOverlay.heading", KeyCommandBind.getDesc(KeyCommandBind.TURN_DETAILS));
    }

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
        if ((clientGui == null) || !clientGui.getClient().isMyTurn()) {
            lines.clear();
        }
        super.gameTurnOrPhaseChange();
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

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        if (e.getName().equals(GUIPreferences.TURN_DETAILS_OVERLAY)) {
            setVisible((boolean) e.getNewValue());
            scheduleBoardViewRepaint();
        }
        super.preferenceChange(e);
    }
}