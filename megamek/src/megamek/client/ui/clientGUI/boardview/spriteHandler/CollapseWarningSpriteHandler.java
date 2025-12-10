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
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.sprite.CollapseWarningSprite;
import megamek.common.board.BoardLocation;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;

public class CollapseWarningSpriteHandler extends BoardViewSpriteHandler implements IPreferenceChangeListener {

    // Cache the warn list; thus, when CF warning is turned on the sprites can easily be created
    private Collection<BoardLocation> currentWarnList;

    public CollapseWarningSpriteHandler(AbstractClientGUI clientGUI) {
        super(clientGUI);
    }

    public void setCFWarningSprites(Collection<BoardLocation> warnList) {
        clear();
        if (clientGUI.boardViews().isEmpty()) {
            return;
        }
        currentWarnList = warnList;
        if ((warnList != null) && GUIP.getShowCFWarnings()) {
            warnList.stream()
                  .map(location -> new CollapseWarningSprite(
                        (BoardView) clientGUI.getBoardView(location), location.coords()))
                  .forEach(currentSprites::add);
        }
        currentSprites.forEach(sprite -> sprite.bv.addSprite(sprite));
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
