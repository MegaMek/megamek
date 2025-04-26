/*
 * MegaMek - Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
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
