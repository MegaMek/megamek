/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.server.trigger;

import megamek.common.IGame;

/**
 * This interface is implemented by Triggers that enable events like messages, game end, defeat or victory.
 */
public interface Trigger {

    /**
     * Returns true when this Trigger is triggered, i.e. if the prerequisites for its associated event are deemed to be
     * satisfied. Note that the event is itself responsible for making sure that it only returns true once, if it should
     * only trigger once. If it returns true on multiple occasions, the triggered event may happen multiple times. The
     * given TriggerEvent specifies when at what moment of the game this method is called.
     *
     * @param game  The game
     * @param event The type of event that caused the trigger to be called
     *
     * @return True when this Trigger considers itself triggered, false otherwise
     */
    boolean isTriggered(IGame game, TriggerSituation event);
}
