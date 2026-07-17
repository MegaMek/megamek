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
package megamek.client.ui.clientGUI.boardview.sprite;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ImageIcon;

import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.tileset.HexTileset;
import megamek.client.ui.util.UIUtil;
import megamek.common.Configuration;
import megamek.common.Hex;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.preference.PreferenceManager;
import megamek.common.units.Terrains;
import megamek.common.util.ImageUtil;
import megamek.logging.MMLogger;

/**
 * Displays a rubble-clearing-in-progress indicator on the hex a bulldozer vehicle is clearing (TacOps). Shows the
 * clearing progress as text ("2/8") and fades in the matching "cleared paths" rubble artwork
 * (saxarba/rubble_&lt;type&gt;_path.png) at an opacity that grows with progress, so the rubble hex is progressively
 * replaced by the bulldozed-path version as the work nears completion. If the path image cannot be loaded it falls back
 * to fading the hex to its exposed base-terrain colour.
 */
public class RubbleClearSprite extends HexSprite {

    private static final MMLogger LOGGER = MMLogger.create(RubbleClearSprite.class);

    /** Fallback cleared-ground tone, used only when neither the path image nor the base terrain can be sampled. */
    private static final Color CLEARED_FALLBACK_COLOR = new Color(170, 166, 156);
    private static final Color TEXT_OUTLINE_COLOR = new Color(40, 40, 50);

    private static final int HEX_CENTER_X = HexTileset.HEX_W / 2;
    private static final int FONT_SIZE = 11;
    private static final int TOP_OFFSET = 13;

    /** Faintest reveal opacity, so the cleared-path artwork is visible from the first turn of work. */
    private static final float MIN_ALPHA = 0.15f;
    /** Strongest opacity of the base-terrain colour fallback fade. */
    private static final float MAX_FADE_ALPHA = 0.65f;

    // The six corners of the standard 84x72 hex, used for the base-terrain fallback fade.
    private static final int[] HEX_POLY_X = { 21, 63, HexTileset.HEX_W, 63, 21, 0 };
    private static final int[] HEX_POLY_Y = { 0, 0, 36, HexTileset.HEX_H, HexTileset.HEX_H, 36 };

    /** Cache of loaded cleared-path images by file path; a {@code null} value marks an image that failed to load. */
    private static final Map<String, Image> PATH_IMAGE_CACHE = new HashMap<>();

    private final int turnsCompleted;
    private final int turnsRequired;
    /**
     * When {@code true} this sprite draws only the "n/m" progress counter and layers OVER the unit; when {@code false} it draws only
     * the cleared-path fade and layers BEHIND the unit. The handler creates one of each so the fade does not dim the
     * vehicle while the counter still reads clearly on top of it (matching the fortify indicator). QA feedback.
     */
    private final boolean overlayCounter;

    /**
     * Creates a new rubble-clearing sprite for the given hex.
     *
     * @param boardView      the parent board view
     * @param loc            the rubble hex being cleared
     * @param turnsCompleted the turns of clearing banked so far (the numerator)
     * @param turnsRequired  the total turns the clearing needs (the denominator)
     * @param overlayCounter {@code true} to draw the progress counter over the unit; {@code false} to draw the cleared-path fade behind
     */
    public RubbleClearSprite(BoardView boardView, Coords loc, int turnsCompleted, int turnsRequired,
          boolean overlayCounter) {
        super(boardView, loc);
        this.turnsCompleted = turnsCompleted;
        this.turnsRequired = turnsRequired;
        this.overlayCounter = overlayCounter;
    }

    @Override
    public void prepare() {
        Graphics2D graph = spriteSetup();
        if (overlayCounter) {
            drawProgressIndicator(graph);
        } else {
            drawClearedReveal(graph);
        }
        graph.dispose();
    }

    private Graphics2D spriteSetup() {
        updateBounds();
        image = createNewHexImage();
        Graphics2D graph = (Graphics2D) image.getGraphics();
        UIUtil.setHighQualityRendering(graph);
        graph.scale(bv.getScale(), bv.getScale());
        return graph;
    }

