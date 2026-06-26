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
package megamek.client.ui.clientGUI.boardview.overlay;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import megamek.MMConstants;
import megamek.client.ui.IDisplayable;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.util.StringDrawer;
import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.units.Entity;

/**
 * A board view overlay that displays transient toast notifications. Messages appear at the top-center of the viewport,
 * optionally showing the acting unit's sprite icon, then fade out after a configurable duration. Multiple messages
 * stack vertically, each on its own independent timer.
 *
 * <p>Thread-safe: {@link #show} may be called from any thread. Rendering and animation run on the EDT.</p>
 */
public class BoardToastOverlay implements IDisplayable {

    private static final int FADE_IN_MS = 300;
    private static final int FADE_OUT_MS = 1000;
    private static final int MAX_VISIBLE = 5;
    private static final int ICON_WIDTH = 56;
    private static final int ICON_HEIGHT = 48;
    private static final int ICON_TEXT_GAP = 8;
    private static final int PADDING_X = 12;
    private static final int PADDING_Y = 8;
    private static final int TOAST_GAP = 6;
    private static final int DIST_TOP = 20;
    private static final int CORNER_RADIUS = 10;
    private static final int BG_ALPHA = 180;
    private static final float SLIDE_SPEED = 0.15f;
    private static final Font FONT = new Font(MMConstants.FONT_SANS_SERIF, Font.BOLD, 13);

    private enum ToastPhase {FADE_IN, HOLD, FADE_OUT, DONE}

    private static class ToastMessage {
        final String text;
        final ToastLevel level;
        final int entityId;
        final int durationMs;
        float alpha;
        float targetY;
        float currentY;
        ToastPhase phase;
        long phaseStartTimeMs;

        ToastMessage(String text, ToastLevel level, int entityId, int durationMs) {
            this.text = text;
            this.level = level;
            this.entityId = entityId;
            this.durationMs = durationMs;
            this.alpha = 0;
            this.phase = ToastPhase.FADE_IN;
            this.phaseStartTimeMs = System.currentTimeMillis();
        }
    }

    private final BoardView boardView;
    private final ClientGUI clientGui;
    private final ConcurrentLinkedQueue<ToastMessage> pendingToasts = new ConcurrentLinkedQueue<>();
    private final List<ToastMessage> activeToasts = new ArrayList<>();

    public BoardToastOverlay(BoardView boardView, ClientGUI clientGui) {
        this.boardView = boardView;
        this.clientGui = clientGui;
    }

    /**
     * Shows a toast notification with no associated entity icon, using the level's default duration.
     *
     * @param level the severity level determining color and default duration
     * @param text  the message text to display
     */
    public void show(ToastLevel level, String text) {
        show(level, text, null, level.getDefaultDurationMs());
    }

    /**
     * Shows a toast notification, optionally displaying the given entity's sprite icon, using the level's default
     * duration.
     *
     * @param level  the severity level determining color and default duration
     * @param text   the message text to display
     * @param entity the entity whose icon to show, or null for text-only
     */
    public void show(ToastLevel level, String text, @Nullable Entity entity) {
        show(level, text, entity, level.getDefaultDurationMs());
    }

    /**
     * Shows a toast notification with explicit duration control.
     *
     * @param level      the severity level determining color
     * @param text       the message text to display
     * @param entity     the entity whose icon to show, or null for text-only
     * @param durationMs how long the toast remains visible in milliseconds
     */
    public void show(ToastLevel level, String text, @Nullable Entity entity, int durationMs) {
        int entityId = (entity != null) ? entity.getId() : -1;
        pendingToasts.add(new ToastMessage(text, level, entityId, durationMs));
        boardView.getPanel().repaint();
    }

    @Override
    public boolean isSliding() {
        return !pendingToasts.isEmpty() || !activeToasts.isEmpty();
    }

    @Override
    public boolean slide() {
        drainPending();

        long now = System.currentTimeMillis();
        boolean animating = false;

        for (ToastMessage toast : activeToasts) {
            long elapsed = now - toast.phaseStartTimeMs;
            switch (toast.phase) {
                case FADE_IN:
                    toast.alpha = Math.min(1.0f, elapsed / (float) FADE_IN_MS);
                    if (elapsed >= FADE_IN_MS) {
                        toast.phase = ToastPhase.HOLD;
                        toast.phaseStartTimeMs = now;
                        toast.alpha = 1.0f;
                    }
                    animating = true;
                    break;
                case HOLD:
                    if (elapsed >= toast.durationMs) {
                        toast.phase = ToastPhase.FADE_OUT;
                        toast.phaseStartTimeMs = now;
                        animating = true;
                    }
                    break;
                case FADE_OUT:
                    toast.alpha = Math.max(0.0f, 1.0f - elapsed / (float) FADE_OUT_MS);
                    if (elapsed >= FADE_OUT_MS) {
                        toast.phase = ToastPhase.DONE;
                        toast.alpha = 0;
                    }
                    animating = true;
                    break;
                default:
                    break;
            }
        }

        // Remove completed toasts
        activeToasts.removeIf(t -> t.phase == ToastPhase.DONE);

        // Recalculate target Y positions
        recalculateTargetPositions();

        // Smooth-interpolate currentY toward targetY
        for (ToastMessage toast : activeToasts) {
            float diff = toast.targetY - toast.currentY;
            if (Math.abs(diff) < 0.5f) {
                toast.currentY = toast.targetY;
            } else {
                toast.currentY += diff * SLIDE_SPEED;
                animating = true;
            }
        }

        return animating;
    }

