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
 * A player's standing order to a bot's flight: what its priority target set is this battle.
 *
 * <p>Issued through the {@code AERO_FOCUS} chat command (and, in a later UI, the bot commands
 * panel). This is a battle order, not saved configuration - it lives as runtime state on the bot,
 * like hold-position, and resets with the bot client.</p>
 *
 * <p>Orders bias, they never blind: the doctrine consumes the focus as a multiplier pair on its
 * air-to-air and air-to-ground credit sets, so a Focus-Ground fighter with a free shot at a laden
 * bomber can still take it. The measured basis for shipping this as an order rather than a
 * doctrine default: the intercept tuning run of 2026-08-14, where a standing air-priority
 * equivalent (intercept weight 0.6) won the air war and lost the battle 0.71:1 - the fighters
 * chased enemy air while their ground lance died unsupported. Air superiority at ground cost is a
 * choice the player makes, not one the doctrine assumes.</p>
 */
public enum AerospaceFocus {

    /** Press the air battle first: air-to-air credit doubled, ground-attack credit quartered. */
    AEROSPACE,

    /** Support the ground force first: ground-attack credit doubled, air-to-air credit quartered. */
    GROUND,

    /** No standing order - the doctrine weighs both halves itself. The default. */
    AUTO
}
