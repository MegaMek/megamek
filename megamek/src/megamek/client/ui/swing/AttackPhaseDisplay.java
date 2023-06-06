/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing;

import megamek.common.actions.*;

import java.util.*;

import static megamek.client.ui.swing.util.UIUtil.guiScaledFontHTML;

public abstract class AttackPhaseDisplay extends ActionPhaseDisplay {
    // client list of attacks user has input
    protected Vector<EntityAction> attacks;

    protected AttackPhaseDisplay(ClientGUI cg) {
        super(cg);
    }

    /**
     * called by updateDonePanel to populate the label text of the Done button. Usually wraps a call to Messages.getString(...Fire)
     * but can be extended to add more details.
     * @return text for label
     */
    abstract protected String getDoneButtonLabel();

    /**
     * called by updateDonePanel to populate the label text of the NoAction button. Usually wraps a call to Messages.getString(...Skip)
     * but can be extended to add more details.
     * @return text for label
     */
    abstract protected String getSkipTurnButtonLabel();


    @Override
    protected void updateDonePanel()
    {
        if (attacks.isEmpty() || (attacks.size() == 1 && attacks.firstElement() instanceof TorsoTwistAction)){
            // a torso twist alone should not trigger Done button
            updateDonePanelButtons(getDoneButtonLabel(), getSkipTurnButtonLabel(), false, null);
        } else {
            StringBuilder tooltip = new StringBuilder();
            for (var a : attacks) {
                tooltip.append(a.toDisplayableString(clientgui.getClient()));
                tooltip.append("<br>");
            }
            updateDonePanelButtons(getDoneButtonLabel(), getSkipTurnButtonLabel(), true, tooltip.toString());
        }
    }

    protected void removeAttack(Object o)
    {
        attacks.remove(o);
        updateDonePanel();
    }

    /** removes all elements from the local temporary attack list */
    protected void removeAllAttacks()
    {
        attacks.removeAllElements();
        updateDonePanel();
    }

    /** add an attack at the given index to the local temporary attack list */
    protected void addAttack(int index, EntityAction entityAction)
    {
        attacks.add(index, entityAction);
        updateDonePanel();
    }

    /** add an attack to the end of the local temporary attack list */
    protected void addAttack(EntityAction entityAction)
    {
        attacks.add(entityAction);
        updateDonePanel();
    }
}
