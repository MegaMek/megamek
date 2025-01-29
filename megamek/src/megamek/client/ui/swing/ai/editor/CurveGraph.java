/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.swing.ai.editor;

import megamek.ai.utility.Curve;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.concurrent.atomic.AtomicReference;

public class CurveGraph extends JPanel {

    private HoverStateModel hoverStateModel;
    private final AtomicReference<Curve> selectedCurve;

    public CurveGraph(AtomicReference<Curve> selectedCurve) {
        this.selectedCurve = selectedCurve;
        this.setHoverStateModel(new HoverStateModel());
    }

    public CurveGraph(HoverStateModel model, AtomicReference<Curve> selectedCurve) {
        this.selectedCurve = selectedCurve;
        this.hoverStateModel = model;
    }

    public void setHoverStateModel(HoverStateModel model) {
        if (this.hoverStateModel != null) {
            this.hoverStateModel.removeListener(this::repaint);
        }

        this.hoverStateModel = model;

        // React to mouse movement
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                double relativeX = e.getX() / (double) getWidth();
                hoverStateModel.setHoveringRelativeXPosition(relativeX); // Update shared state
            }
        });

        // React to hover state changes
        hoverStateModel.addListener(this::repaint);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (selectedCurve.get() == null) {
            return;
        }
        var curve = selectedCurve.get();

        curve.drawAxes(g, getWidth(), getHeight());
        curve.drawCurve(g, getWidth(), getHeight(), Color.BLUE);

        double hoverX = hoverStateModel.getHoveringRelativeXPosition();
        curve.drawPoint(g, getWidth(), getHeight(), Color.GREEN, hoverX);
    }

}
