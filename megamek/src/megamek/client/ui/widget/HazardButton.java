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

package megamek.client.ui.widget;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;

import megamek.client.ui.util.UIUtil;

/**
 * A button styled like an emergency stop: a red face with bold yellow text, framed in yellow and red hazard stripes.
 *
 * <p>It is meant for the one control on a crowded strip that has to be findable at a glance by a player who has
 * just watched something go wrong - the bug reporting button - and should not be used for anything routine, or the
 * effect is lost.</p>
 *
 * <p>The face darkens while the mouse is over it and again while it is pressed, and fades to grey when the button is
 * disabled, so it still reads as a button rather than a label.</p>
 */
public class HazardButton extends JButton {

    private static final Color FACE = HazardStripeBorder.HAZARD_RED;
    private static final Color FACE_HOVER = FACE.darker();
    private static final Color FACE_PRESSED = FACE_HOVER.darker();
    private static final Color FACE_DISABLED = new Color(120, 120, 120);
    private static final Color TEXT = HazardStripeBorder.HAZARD_YELLOW;
    private static final Color TEXT_DISABLED = new Color(200, 200, 200);

    /** Breathing room between the striped border and the text, in unscaled pixels. */
    private static final int PADDING = 3;
    /** Radius of the face's rounded corners, in unscaled pixels. */
    private static final int CORNER = 6;

    private final HazardStripeBorder stripeBorder = new HazardStripeBorder();

    /**
     * @param text the button's label
     */
    public HazardButton(String text) {
        super(text);
        styleAsHazard();
    }

    /**
     * @param action the action the button performs, which also supplies its label and tooltip
     */
    public HazardButton(Action action) {
        super(action);
        styleAsHazard();
    }

    private void styleAsHazard() {
        setContentAreaFilled(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setForeground(TEXT);
        setFont(getFont().deriveFont(Font.BOLD));
        int padding = UIUtil.scaleForGUI(PADDING);
        setBorder(BorderFactory.createCompoundBorder(stripeBorder,
              BorderFactory.createEmptyBorder(padding, padding, padding, padding)));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D faceGraphics = (Graphics2D) graphics.create();
        try {
            faceGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            faceGraphics.setColor(faceColor());
            faceGraphics.fill(faceShape());
        } finally {
            faceGraphics.dispose();
        }
        setForeground(isEnabled() ? TEXT : TEXT_DISABLED);
        super.paintComponent(graphics);
    }

    private Color faceColor() {
        if (!isEnabled()) {
            return FACE_DISABLED;
        }
        if (getModel().isPressed()) {
            return FACE_PRESSED;
        }
        if (getModel().isRollover()) {
            return FACE_HOVER;
        }
        return FACE;
    }

    /** @return the area inside the hazard stripes, with rounded corners so the red face does not fight the frame */
    private Shape faceShape() {
        var stripeInsets = stripeBorder.getBorderInsets(this);
        int corner = UIUtil.scaleForGUI(CORNER);
        return new RoundRectangle2D.Float(stripeInsets.left, stripeInsets.top,
              getWidth() - stripeInsets.left - stripeInsets.right,
              getHeight() - stripeInsets.top - stripeInsets.bottom, corner, corner);
    }
}
