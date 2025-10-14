/*
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013 Nicholas Walczak (walczak@cs.umn.edu)
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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
 * Class used in processing KeyEvents in <code>MegamekController</code>.  A hotkey defined in
 * <code>MegamekController</code> consists of a key and a command, the command is then linked to a CommandAction that
 * carriers out the desired action.
 *
 * @author arlith
 */
public interface CommandAction {

    /**
     * Returns true when the registered receiver of the keybind is in a state to receive the keybind and may react to
     * it, false otherwise. This should return false when the receiver is hidden, inactive or behind a modal dialog. It
     * should return true when it would be expected to act on a keybind, i.e. when it has the appearance of being in
     * focus.
     * <p>
     * When this method returns false, it will not be counted as having consumed the key press.
     *
     * @return True when the registered receiver of the keybind is in a state to receive the keybind and may react to
     *       it. False otherwise.
     */
    default boolean shouldReceiveAction() {
        return true;
    }

    /**
     * Called on a key press when the registered receiver of the keybind is allowed to receive a key press according to
     * the return value of shouldReceiveAction().
     *
     * @see #shouldReceiveAction()
     */
    void performAction();

    /**
     * Called on a key release when the registered receiver of the keybind is allowed to receive it according to the
     * return value of shouldReceiveAction(). Note that it may happen that the receiver was allowed to receive the key
     * press but not the key release. By default, this method does nothing.
     *
     * @see #shouldReceiveAction()
     */
    default void releaseAction() {}
}
