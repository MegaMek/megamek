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
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.function.Consumer;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * A phase display button in emergency-stop colours: the usual skinned frame and brushed face, but tinted red, with
 * the label in bold yellow.
 *
 * <p>It is meant for the one control on a crowded strip that has to be findable at a glance by a player who has
 * just watched something go wrong - the bug reporting button - and should not be used for anything routine, or the
 * effect is lost.</p>
 *
 * <p>The skin's own images are reused rather than copied and recoloured on disk: everything the parent paints,
 * face and frame alike, is rendered to an offscreen image and each pixel is remapped by its brightness onto a
 * dark-red to yellow ramp. That keeps the shape identical to the buttons beside it whatever skin is in use, and the
 * pressed image darkens the red just as it darkens the grey.</p>
 */
public class HazardButton extends MegaMekButton {

    @Serial
    private static final long serialVersionUID = 2817492054981104913L;

    private static final Color LABEL = new Color(255, 221, 0);
    private static final Color LABEL_HOVER = new Color(255, 245, 150);
    private static final Color LABEL_DISABLED = new Color(160, 110, 40);

    /** Brightness at and above which the ramp starts bending from red towards yellow, for the bevel highlights. */
    private static final float HIGHLIGHT_START = 0.7f;
    /** How much redder than blue a source pixel has to be to count as part of the skin's warm-coloured frame. */
    private static final int FRAME_WARMTH = 40;
    /** Brightness multiplier for frame pixels, so the bevel reads as a bright yellow rather than a muddy one. */
    private static final float FRAME_LIFT = 1.6f;

    /** While this is set the parent paints no text, so that the label can be drawn in the hazard colour instead. */
    private boolean hidingTextForTint;

    /**
     * The tinted face and frame, kept between repaints. Tinting walks every pixel, so it is done once per size and
     * pressed state rather than on every hover or expose; the skin's own images do not change otherwise.
     */
    private final TintedImage tintedFace = new TintedImage();
    private final TintedImage tintedFrame = new TintedImage();

    /**
     * @param text the button's label
     */
    public HazardButton(String text) {
        super(text, SkinSpecification.UIComponents.PhaseDisplayButton.getComp());
        setFont(getFont().deriveFont(Font.BOLD));
    }

    @Override
    public String getText() {
        return hidingTextForTint ? "" : super.getText();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        paintTinted(graphics, tintedFace, super::paintComponent);

        JLabel textLabel = new JLabel(super.getText(), SwingConstants.CENTER);
        textLabel.setSize(getSize());
        textLabel.setFont(getFont().deriveFont(Font.BOLD));
        textLabel.setForeground(labelColor());
        textLabel.paint(graphics);
    }

    @Override
    protected void paintBorder(Graphics graphics) {
        paintTinted(graphics, tintedFrame, super::paintBorder);
    }

    private Color labelColor() {
        if (!isEnabled()) {
            return LABEL_DISABLED;
        }
        return (isMousedOver || hasFocus()) ? LABEL_HOVER : LABEL;
    }

    /**
     * Draws the tinted version of what the given painter paints, rebuilding it only when the button's size or pressed
     * state has changed since it was last built.
     *
     * @param graphics the graphics to draw the tinted result on
     * @param cache    where the tinted image for this painter is kept between repaints
     * @param painter  the parent's painting step, handed an offscreen graphics when a rebuild is needed
     */
    private void paintTinted(Graphics graphics, TintedImage cache, Consumer<Graphics> painter) {
        int width = getWidth();
        int height = getHeight();
        if ((width <= 0) || (height <= 0)) {
            return;
        }
        if (!cache.matches(width, height, isPressed)) {
            cache.update(renderTinted(width, height, painter), isPressed);
        }
        graphics.drawImage(cache.image, 0, 0, null);
    }

    /**
     * Runs the given painter into a fresh offscreen image and remaps every pixel onto the hazard ramp.
     *
     * @param width   the button's current width
     * @param height  the button's current height
     * @param painter the parent's painting step
     *
     * @return the tinted image
     */
    private BufferedImage renderTinted(int width, int height, Consumer<Graphics> painter) {
        BufferedImage offscreen = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D offscreenGraphics = offscreen.createGraphics();
        try {
            hidingTextForTint = true;
            painter.accept(offscreenGraphics);
        } finally {
            hidingTextForTint = false;
            offscreenGraphics.dispose();
        }

        int[] pixels = offscreen.getRGB(0, 0, width, height, null, 0, width);
        for (int index = 0; index < pixels.length; index++) {
            pixels[index] = tint(pixels[index]);
        }
        offscreen.setRGB(0, 0, width, height, pixels, 0, width);
        return offscreen;
    }

    /**
     * Maps one ARGB pixel onto the hazard ramp by its brightness: shadows stay dark red, the face is red, and the
     * brightest bevel highlights turn yellow. Alpha is kept, so transparent corners stay transparent.
     *
     * @param argb the source pixel
     *
     * @return the tinted pixel
     */
    static int tint(int argb) {
        int alpha = (argb >>> 24) & 0xFF;
        if (alpha == 0) {
            return argb;
        }
        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = argb & 0xFF;
        float brightness = ((0.299f * red) + (0.587f * green) + (0.114f * blue)) / 255f;

        int tintedRed;
        int tintedGreen;
        int tintedBlue;
        if ((red - blue) > FRAME_WARMTH) {
            // The skin draws its bevel and corner pieces in warm orange; those become the yellow of the frame.
            float lift = Math.min(1f, brightness * FRAME_LIFT);
            tintedRed = Math.round(120 + (135 * lift));
            tintedGreen = Math.round(90 + (130 * lift));
            tintedBlue = 0;
        } else {
            // Everything else - the brushed grey face and its shadows - becomes the red of the face.
            tintedRed = Math.round(70 + (185 * brightness));
            tintedGreen = Math.round(10 + (30 * brightness));
            tintedBlue = Math.round(5 + (15 * brightness));
            if (brightness > HIGHLIGHT_START) {
                float highlight = (brightness - HIGHLIGHT_START) / (1f - HIGHLIGHT_START);
                tintedGreen = Math.round(tintedGreen + (highlight * 180));
            }
        }
        return (alpha << 24) | (Math.min(255, tintedRed) << 16) | (Math.min(255, tintedGreen) << 8)
              | Math.min(255, tintedBlue);
    }

    /** A tinted image together with the size and pressed state it was built for. */
    private static class TintedImage {
        private BufferedImage image;
        private boolean pressed;

        boolean matches(int width, int height, boolean isPressed) {
            return (image != null) && (image.getWidth() == width) && (image.getHeight() == height)
                  && (pressed == isPressed);
        }

        void update(BufferedImage newImage, boolean isPressed) {
            image = newImage;
            pressed = isPressed;
        }
    }
}
