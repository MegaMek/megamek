/*
 * Copyright (C) 2023-2026 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;

import megamek.MMConstants;
import megamek.client.ui.dialogs.buttonDialogs.CommonSettingsDialog;
import megamek.common.preference.PreferenceManager;
import megamek.logging.MMLogger;

/**
 * This singleton class reads in the available fonts when first called, including those in
 * {@link MMConstants#FONT_DIRECTORY}. Lists of available fonts can be obtained by calling
 * {@link #getAvailableNonSymbolFonts()} and {@link #getAvailableFonts()}.
 */
public final class FontHandler {

    private static final MMLogger logger = MMLogger.create(FontHandler.class);

    private static final FontHandler instance = new FontHandler();
    private static final String SYMBOL_TEST_STRING = "abcdefgnzABCDEFGNZ1234567890/()[]";

    private final List<String> nonSymbolFontNames = new ArrayList<>();
    private final List<String> allFontNames = new ArrayList<>();
    volatile boolean initialized = false;

    /**
     * Returns a list of available font names excluding some symbol fonts (specifically, excluding fonts that cannot
     * display any of the characters "abcdefgnzABCDEFGNZ1234567890/()[]"). This list is only read from the
     * GraphicsEnvironment once and then not updated while the application is running.
     */
    public static List<String> getAvailableNonSymbolFonts() {
        ensureInitialization();
        return instance.nonSymbolFontNames;
    }

    /**
     * Returns a list of available font names. This list is only read from the GraphicsEnvironment once and then not
     * updated while the application is running.
     */
    public static List<String> getAvailableFonts() {
        ensureInitialization();
        return instance.allFontNames;
    }

    /**
     * Initializes the FontHandler, reading in and storing the available fonts for retrieval. Also reads in any fonts in
     * {@link MMConstants#FONT_DIRECTORY}.
     */
    public static void initialize() {
        synchronized (instance) {
            if (!instance.initialized) {
                instance.initializeFonts();
            }
        }
    }

    /**
     * @return A standardized symbols font ("Material Symbols"). This font has a load of useful icons. They are centered
     *       in a way that makes them less aesthetic to use within normal text, so their primary use is for standalone
     *       symbols like marking hexes. Look for the symbol Unicode's on the linked website.
     *
     * @see <a href="https://fonts.google.com/icons">(Google) Material Symbols</a>
     */
    public static Font symbolFont() {
        ensureInitialization();
        return new Font("Material Symbols Rounded", Font.PLAIN, 12);
    }

    /**
     * Creates an {@link Icon} that paints a single Google Material Symbol glyph from {@link #symbolFont()}, sized and
     * tinted as requested. The glyph is drawn live on each repaint so it stays crisp on HiDPI displays, and is sized to
     * the glyph's ink box so it carries no uneven side bearing. Code points are listed at
     * <a href="https://fonts.google.com/icons">fonts.google.com/icons</a>.
     *
     * @param codePoint the Material Symbols code point, for example {@code 0xE5D7} for {@code unfold_more}
     * @param size      the glyph height in (already scaled) pixels
     * @param color     the color to paint the glyph
     *
     * @return an {@link Icon} drawing the glyph, or {@code null} on invalid input (out-of-range code point, non-positive
     *       size, or null color) or if the symbols font cannot display the code point
     */
    public static Icon symbolIcon(int codePoint, int size, Color color) {
        if (!Character.isValidCodePoint(codePoint) || size <= 0 || color == null) {
            return null;
        }
        Font font = symbolFont().deriveFont((float) size);
        if (!font.canDisplay(codePoint)) {
            return null;
        }
        return new MaterialSymbolIcon(font, new String(Character.toChars(codePoint)), color);
    }

    /**
     * An {@link Icon} that paints one glyph live through the host component's graphics, sized to the glyph's ink box.
     */
    private static final class MaterialSymbolIcon implements Icon {
        private final Font font;
        private final String glyph;
        private final Color color;
        private final int width;
        private final int height;
        private final float drawX;
        private final float drawY;

