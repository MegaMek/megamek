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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
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
    private static final long FADE_IN_NANOS = 300_000_000;
    private static final long FADE_OUT_NANOS = 1_000_000_000;
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
    private static final Font FONT = new Font(MMConstants.FONT_SANS_SERIF, Font.BOLD, 13);

    private record Artwork(int width, int height, Font font, Image icon, float guiScale,
          double scaleX, double scaleY) { }

    private static class ToastMessage {
        final String text;
        final ToastLevel level;
        final int entityId;
        final int durationMs;
        OverlayImage.Fade fade;
        OverlayImage.Transition position;
        Artwork artwork;
        BufferedImage image;

        ToastMessage(String text, ToastLevel level, int entityId, int durationMs) {
            this.text = text;
            this.level = level;
            this.entityId = entityId;
            this.durationMs = durationMs;
        }

        boolean expired(long now) {
            return fade != null && !fade.isAnimating(now) && fade.opacity(now) == 0;
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

    public void show(ToastLevel level, String text) {
        show(level, text, null, level.getDefaultDurationMs());
    }

    public void show(ToastLevel level, String text, @Nullable Entity entity) {
        show(level, text, entity, level.getDefaultDurationMs());
    }

    /** Enqueues a notification from any thread; Swing owns its artwork and animation schedule. */
    public void show(ToastLevel level, String text, @Nullable Entity entity, int durationMs) {
        pendingToasts.add(new ToastMessage(text, level, entity == null ? -1 : entity.getId(), durationMs));
        boardView.getPanel().repaint();
    }

    @Override
    public boolean isSliding() {
        return !pendingToasts.isEmpty() || !activeToasts.isEmpty();
    }

    @Override
    public boolean slide() {
        long now = System.nanoTime();
        boolean removed = activeToasts.removeIf(toast -> toast.expired(now));
        drainPending(now);
        return removed || activeToasts.stream().anyMatch(toast -> toast.fade == null
              || toast.fade.isAnimating(now) || toast.position.isAnimating(now));
    }

    @Override
    public void draw(Graphics graph, Rectangle clipBounds) {
        List<OverlayImage> layers = captureLayers((Graphics2D) graph, clipBounds);
        long now = System.nanoTime();
        for (OverlayImage layer : layers) {
            layer.draw((Graphics2D) graph, now);
        }
    }

    @Override
    public List<OverlayImage> captureLayers(Graphics2D graph, Rectangle clipBounds) {
        long now = System.nanoTime();
        activeToasts.removeIf(toast -> toast.expired(now));
        drainPending(now);
        var transform = graph.getTransform();
        double scaleX = Math.hypot(transform.getScaleX(), transform.getShearY());
        double scaleY = Math.hypot(transform.getShearX(), transform.getScaleY());
        int iconW = UIUtil.scaleForGUI(ICON_WIDTH);
        int iconH = UIUtil.scaleForGUI(ICON_HEIGHT);
        int gap = UIUtil.scaleForGUI(ICON_TEXT_GAP);
        int padX = UIUtil.scaleForGUI(PADDING_X);
        int padY = UIUtil.scaleForGUI(PADDING_Y);
        int cornerR = UIUtil.scaleForGUI(CORNER_RADIUS);
        Font font = FONT.deriveFont(UIUtil.scaleForGUI((float) FONT.getSize()));
        FontMetrics metrics = graph.getFontMetrics(font);
        List<OverlayImage> layers = new ArrayList<>();
        for (ToastMessage toast : activeToasts) {
            Image icon = null;
            if (toast.entityId >= 0) {
                Entity entity = clientGui.getClient().getGame().getEntity(toast.entityId);
                if (entity != null) {
                    icon = boardView.getTilesetManager().iconFor(entity);
                }
            }
            int contentHeight = icon == null ? metrics.getHeight() : Math.max(iconH, metrics.getHeight());
            int height = contentHeight + 2 * padY;
            int width = Math.max(1, Math.min(metrics.stringWidth(toast.text) + 2 * padX
                  + (icon == null ? 0 : iconW + gap), clipBounds.width - 2 * padX));
            Artwork artwork = new Artwork(width, height, font, icon, UIUtil.scaleForGUI(1f), scaleX, scaleY);
            if (!artwork.equals(toast.artwork)) {
                toast.artwork = artwork;
                toast.image = new BufferedImage(Math.max(1, (int) Math.ceil(width * scaleX)),
                      Math.max(1, (int) Math.ceil(height * scaleY)), BufferedImage.TYPE_INT_ARGB);
                Graphics2D painter = toast.image.createGraphics();
                try {
                    painter.scale(scaleX, scaleY);
                    UIUtil.setHighQualityRendering(painter);
                    Color background = toast.level.getBackgroundColor();
                    painter.setColor(new Color(background.getRed(), background.getGreen(), background.getBlue(), BG_ALPHA));
                    painter.fillRoundRect(0, 0, width, height, cornerR, cornerR);
                    int contentX = padX;
                    if (icon != null) {
                        painter.drawImage(icon, contentX, (height - iconH) / 2, iconW, iconH, null);
                        contentX += iconW + gap;
                    }
                    new StringDrawer(toast.text).at(contentX, height / 2).centerY()
                          .font(font).color(Color.WHITE).outline(Color.BLACK, 1.0f).draw(painter);
                } finally {
                    painter.dispose();
                }
            }
            if (toast.fade == null) {
                // Start when artwork can first be presented, not while a notification waits in a busy EDT queue.
                toast.fade = new OverlayImage.Fade(new OverlayImage.Transition(now, 0, 1, FADE_IN_NANOS),
                      new OverlayImage.Transition(now + FADE_IN_NANOS + toast.durationMs * 1_000_000L,
                            1, 0, FADE_OUT_NANOS));
            }
            int x = (int) Math.round((clipBounds.x + (clipBounds.width - width) / 2.0) * scaleX
                  + transform.getTranslateX());
            int y = (int) Math.round(clipBounds.y * scaleY + transform.getTranslateY());
            layers.add(new OverlayImage(toast.image, x, y, toast.fade, toast.position.scaled((float) scaleY)));
        }
        return List.copyOf(layers);
    }

    private void drainPending(long now) {
        ToastMessage pending;
        while ((pending = pendingToasts.poll()) != null) {
            activeToasts.removeIf(toast -> toast.expired(now));
            if (activeToasts.size() >= MAX_VISIBLE) {
                forceExpireOldest(now);
                activeToasts.removeIf(toast -> toast.expired(now));
            }
            if (activeToasts.size() < MAX_VISIBLE) {
                activeToasts.add(pending);
            }
        }
        recalculateTargetPositions(now);
    }

    private void forceExpireOldest(long now) {
        for (ToastMessage toast : activeToasts) {
            if (toast.fade == null || now < toast.fade.expiry().startedNanos()) {
                float opacity = toast.fade == null ? 0 : toast.fade.opacity(now);
                toast.fade = new OverlayImage.Fade(new OverlayImage.Transition(now, opacity, 0, FADE_OUT_NANOS),
                      OverlayImage.Transition.ONE);
                return;
            }
        }
    }

    private void recalculateTargetPositions(long now) {
        float y = UIUtil.scaleForGUI(DIST_TOP);
        int toastGap = UIUtil.scaleForGUI(TOAST_GAP);
        int padY = UIUtil.scaleForGUI(PADDING_Y);
        int iconH = UIUtil.scaleForGUI(ICON_HEIGHT);
        for (ToastMessage toast : activeToasts) {
            if (toast.position == null) {
                toast.position = new OverlayImage.Transition(now, y, y, 0);
            } else if (toast.position.to() != y) {
                toast.position = new OverlayImage.Transition(now, toast.position.value(now), y, 200_000_000);
            }
            int contentHeight = toast.entityId >= 0 ? iconH : UIUtil.scaleForGUI(FONT.getSize() + 4);
            y += contentHeight + 2 * padY + toastGap;
        }
    }
}
