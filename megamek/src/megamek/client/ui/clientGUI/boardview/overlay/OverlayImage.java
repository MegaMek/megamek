/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.overlay;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/** Native-density artwork and placement, borrowed on the Swing thread until the next overlay capture. */
public record OverlayImage(BufferedImage image, int x, int y, Fade fade, Transition shiftY) {
    public OverlayImage(BufferedImage image, int x, int y, Fade fade) {
        this(image, x, y, fade, Transition.ZERO);
    }

    /** Draw the same native-density layer in the classic view without baking animation into its artwork. */
    public void draw(Graphics2D graphics, long now) {
        Graphics2D painter = (Graphics2D) graphics.create();
        try {
            painter.setTransform(new AffineTransform());
            painter.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fade.opacity(now)));
            painter.drawImage(image, AffineTransform.getTranslateInstance(x, y + shiftY.value(now)), null);
        } finally {
            painter.dispose();
        }
    }

    /** Immutable timing shared by Swing and GPU rendering; neither renderer advances the other's animation. */
    public record Transition(long startedNanos, float from, float to, long durationNanos) {
        public static final Transition ZERO = new Transition(0, 0, 0, 0);
        public static final Transition ONE = new Transition(0, 1, 1, 0);

        public float value(long now) {
            if (from == to || durationNanos == 0) {
                return to;
            }
            float progress = Math.clamp((now - startedNanos) / (float) durationNanos, 0, 1);
            float eased = progress * progress * (3 - 2 * progress);
            return from + (to - from) * eased;
        }

        public boolean isAnimating(long now) {
            return from != to && now >= startedNanos && now - startedNanos < durationNanos;
        }

        public Transition scaled(float scale) {
            return new Transition(startedNanos, from * scale, to * scale, durationNanos);
        }
    }

    /** An optional scheduled expiry lets a toast complete its entire lifetime without another Swing snapshot. */
    public record Fade(Transition transition, Transition expiry) {
        public static final Fade OPAQUE = new Fade(Transition.ONE, Transition.ONE);

        public Fade(long startedNanos, float from, float to) {
            this(new Transition(startedNanos, from, to, 200_000_000), Transition.ONE);
        }

        public float opacity(long now) {
            return transition.value(now) * expiry.value(now);
        }

        public boolean isAnimating(long now) {
            return transition.isAnimating(now) || expiry.isAnimating(now);
        }
    }
}