    /**
     * Fades in the matching cleared-paths rubble artwork over the hex at an opacity that grows with clearing progress,
     * so the rubble is progressively replaced by the bulldozed-path version. Falls back to fading the hex toward its
     * exposed base-terrain colour when the path image is unavailable.
     *
     * @param graph the sprite graphics
     */
    private void drawClearedReveal(Graphics2D graph) {
        graph.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float progress = Math.clamp((float) displayTurn() / Math.max(1, turnsRequired), 0f, 1f);

        Composite oldComposite = graph.getComposite();
        Hex hex = bv.getBoard().getHex(loc);
        Image pathImage = clearedPathImage(hex);
        if (pathImage != null) {
            // The path artwork is only the rubble debris pushed into the wedges; its lanes are transparent. Composite
            // it over the exposed base terrain first so the lanes reveal cleared ground rather than the original
            // rubble (which the board view still draws beneath this sprite), then fade the whole composite in.
            BufferedImage composite = new BufferedImage(HexTileset.HEX_W,
                  HexTileset.HEX_H,
                  BufferedImage.TYPE_INT_ARGB);
            Graphics2D compositeGraphics = composite.createGraphics();
            UIUtil.setHighQualityRendering(compositeGraphics);
            Image baseImage = (hex == null) ? null : bv.getTilesetManager().baseFor(hex);
            if (baseImage != null) {
                compositeGraphics.drawImage(baseImage, 0, 0, HexTileset.HEX_W, HexTileset.HEX_H, null);
            }
            compositeGraphics.drawImage(pathImage, 0, 0, HexTileset.HEX_W, HexTileset.HEX_H, null);
            compositeGraphics.dispose();

            graph.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(MIN_ALPHA, progress)));
            graph.drawImage(composite, 0, 0, null);
        } else {
            graph.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, progress * MAX_FADE_ALPHA));
            graph.setColor(baseTerrainColor());
            graph.fillPolygon(HEX_POLY_X, HEX_POLY_Y, HEX_POLY_X.length);
        }
        graph.setComposite(oldComposite);
    }

    /**
     * @param hex the rubble hex (may be {@code null})
     *
     * @return the saxarba cleared-paths rubble image matching the hex's rubble level
     *       (light/medium/heavy/hardened/wall), or {@code null} if the active tileset is not saxarba (other tilesets fall back
     *       to the generic base-terrain fade), the hex has no rubble, or the image cannot be loaded
     */
    private @Nullable Image clearedPathImage(@Nullable Hex hex) {
        if (hex == null) {
            return null;
        }
        // The "_path" artwork lives in the saxarba tileset; on other tilesets fall back to the generic fade.
        String activeTileset = PreferenceManager.getClientPreferences().getMapTileset();
        if ((activeTileset == null) || !activeTileset.toLowerCase().contains("saxarba")) {
            return null;
        }
        int rubbleLevel = hex.terrainLevel(Terrains.RUBBLE);
        if (rubbleLevel <= 0) {
            return null;
        }
        return loadPathImage("saxarba/rubble_" + structureImageFileToken(rubbleLevel) + "_path.png");
    }

    /**
     * @param rubbleLevel the {@link Terrains#RUBBLE} level (1 light .. 4 hardened, 5 wall, 6+ ultra)
     *
     * @return the structure token used to build the cleared-path image file name (NOT a user-facing label); ultra and
     *       any other level reuse the heavy artwork, matching the saxarba tileset's rubble image mapping
     */
    private static String structureImageFileToken(int rubbleLevel) {
        return switch (rubbleLevel) {
            case 1 -> "light";
            case 2 -> "medium";
            case 4 -> "hardened";
            case 5 -> "wall";
            default -> "heavy";
        };
    }

    /**
     * Loads a cleared-path image from the hex image directory, caching the result (including failures) so each file is
     * read at most once.
     *
     * @param relativePath the image path relative to the hexes image directory
     *
     * @return the loaded image, or {@code null} if it could not be found/loaded
     */
    private static @Nullable Image loadPathImage(String relativePath) {
        if (!PATH_IMAGE_CACHE.containsKey(relativePath)) {
            File imageFile = new File(Configuration.hexesDir(), relativePath);
            if (imageFile.exists()) {
                // ImageIcon blocks until the image is fully decoded; a sprite paints its buffer only once, so a
                // still-loading image would silently draw nothing.
                PATH_IMAGE_CACHE.put(relativePath,
                      new ImageIcon(ImageUtil.loadImageFromFile(imageFile.toString())).getImage());
            } else {
                LOGGER.warn("[Bulldozer] cleared-path image not found: {}; falling back to a base-terrain fade",
                      imageFile);
                PATH_IMAGE_CACHE.put(relativePath, null);
            }
        }
        return PATH_IMAGE_CACHE.get(relativePath);
    }

    /**
     * Samples the average colour of the hex's base terrain tile - the ground beneath the rubble - for the fallback fade
     * used when the cleared-path image is unavailable.
     *
     * @return the average base-terrain colour, or {@link #CLEARED_FALLBACK_COLOR} if it cannot be sampled
     */
    private Color baseTerrainColor() {
        try {
            Hex hex = bv.getBoard().getHex(loc);
            if (hex == null) {
                return CLEARED_FALLBACK_COLOR;
            }
            Image baseImage = bv.getTilesetManager().baseFor(hex);
            int width = (baseImage == null) ? 0 : baseImage.getWidth(null);
            int height = (baseImage == null) ? 0 : baseImage.getHeight(null);
            if ((width <= 0) || (height <= 0)) {
                return CLEARED_FALLBACK_COLOR;
            }
            BufferedImage buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D bufferGraphics = buffer.createGraphics();
            bufferGraphics.drawImage(baseImage, 0, 0, null);
            bufferGraphics.dispose();

            long red = 0;
            long green = 0;
            long blue = 0;
            long samples = 0;
            for (int y = 0; y < height; y += 2) {
                for (int x = 0; x < width; x += 2) {
                    int argb = buffer.getRGB(x, y);
                    if (((argb >> 24) & 0xFF) < 128) {
                        continue;
                    }
                    red += (argb >> 16) & 0xFF;
                    green += (argb >> 8) & 0xFF;
                    blue += argb & 0xFF;
                    samples++;
                }
            }
            if (samples == 0) {
                return CLEARED_FALLBACK_COLOR;
            }
            return new Color((int) (red / samples), (int) (green / samples), (int) (blue / samples));
        } catch (RuntimeException runtimeException) {
            return CLEARED_FALLBACK_COLOR;
        }
    }

    /**
     * @return the turn of work currently under way (1-based, capped at the total). Clearing banks a turn at end phase,
     *       so it starts a turn at 0 completed; showing the current turn (1..n) keeps the indicator consistent with the
     *       fortify indicator, which starts at 1/3 rather than 0/3.
     */
    private int displayTurn() {
        return Math.min(turnsCompleted + 1, turnsRequired);
    }

    /**
     * Draws the clearing progress ("2/8") as text in the upper portion of the hex, clear of the unit icon.
     *
     * @param graph the sprite graphics
     */
    private void drawProgressIndicator(Graphics2D graph) {
        graph.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        String progress = displayTurn() + "/" + turnsRequired;
        Font progressFont = new Font("Sans Serif", Font.BOLD, FONT_SIZE);
        FontMetrics metrics = graph.getFontMetrics(progressFont);
        int textX = HEX_CENTER_X - (metrics.stringWidth(progress) / 2);
        int textY = TOP_OFFSET + metrics.getAscent();

        graph.setFont(progressFont);
        graph.setColor(TEXT_OUTLINE_COLOR);
        graph.drawString(progress, textX - 1, textY);
        graph.drawString(progress, textX + 1, textY);
        graph.drawString(progress, textX, textY - 1);
        graph.drawString(progress, textX, textY + 1);
        graph.setColor(Color.WHITE);
        graph.drawString(progress, textX, textY);
    }

    @Override
    public boolean isBehindTerrain() {
        // The fade layers BEHIND the unit (so it does not dim the vehicle, like DugInSprite); the progress counter
        // layers OVER the unit so it reads clearly (like the fortify indicator). QA feedback.
        return !overlayCounter;
    }
}
