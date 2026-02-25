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

package megamek.client.ui.util;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

/**
 * This class can be used to simplify creating a Java Swing Action if it only requires a name and its action handling
 * can be given as a method reference. This is not useful if the Action requires more parameters or needs to track its
 * state (enabled/disabled).
 */
public final class LambdaAction extends AbstractAction {

    private final Consumer<ActionEvent> handler;

    /**
     * Creates an Action with the given name and an event handler when the action is used (clicked). The given name
     * appears in menus or buttons where its added (so, should use i18n).
     *
     * @param name    The name of the action for buttons and menus
     * @param handler A method reference that is called when the action is invoked
     */
    public LambdaAction(String name, Consumer<ActionEvent> handler) {
        super(name);
        this.handler = handler;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        handler.accept(e);
    }
}
