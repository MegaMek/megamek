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

    abstract protected void setDoneButtonValid();

    abstract protected void setDoneButtonSkip();

    protected void updateDonePanel()
    {
        if (attacks.isEmpty()) {
            setDoneButtonSkip();
        } else {
            setDoneButtonValid();
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