    @Override
    public void draw(Graphics graph, Rectangle clipBounds) {
        drainPending();

        if (activeToasts.isEmpty()) {
            return;
        }

        Graphics2D g2d = (Graphics2D) graph;
        UIUtil.setHighQualityRendering(g2d);

        int iconW = UIUtil.scaleForGUI(ICON_WIDTH);
        int iconH = UIUtil.scaleForGUI(ICON_HEIGHT);
        int gap = UIUtil.scaleForGUI(ICON_TEXT_GAP);
        int padX = UIUtil.scaleForGUI(PADDING_X);
        int padY = UIUtil.scaleForGUI(PADDING_Y);
        int cornerR = UIUtil.scaleForGUI(CORNER_RADIUS);
        float fontSize = UIUtil.scaleForGUI((float) FONT.getSize());
        Font scaledFont = FONT.deriveFont(fontSize);
        FontMetrics fm = g2d.getFontMetrics(scaledFont);

        for (ToastMessage toast : activeToasts) {
            if (toast.alpha <= 0) {
                continue;
            }

            // Look up entity icon if available
            Image icon = null;
            if (toast.entityId >= 0) {
                Entity entity = clientGui.getClient().getGame().getEntity(toast.entityId);
                if (entity != null) {
                    icon = boardView.getTilesetManager().iconFor(entity);
                }
            }

            // Calculate dimensions
            int textW = fm.stringWidth(toast.text);
            int textH = fm.getHeight();
            boolean hasIcon = (icon != null);
            int contentH = hasIcon ? Math.max(iconH, textH) : textH;
            int toastH = contentH + 2 * padY;
            int toastW = textW + 2 * padX;
            if (hasIcon) {
                toastW += iconW + gap;
            }

            // Clamp toast width to viewport
            int maxToastW = clipBounds.width - 2 * padX;
            toastW = Math.min(toastW, maxToastW);

            // Center horizontally in viewport
            int toastX = clipBounds.x + (clipBounds.width - toastW) / 2;
            int toastY = clipBounds.y + (int) toast.currentY;

            // Apply per-toast alpha
            Composite savedComposite = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, toast.alpha));

            // Draw background
            Color bg = toast.level.getBackgroundColor();
            g2d.setColor(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), BG_ALPHA));
            g2d.fillRoundRect(toastX, toastY, toastW, toastH, cornerR, cornerR);

            // Draw icon if present
            int contentX = toastX + padX;
            int contentCenterY = toastY + toastH / 2;
            if (hasIcon) {
                int iconY = contentCenterY - iconH / 2;
                g2d.drawImage(icon, contentX, iconY, iconW, iconH, null);
                contentX += iconW + gap;
            }

            // Draw text
            new StringDrawer(toast.text).at(contentX, contentCenterY).centerY()
                  .font(scaledFont).color(Color.WHITE).outline(Color.BLACK, 1.0f)
                  .draw(g2d);

            g2d.setComposite(savedComposite);
        }
    }

    private void drainPending() {
        ToastMessage pending;
        while ((pending = pendingToasts.poll()) != null) {
            // Remove completed toasts first to free capacity
            activeToasts.removeIf(t -> t.phase == ToastPhase.DONE);
            // If still at capacity, force the oldest to fade out
            if (activeToasts.size() >= MAX_VISIBLE) {
                forceExpireOldest();
                activeToasts.removeIf(t -> t.phase == ToastPhase.DONE);
            }
            // Drop the toast if we still can't fit it
            if (activeToasts.size() >= MAX_VISIBLE) {
                continue;
            }
            activeToasts.add(pending);
        }
        recalculateTargetPositions();
    }

    private void forceExpireOldest() {
        for (ToastMessage toast : activeToasts) {
            if (toast.phase == ToastPhase.HOLD || toast.phase == ToastPhase.FADE_IN) {
                toast.phase = ToastPhase.FADE_OUT;
                toast.phaseStartTimeMs = System.currentTimeMillis();
                return;
            }
        }
    }

    private void recalculateTargetPositions() {
        float y = UIUtil.scaleForGUI(DIST_TOP);
        int toastGap = UIUtil.scaleForGUI(TOAST_GAP);
        int padY = UIUtil.scaleForGUI(PADDING_Y);
        int iconH = UIUtil.scaleForGUI(ICON_HEIGHT);

        for (ToastMessage toast : activeToasts) {
            toast.targetY = y;
            // Estimate toast height for stacking (icon height or single line of text)
            int contentH = (toast.entityId >= 0) ? iconH : UIUtil.scaleForGUI(FONT.getSize() + 4);
            int toastH = contentH + 2 * padY;
            y += toastH + toastGap;
        }

        // Initialize currentY for newly added toasts that haven't been positioned yet
        for (ToastMessage toast : activeToasts) {
            if (toast.currentY == 0 && toast.targetY > 0) {
                toast.currentY = toast.targetY;
            }
        }
    }
}
