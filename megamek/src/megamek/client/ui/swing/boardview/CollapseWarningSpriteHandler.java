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

import megamek.client.ui.swing.GUIPreferences;
import megamek.common.Coords;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;

import java.util.List;

public class CollapseWarningSpriteHandler extends BoardViewSpriteHandler implements IPreferenceChangeListener {

    // Cache the warn list; thus, when CF warning is turned off the sprites don't have to be created
    private List<Coords> currentWarnList;

    public CollapseWarningSpriteHandler(BoardView boardView) {
        super(boardView);
    }

    public void setCFWarningSprites(List<Coords> warnList) {
        clear();
        currentWarnList = warnList;
        if ((warnList != null) && GUIP.getShowCFWarnings()) {
            warnList.stream().map(coords -> new ConstructionWarningSprite(boardView, coords)).forEach(currentSprites::add);
        }
        boardView.addSprites(currentSprites);
    }

    @Override
    public void clear() {
        super.clear();
        currentWarnList = null;
    }

    @Override
    public void initialize() {
        GUIP.addPreferenceChangeListener(this);
    }

    @Override
    public void dispose() {
        clear();
        GUIP.removePreferenceChangeListener(this);
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        if (e.getName().equals(GUIPreferences.CONSTRUCTOR_FACTOR_WARNING)) {
            setCFWarningSprites(currentWarnList);
        }
    }
}
