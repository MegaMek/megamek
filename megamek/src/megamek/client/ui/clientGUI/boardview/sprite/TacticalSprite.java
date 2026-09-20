/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.sprite;

import java.awt.Graphics2D;

import megamek.client.ui.clientGUI.boardview.BoardTactical;

/** A sprite whose existing painter can supply unscaled board vectors without preparing a raster image. */
public interface TacticalSprite {
    void drawTactical(Graphics2D graphics);

    default BoardTactical.Playback playback() {
        return BoardTactical.Playback.HIDE_DURING_MOVEMENT;
    }
}
