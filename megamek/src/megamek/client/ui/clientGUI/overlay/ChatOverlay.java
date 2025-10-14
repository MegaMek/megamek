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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import megamek.common.Player;

public class ChatOverlay implements OverlayPanel {
    private final List<String> messages;
    private final int limit;

    public ChatOverlay(int lines) {
        messages = new ArrayList<>(lines);
        this.limit = lines;
    }

    public void addChatMessage(Player player, String message) {
        if (messages.size() > limit) {
            messages.remove(messages.size() - 1);
        }
        if (player == null) {
            messages.add(0, ">> " + message);
        } else {
            messages.add(0, player.getName() + "> " + message);
        }
    }

    @Override
    public void paint(Graphics g, int width, int height) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setColor(new Color(255, 255, 255, 100));
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i) != null) {
                g2d.drawString(messages.get(i), 10, 15 + i * 20);
            }
        }
    }
}
