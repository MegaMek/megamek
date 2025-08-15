/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.event;

import megamek.common.enums.GamePhase;

/**
 * Instances of this class are sent when Game phase changes
 */
public class GamePhaseChangeEvent extends GameEvent {
    private static final long serialVersionUID = 5589252062756476819L;

    /**
     * Old phase
     */
    private GamePhase oldPhase;

    /**
     * new phase
     */
    private GamePhase newPhase;

    /**
     * Constructs new <code>GamePhaseChangeEvent</code>
     *
     * @param source   Event source
     * @param oldPhase
     * @param newPhase
     */
    public GamePhaseChangeEvent(Object source, GamePhase oldPhase, GamePhase newPhase) {
        super(source);
        this.oldPhase = oldPhase;
        this.newPhase = newPhase;
    }

    /**
     * Returns the newPhase.
     *
     * @return the newPhase.
     */
    public GamePhase getNewPhase() {
        return newPhase;
    }

    /**
     * Returns the oldPhase.
     *
     * @return the oldPhase.
     */
    public GamePhase getOldPhase() {
        return oldPhase;
    }

    @Override
    public void fireEvent(GameListener gl) {
        gl.gamePhaseChange(this);
    }

    @Override
    public String getEventName() {
        return "Phase Change";
    }
}
