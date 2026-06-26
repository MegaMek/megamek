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

package megamek.client.ui.util;

/**
 * This interface may be implemented by classes that receive keybind key presses, such as the BoardView or PhaseDisplays
 * to simplify registering those keybinds.
 */
public interface KeyBindReceiver {

    /**
     * This method should return true when this KeyBindReceiver is in a state to receive the keybind and may react to
     * it, false otherwise. This should return false when the receiver is hidden, inactive or behind a modal dialog. It
     * should return true when it would be expected to act on a keybind, i.e. when it has the appearance of being in
     * focus.
     * <p>
     * When this method returns false, this receiver will not be counted as having consumed the key press.
     *
     * @return True when the receiver is in a state to receive the keybind and may react to it. False otherwise.
     */
    boolean shouldReceiveKeyCommands();
}
