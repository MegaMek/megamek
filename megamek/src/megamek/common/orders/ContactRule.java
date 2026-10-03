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
package megamek.common.orders;

/**
 * What a lance does on a leg of its route when it comes under fire - any of its units hit by enemy fire the turn
 * before (HammerGS, 2026-09-27). The names are kept from when this was the formation's rule on contact, so saves read
 * as before: a Break then read as Break and fight, a Hold as Push through.
 */
public enum ContactRule {
    /** Push through: keep moving and keep the route's facing, firing back only with torso twists and turrets. */
    HOLD,
    /** Turn and fire: keep moving along the route, but turn to bring the attackers into the front arc. */
    TURN_AND_FIRE,
    /** Break and fight: leave the route to fight the attackers, then hold until given the Resume order. */
    BREAK
}
