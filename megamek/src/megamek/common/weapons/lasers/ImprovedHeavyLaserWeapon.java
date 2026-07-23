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

package megamek.common.weapons.lasers;

import java.io.Serial;

import megamek.common.options.IGameOptions;

/**
 * Base class for the Clan Improved Heavy Lasers (TO:AUE p.133). Improved Heavy Lasers are explosive and can be
 * deactivated per the activation/deactivation rules: a laser switched to {@code "Off"} cannot fire and does not
 * explode when critically hit. The switch may be declared at any time but only takes effect in the End Phase.
 */
public abstract class ImprovedHeavyLaserWeapon extends LaserWeapon {
    @Serial
    private static final long serialVersionUID = 4142894635724116817L;

    protected ImprovedHeavyLaserWeapon() {
        super();
        setModes("On", "Off");
        setInstantModeSwitch(false);
    }

    @Override
    public void adaptToGameOptions(IGameOptions gameOptions) {
        super.adaptToGameOptions(gameOptions);
        // LaserWeapon adds a "Pulse <mode>" variant of every non-pulse mode for the RISC Laser Pulse
        // Module; a deactivated laser cannot fire in pulse mode, so drop that combination
        removeMode("Pulse Off");
    }
}
