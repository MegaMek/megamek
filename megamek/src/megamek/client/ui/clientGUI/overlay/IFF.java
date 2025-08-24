/*
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

package megamek.client.ui.clientGUI.overlay;

import java.awt.Color;

import megamek.common.units.Entity;
import megamek.common.Player;

public enum IFF {
    PLAYER(Color.GREEN, new Color(0x005f00)),
    ALLY(Color.YELLOW, new Color(0x5f5f00)),
    ENEMY(Color.RED, new Color(0x5f0000));

    private final Color color;
    private final Color darkColor;

    IFF(Color color, Color darkColor) {
        this.color = color;
        this.darkColor = darkColor;
    }

    public Color getColor() {
        return color;
    }

    public Color getDarkColor() {
        return darkColor;
    }

    public static IFF getIFFStatus(Entity entity, Player player) {
        if (entity.getOwner().isEnemyOf(player)) {
            return ENEMY;
        } else if (entity.getOwner().equals(player) || entity.getOwner().getName().contains("@AI")) {
            return PLAYER;
        } else {
            return ALLY;
        }
    }

    public static IFF getPlayerIff(Player localPlayer, Player player) {
        if (player.isEnemyOf(localPlayer)) {
            return ENEMY;
        } else if (player.equals(localPlayer) || player.getName().contains("@AI")) {
            return PLAYER;
        } else {
            return ALLY;
        }
    }
}
