/*
 * MegaMek -
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013 Nicholas Walczak (walczak@cs.umn.edu)
 * Copyright (C) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing.util;

/**
 * Class used in processing KeyEvents in <code>MegamekController</code>.  A 
 * hotkey defined in <code>MegamekController</code> consists of a key and a 
 * command, the command is then linked to a CommandAction that carriers out the
 * desired action.
 * 
 * @author arlith
 */
public interface CommandAction {

    /**
     * Returns true when the registered receiver of the keybind is in a state to receive the keybind and may
     * react to it, false otherwise. This should return false when the receiver is hidden, inactive or behind
     * a modal dialog. It should return true when it would be expected to act on a keybind, i.e. when it
     * has the appearance of being in focus.
     *
     * When this method returns false, it will not be counted as having consumed the key press.
     *
     * @return True when the registered receiver of the keybind is in a state to receive the keybind and may
     * react to it. False otherwise.
     */
    default boolean shouldReceiveAction() {
        return true;
    }

    /**
     * Called on a key press when the registered receiver of the keybind is allowed to receive a key press
     * according to the return value of shouldReceiveAction().
     *
     * @see #shouldReceiveAction()
     */
    void performAction();

    /**
     * Called on a key release when the registered receiver of the keybind is allowed to receive it
     * according to the return value of shouldReceiveAction(). Note that it may happen that the receiver
     * was allowed to receive the key press but not the key release.
     * By default, this method does nothing.
     *
     * @see #shouldReceiveAction()
     */
    default void releaseAction() { }
}