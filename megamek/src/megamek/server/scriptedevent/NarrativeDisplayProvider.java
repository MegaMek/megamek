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
package megamek.server.scriptedevent;

import megamek.common.annotations.Nullable;

import java.awt.*;

/**
 * This interface is implemented by scripted event objects that show a story or informative messages in
 * a dialog. This is meant to give a common interface to MHQ's story arc NarrativeStoryPoint as well as
 * scripted event messages in MM so that both can be displayed using a common dialog.
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