        private MaterialSymbolIcon(Font font, String glyph, Color color) {
            this.font = font;
            this.glyph = glyph;
            this.color = color;

            // Swing needs a fixed icon size up front, so measure the glyph once and cache it. The 1x1 image only
            // exists to obtain a real Graphics2D, and therefore accurate font metrics; nothing is ever drawn into it.
            BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = scratch.createGraphics();
            FontMetrics metrics = graphics.getFontMetrics(font);
            // Height uses the full line metrics (ascent + descent + leading) so every icon shares one vertical box;
            // glyphs then line up on a common baseline instead of jumping around as the height varies per symbol.
            height = Math.max(1, metrics.getHeight());
            drawY = metrics.getAscent();
            // Width uses the visual (ink) bounds instead, because Material Symbols bake uneven left/right side bearing
            // into each glyph; trimming to the painted pixels lets a single icon center cleanly inside a button.
            // Measure with fractional metrics and keep the offsets as floats, so paintIcon can position the glyph
            // sub-pixel instead of rounding the bearing to a whole pixel (which would clip an edge or add a 1px gap).
            FontRenderContext renderContext = new FontRenderContext(null, true, true);
            Rectangle2D inkBounds = font.createGlyphVector(renderContext, glyph).getVisualBounds();
            width = Math.max(1, (int) Math.ceil(inkBounds.getWidth()));
            // getX() is the left bearing (offset from the pen origin to the first visible pixel); cancel it so the
            // glyph paints flush at x = 0 with no leading gap.
            drawX = (float) -inkBounds.getX();
            graphics.dispose();
        }

        @Override
        public void paintIcon(Component component, Graphics g, int x, int y) {
            Graphics2D graphics = (Graphics2D) g.create();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                // Match the fractional metrics used when the ink bounds were measured, so the glyph lands exactly
                // where width/drawX expect; mismatched metrics could otherwise clip an edge or shift it a pixel.
                graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                graphics.setFont(font);
                graphics.setColor(color);
                graphics.drawString(glyph, x + drawX, y + drawY);
            } finally {
                graphics.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return width;
        }

        @Override
        public int getIconHeight() {
            return height;
        }
    }

    /**
     * @return The Noto Symbols 2 font. This font has icons that mesh well in inline text with the Noto Sans font.
     */
    @Deprecated(since = "0.51.0", forRemoval = true)
    public static Font notoSymbol2Font() {
        ensureInitialization();
        return new Font("Noto Sans Symbols 2", Font.PLAIN, 12);
    }

    /**
     * @return The Noto Sans font which is included with the distribution and can be safely used everywhere. It is
     *       advertised to have a wide language support.
     *
     * @see <a href="https://fonts.google.com/icons">(Google) Material Symbols</a>
     */
    public static Font notoFont() {
        ensureInitialization();
        return new Font("Noto Sans", Font.PLAIN, 14);
    }

    private void initializeFonts() {
        logger.info("Loading fonts from " + MMConstants.FONT_DIRECTORY);
        parseFontsInDirectory(MMConstants.FONT_DIRECTORY);

        String userDir = PreferenceManager.getClientPreferences().getUserDir();
        if (!userDir.isBlank()) {
            logger.info("Loading fonts from {}", userDir);
            parseFontsInDirectory(userDir);
        }

        logger.info("Loading fonts from Java's GraphicsEnvironment");
        for (String fontName : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            // Skip Aakar specifically; it causes graphical artefacts when present and selected as the default font.
            if (!fontName.toLowerCase().contains("aakar")) {
                allFontNames.add(fontName);
                Font font = Font.decode(fontName);
                if (font.canDisplayUpTo(SYMBOL_TEST_STRING) == -1) {
                    nonSymbolFontNames.add(fontName);
                }
            }
        }
        initialized = true;
    }

    /**
     * Searches the provided directory and all subdirectories and registers any truetype fonts from .ttf files it
     * finds.
     *
     * @param directory the directory to parse
     */
    public static void parseFontsInDirectory(String directory) {
        parseFontsInDirectory(new File(directory));
    }

    /**
     * Searches the provided directory and all subdirectories and registers any truetype fonts from .ttf files it
     * finds.
     *
     * @param directory the directory to parse
     */
    public static void parseFontsInDirectory(final File directory) {
        List<String> errors = new ArrayList<>();
        for (String fontFile : CommonSettingsDialog.filteredFilesWithSubDirs(directory, MMConstants.TRUETYPE_FONT)) {
            try (InputStream fis = new FileInputStream(fontFile)) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, fis);
                if (!GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font)) {
                    errors.add("    Failed to register font " + fontFile);
                }
            } catch (Exception ex) {
                errors.add("    Failed to read font " + fontFile);
            }
        }
        logger.debug("Could not register some fonts\n{}", String.join("\n", errors));
    }

    private static void ensureInitialization() {
        if (!instance.initialized) {
            initialize();
        }
    }
}
