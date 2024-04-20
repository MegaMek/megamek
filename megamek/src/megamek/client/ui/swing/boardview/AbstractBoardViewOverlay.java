/*
 * MegaMek - Copyright (C) 2023 - The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.client.ui.swing.boardview;

import megamek.client.ui.IDisplayable;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.StringDrawer;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Game;
import megamek.common.KeyBindParser;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameListener;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.util.ImageUtil;
import org.apache.logging.log4j.LogManager;

import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This is a framework for boardview overlays that can show a list of data, conditions etc.
 */
public abstract class AbstractBoardViewOverlay implements IDisplayable, IPreferenceChangeListener {
    private static final int PADDING_X = 10;
    private static final int PADDING_Y = 5;
    private static final float FADE_SPEED = 0.2f;
    /** The ClientGUI of the boardview. May be null! */
    protected final ClientGUI clientGui;
    protected static final GUIPreferences GUIP = GUIPreferences.getInstance();
    protected final BoardView boardView;

    /** True when the overlay is displayed or fading in. */
    private boolean visible;
    /** True indicates the strings should be redrawn. */
    private boolean dirty = true;
    private boolean hasContents = false;
    /** The cached image for this Display. */
    private Image displayImage;
    /** The current game phase. */
    protected GamePhase currentPhase;
    protected final Game currentGame;

    protected final Font font;

    /** True while fading in this overlay. */
    private boolean fadingIn = false;
    /** True while fading out this overlay. */
    private boolean fadingOut = false;
    /** The transparency of the overlay. Only used while fading in/out. */
    private float alpha = 1;
    private int overlayWidth = 500;
    private int overlayHeight = 500;

    public AbstractBoardViewOverlay(BoardView boardView, Font font) {
        this.font = font;
        visible =  getVisibilityGUIPreference();
        this.boardView = Objects.requireNonNull(boardView);
        clientGui = boardView.clientgui;
        currentGame = Objects.requireNonNull(boardView.game);
        currentPhase = currentGame.getPhase();
        currentGame.addGameListener(gameListener);
        GUIPreferences.getInstance().addPreferenceChangeListener(this);
        KeyBindParser.addPreferenceChangeListener(this);
    }

    protected void addHeader(List<String> lines) {
        if (GUIP.getPlanetaryConditionsShowHeader()) {
            Color colorTitle = GUIP.getPlanetaryConditionsColorTitle();
            lines.add(colorToHex(colorTitle) + getHeaderText());
        }
    }

    /** Override to return the localized header text, possibly including the current keyboard shortcut to toggle it. */
    protected abstract String getHeaderText();

    @Override
    public void draw(Graphics graph, Rectangle clipBounds) {
        if (!visible && !isSliding()) {
            return;
        }

        // At startup, phase and turn change and when the Planetary Conditions change,
        // the cached image is (re)created
        if (dirty) {
            dirty = false;

            List<String> allLines = assembleTextLines();
            hasContents = !allLines.isEmpty();
            if (hasContents) {
                // calculate the size from the text lines, font and padding
                float fontSize = UIUtil.scaleForGUI(font.getSize());
                Font newFont = font.deriveFont(fontSize);
                FontMetrics fm = graph.getFontMetrics(newFont);
                Rectangle r = getSize(allLines, fm);
                r = new Rectangle(r.width + 2 * PADDING_X, r.height + 2 * PADDING_Y);
                overlayWidth = r.width;
                overlayHeight = r.height;

                displayImage = ImageUtil.createAcceleratedImage(r.width, r.height);
                Graphics intGraph = displayImage.getGraphics();
                UIUtil.setHighQualityRendering(intGraph);

                // draw a semi-transparent background rectangle
                Color colorBG = GUIP.getPlanetaryConditionsColorBackground();
                intGraph.setColor(new Color(colorBG.getRed(), colorBG.getGreen(), colorBG.getBlue(), GUIP.getPlanetaryConditionsBackgroundTransparency()));
                intGraph.fillRoundRect(0, 0, r.width, r.height, PADDING_X, PADDING_Y);

                // The coordinates to write the texts to
                int y = PADDING_Y + fm.getAscent();

                // write the strings
                for (String line : allLines) {
                    new StringDrawer(cleanedLine(line)).at(PADDING_X, y).font(font).fontSize(fontSize)
                            .color(lineColor(line)).draw(intGraph);
                    y += fm.getHeight();
                }
            }
        }

        if (hasContents) {
            // draw the cached image to the boardview
            // uses Composite to draw the image with variable transparency
            int distSide = getDistSide(clipBounds, overlayWidth);
            int distTop = getDistTop(clipBounds, overlayHeight);

            if (alpha < 1) {
                // Save the former composite and set an alpha blending composite
                Composite saveComp = ((Graphics2D) graph).getComposite();
                int type = AlphaComposite.SRC_OVER;
                ((Graphics2D) graph).setComposite(AlphaComposite.getInstance(type, alpha));
                graph.drawImage(displayImage, clipBounds.x + distSide, clipBounds.y + distTop, null);
                ((Graphics2D) graph).setComposite(saveComp);
            } else {
                graph.drawImage(displayImage, clipBounds.x + distSide, clipBounds.y + distTop, null);
            }
        }
    }

