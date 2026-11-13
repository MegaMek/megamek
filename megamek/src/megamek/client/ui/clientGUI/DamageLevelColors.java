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
package megamek.client.ui.clientGUI;

import java.awt.Color;

import jakarta.annotation.Nullable;
import megamek.common.units.Entity;

/**
 * Maps a unit's damage level to the colour used to show it. The board's unit label and the mini-map's damage pip both
 * use this, so the same damage level always has the same colour.
 */
public final class DamageLevelColors {

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    private DamageLevelColors() {
    }

    /**
     * @param entity the unit to look at
     *
     * @return the colour for the unit's current damage level, or {@code null} when it is undamaged
     */
    public static @Nullable Color forEntity(Entity entity) {
        return forDamageLevel(entity.getDamageLevel());
    }

    /**
     * @param damageLevel one of the {@code Entity.DMG_*} constants
     *
     * @return the colour for that damage level, or {@code null} for {@link Entity#DMG_NONE} and unknown values
     */
    public static @Nullable Color forDamageLevel(int damageLevel) {
        return switch (damageLevel) {
            case Entity.DMG_CRIPPLED -> Color.BLACK;
            case Entity.DMG_HEAVY -> GUIP.getWarningColor();
            case Entity.DMG_MODERATE -> GUIP.getCautionColor();
            case Entity.DMG_LIGHT -> Color.GREEN;
            default -> null;
        };
    }
}
