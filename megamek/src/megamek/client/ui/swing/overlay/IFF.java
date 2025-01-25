/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.client.ui.swing.overlay;

import megamek.common.Entity;
import megamek.common.Player;

import java.awt.*;

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
