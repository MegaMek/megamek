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
package megamek.client.bot.princess;

/**
 * A player's standing order to a bot's DropShips and small craft: get in the air, get on the
 * ground, or leave it to doctrine.
 *
 * <p>Issued through the {@code AERO_LIFT} chat command (and, in a later UI, the bot commands
 * panel). Fighters are unaffected - a grounded fighter's job is always to get back up, and the
 * takeoff doctrine handles it without orders. DropShips and small craft are different: one on the
 * ground may be doing its job (cargo, fortress fire support), so under {@link #AUTO} they stay
 * where they are and only a player order moves them between ground and sky. Runtime state like
 * hold-position, not saved configuration.</p>
 */
public enum AerospaceGroundOrder {

    /** Grounded DropShips and small craft take off when it is safe and legal to do so. */
    LIFT_OFF,

    /** Airborne DropShips and small craft land when it is safe and legal to do so. */
    LAND,

    /** No standing order: DropShips and small craft hold their current domain. The default. */
    AUTO
}
