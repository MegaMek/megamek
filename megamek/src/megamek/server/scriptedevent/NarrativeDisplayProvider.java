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

package megamek.server.scriptedevent;

import java.awt.Image;

import megamek.common.annotations.Nullable;

/**
 * This interface is implemented by scripted event objects that show a story or informative messages in a dialog. This
 * is meant to give a common interface to MHQ's story arc NarrativeStoryPoint as well as scripted event messages in MM
 * so that both can be displayed using a common dialog.
 */
public interface NarrativeDisplayProvider {

    /**
     * @return A header text to show in the dialog. May be empty but not null.
     */
    String header();

    /**
     * @return The main narrative (story) text to show in the dialog. May be empty but not null.
     */
    String text();

    /**
     * @return A portrait or other image to show in the dialog
     */
    @Nullable
    Image portrait();

    /**
     * @return A splash image to show as part of the story
     */
    @Nullable
    Image splashImage();
}
