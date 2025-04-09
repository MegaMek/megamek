/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
 */
package megamek.client.ui.swing;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.Serial;
import javax.swing.AbstractAction;

import megamek.client.ui.Messages;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

/**
 * An {@link javax.swing.AbstractAction Action} for canceling a dialog (closing without action) using setVisible(false).
 * Will assign the Messages.Cancel text to the button.
 *
 * @author SJuliez
 */
public class CancelAction extends AbstractAction {
    private static final MMLogger logger = MMLogger.create(CancelAction.class);

    @Serial
    private static final long serialVersionUID = 1680850851585381148L;

    private Window owner;

    /**
     * Constructs a new <code>AbstractAction</code> that closes the Window myOwner when called. Assigns the
     * Messages.Cancel text to the button.
     */
    public CancelAction(Window myOwner) {
        owner = myOwner;
        putValue(NAME, Messages.getString("Cancel"));
    }

    @Override
    public @Nullable CancelAction clone() {
        try {

            CancelAction cancelAction = (CancelAction) super.clone();
            cancelAction.owner = this.owner;
            return cancelAction;
        } catch (CloneNotSupportedException e) {
            logger.error("Failed to clone CancelAction. State of the object: {}", this, e);
            return null;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        owner.setVisible(false);
    }

    @Override
    public String toString() {
        return "CancelAction{" +
                     "owner=" +
                     (owner != null ? owner.getClass().getSimpleName() : "null") +
                     ", name=" +
                     getValue(NAME) +
                     '}';
    }
}