    /** Calculates the pixel size of the display from the necessary text lines. */
    private Rectangle getSize(List<String> lines, FontMetrics fm) {
        List<String> cleanedLines = lines.stream().map(this::cleanedLine).collect(Collectors.toList());
        int width = cleanedLines.stream().mapToInt(fm::stringWidth).max().orElse(0);
        int height = fm.getHeight() * lines.size();
        return new Rectangle(width, height);
    }

    /** Override to return a List of all text lines to be shown. Use {@link #addHeader(List)}. */
    protected abstract List<String> assembleTextLines();

    /** Returns the color encoded in the given line if there is one; the standard text color otherwise. */
    private Color lineColor(String line) {
        Color textColor = getTextColor();
        // Extract a color code from the start of the line
        if (line.startsWith("#") && line.length() > 7) {
            try {
                int red = Integer.parseInt(line.substring(1, 3), 16);
                int grn = Integer.parseInt(line.substring(3, 5), 16);
                int blu = Integer.parseInt(line.substring(5, 7), 16);
                textColor = new Color(red, grn, blu);
            } catch (Exception e) {
                LogManager.getLogger().error("", e);
            }
        }
        return textColor;
    }

    /** Returns the line but without the color code at the start if there was one. */
    private String cleanedLine(String line) {
        if (line.startsWith("#") && line.length() > 7) {
            return line.substring(7);
        } else {
            return line;
        }
    }

    /**
     * Activates or deactivates the overlay, fading it in or out. Also saves the visibility to the GUIPreferences so
     * MegaMek remembers it.
     */
    public void setVisible(boolean vis) {
        visible = vis;

        if (vis) {
            fadingIn = true;
            fadingOut = false;
            setDirty();
        } else {
            fadingIn = false;
            fadingOut = true;
        }
    }

    public boolean isVisible() {
        return visible;
    }

    @Override
    public boolean isSliding() {
        return fadingOut || fadingIn;
    }

    @Override
    public boolean slide() {
        if (fadingIn) {
            alpha += FADE_SPEED;
            if (alpha > 1) {
                alpha = 1;
                fadingIn = false;
            }
            return true;
        } else if (fadingOut) {
            alpha -= FADE_SPEED;
            if (alpha < 0) {
                alpha = 0;
                fadingOut = false;
            }
            return true;
        }
        return false;
    }

    /** Detects phase and turn changes to display */
    private final GameListener gameListener = new GameListenerAdapter() {
        @Override
        public void gamePhaseChange(GamePhaseChangeEvent e) {
            currentPhase = e.getNewPhase();
            gameTurnOrPhaseChange();
        }

        @Override
        public void gameTurnChange(GameTurnChangeEvent e) {
            // The active player has changed
            gameTurnOrPhaseChange();
        }
    };

    protected void setDirty() {
        dirty = true;
        scheduleBoardViewRepaint();
    }

    protected void gameTurnOrPhaseChange() {
        setDirty();
    }

    public static Color getTextColor() {
        return GUIP.getPlanetaryConditionsColorText();
    }

    protected abstract boolean getVisibilityGUIPreference();

    protected abstract int getDistTop(Rectangle clipBounds,  int overlayHeight);

    protected abstract int getDistSide(Rectangle clipBounds, int overlayWidth);

    public static String colorToHex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(),color.getBlue());
    }

    public static String colorToHex(Color color, float brightnessMultiplier) {
        return String.format("#%02X%02X%02X",
                (int)(color.getRed() * brightnessMultiplier), (int)(color.getGreen() * brightnessMultiplier),
                (int)(color.getBlue() * brightnessMultiplier));
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        switch (e.getName()) {
            case KeyBindParser.KEYBINDS_CHANGED:
            case GUIPreferences.GUI_SCALE:
                setDirty();
                break;
        }
    }

    /** Makes the BoardView redraw, updating the overlay in the process. */
    protected void scheduleBoardViewRepaint() {
        boardView.getPanel().repaint();
    }
}