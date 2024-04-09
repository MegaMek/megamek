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

package megamek.client.ui.swing.util;

/**
 * This interface may be implemented by classes that receive keybind key presses, such as the
 * BoardView or PhaseDisplays to simplify registering those keybinds.
 */
public interface KeyBindReceiver {

    /**
     * This method should return true when this KeyBindReceiver is in a state to receive the keybind and may
     * react to it, false otherwise. This should return false when the receiver is hidden, inactive or behind
     * a modal dialog. It should return true when it would be expected to act on a keybind, i.e. when it
     * has the appearance of being in focus.
     *
     * When this method returns false, this receiver will not be counted as having consumed the key press.
     *
     * @return True when the this receiver is in a state to receive the keybind and may
     * react to it. False otherwise.
     */
    boolean shouldReceiveKeyCommands();
}
