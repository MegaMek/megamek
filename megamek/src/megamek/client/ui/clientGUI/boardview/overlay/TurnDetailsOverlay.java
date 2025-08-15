/*
 * Copyright (C) 2023-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.clientGUI.boardview.overlay;

import java.awt.Font;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.util.KeyCommandBind;
import megamek.common.preference.PreferenceChangeEvent;

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
