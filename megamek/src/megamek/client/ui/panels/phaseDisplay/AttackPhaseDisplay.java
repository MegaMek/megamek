/*
 * Copyright (C) 2023-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.panels.phaseDisplay;

import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.tooltip.EntityActionLog;
import megamek.common.actions.EntityAction;
import megamek.common.actions.TorsoTwistAction;

public abstract class AttackPhaseDisplay extends ActionPhaseDisplay {

    // client list of attacks user has input
    protected EntityActionLog attacks;

    protected AttackPhaseDisplay(ClientGUI cg) {
        super(cg);
        attacks = new EntityActionLog(game);
    }

    /**
     * called by updateDonePanel to populate the label text of the Done button. Usually wraps a call to
     * Messages.getString(...Fire) but can be extended to add more details.
     *
     * @return text for label
     */
    abstract protected String getDoneButtonLabel();

    /**
     * called by updateDonePanel to populate the label text of the NoAction button. Usually wraps a call to
     * Messages.getString(...Skip) but can be extended to add more details.
     *
     * @return text for label
     */
    abstract protected String getSkipTurnButtonLabel();

    @Override
    protected void updateDonePanel() {
        if (attacks.isEmpty() || ((attacks.size() == 1) && (attacks.firstElement() instanceof TorsoTwistAction))) {
            // a torso twist alone should not trigger Done button
            updateDonePanelButtons(getDoneButtonLabel(), getSkipTurnButtonLabel(), false, null);
        } else {
            updateDonePanelButtons(getDoneButtonLabel(), getSkipTurnButtonLabel(), true, attacks.getDescriptions());
        }
    }

    protected void removeAttack(EntityAction o) {
        attacks.remove(o);
        updateDonePanel();
    }

    /** removes all elements from the local temporary attack list */
    protected void removeAllAttacks() {
        attacks.clear();
        updateDonePanel();
    }

    /** add an attack at the given index to the local temporary attack list */
    protected void addAttack(int index, EntityAction entityAction) {
        attacks.add(index, entityAction);
        updateDonePanel();
    }

    /** add an attack to the end of the local temporary attack list */
    protected void addAttack(EntityAction entityAction) {
        attacks.add(entityAction);
        updateDonePanel();
    }
}
