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

import megamek.client.ui.IDisplayable;
import megamek.common.Player;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ChatOverlay implements IDisplayable {
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
            messages.add(0,  ">> " + message);
        } else {
            messages.add(0, player.getName() + "> " + message);
        }
    }

    @Override
    public void draw(Graphics graph, Rectangle rect) {
        Graphics2D g2d = (Graphics2D) graph.create();
        g2d.setColor(new Color(255, 255, 255, 100));
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i) != null) {
                g2d.drawString(messages.get(i), 10, 15 + i * 20);
            }
        }
    }
}
