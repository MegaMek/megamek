/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of <Package Name>.
 *
 * <Package Name> is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * <Package Name> is distributed in the hope that it will be useful,
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
 * MechWarrior Copyright Microsoft Corporation. <Package Name> was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.swing.boardview.overlay;

import java.awt.*;

import megamek.client.ui.IDisplayable;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.boardview.BoardView;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.util.ImageUtil;

public class TraceOverlay implements IDisplayable, IPreferenceChangeListener {
    private boolean visible;
    private Image traceImage;
    private final BoardView boardView;
    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    public TraceOverlay(BoardView boardView) {
        this.boardView = boardView;

        visible = GUIP.getShowTraceOverlay();
        GUIP.addPreferenceChangeListener(this);

        traceImage = ImageUtil.loadImageFromFile(GUIP.getTraceOverlayImageFile());
    }

    @Override
    public void draw(Graphics graph, Rectangle clipBounds) {
        if (!visible || traceImage == null) {
            return;
        }

        double scale = GUIP.getTraceOverlayScale();
        int x = GUIP.getTraceOverlayOriginX();
        int y = GUIP.getTraceOverlayOriginY();
        float alpha = GUIP.getTraceOverlayTransparency() / 255f;

        int widthScaled = (int) Math.floor(traceImage.getWidth(null) * scale);
        int heightScaled = (int) Math.floor(traceImage.getHeight(null) * scale);

        ((Graphics2D) graph).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        graph.drawImage(traceImage, x, y, widthScaled, heightScaled, boardView.getPanel());
        ((Graphics2D) graph).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        if (e.getName().equals(GUIPreferences.SHOW_TRACE_OVERLAY)) {
            visible = GUIP.getShowTraceOverlay();
            boardView.refreshDisplayables();
        } else if (e.getName().equals(GUIPreferences.TRACE_OVERLAY_TRANSPARENCY)) {
            boardView.refreshDisplayables();
        } else if (e.getName().equals(GUIPreferences.TRACE_OVERLAY_SCALE)) {
            boardView.refreshDisplayables();
        } else if (e.getName().equals(GUIPreferences.TRACE_OVERLAY_ORIGIN_X)) {
            boardView.refreshDisplayables();
        } else if (e.getName().equals(GUIPreferences.TRACE_OVERLAY_ORIGIN_Y)) {
            boardView.refreshDisplayables();
        } else if (e.getName().equals(GUIPreferences.TRACE_OVERLAY_IMAGE_FILE)) {
            traceImage = ImageUtil.loadImageFromFile(GUIP.getTraceOverlayImageFile());
            boardView.refreshDisplayables();
        }
    }
}
