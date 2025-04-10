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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serial;
import javax.swing.AbstractAction;

import megamek.client.ui.Messages;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

/**
 * An {@link javax.swing.AbstractAction Action} for getting an Okay/Done response from a button in a dialog Assigns the
 * Messages.Okay text to the button. When this action is activated, sends OkayAction.OKAY as the command string.
 *
 * @author SJuliez
 */
public class OkayAction extends AbstractAction {
    private static final MMLogger logger = MMLogger.create(OkayAction.class);

    @Serial
    private static final long serialVersionUID = 1680850851585381148L;

    public final static String OKAY = "OkayAction.Okay";

    private ActionListener owner;

    /**
     * Constructs a new <code>AbstractAction</code> that forwards an Okay to myOwner. Assigns the Messages.Okay text to
     * the button. The forwarded event has the actionCommand: <code>OkayAction.OKAY</code>.
     */
    public OkayAction(ActionListener myOwner) {
        owner = myOwner;
        putValue(NAME, Messages.getString("Okay"));
    }

    @Override
    public @Nullable OkayAction clone() {
        try {
            OkayAction okayAction = (OkayAction) super.clone();
            okayAction.owner = this.owner;
            return okayAction;
        } catch (CloneNotSupportedException e) {
            logger.error("Failed to clone OkayAction. State of the object: {}", this, e);
            return null;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ActionEvent f = new ActionEvent(e.getSource(), e.getID(), OKAY);
        owner.actionPerformed(f);
    }

    @Override
    public String toString() {
        return "OkayAction{" +
                     "owner=" +
                     (owner != null ? owner.getClass().getSimpleName() : "null") +
                     ", name=" +
                     getValue(NAME) +
                     ", actionCommand=" +
                     OKAY +
                     '}';
    }
}
