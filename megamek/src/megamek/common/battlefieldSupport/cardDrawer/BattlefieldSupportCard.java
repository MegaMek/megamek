/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.battlefieldSupport.cardDrawer;

import static java.awt.Color.WHITE;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Locale;
import javax.swing.ImageIcon;

import megamek.MMConstants;
import megamek.client.ui.tileset.MMStaticDirectoryManager;
import megamek.client.ui.tileset.MekTileset;
import megamek.client.ui.util.FluffImageHelper;
import megamek.client.ui.util.StringDrawer;
import megamek.client.ui.util.UIUtil;
import megamek.common.Configuration;
import megamek.common.annotations.Nullable;
import megamek.common.battlefieldSupport.BFSDamage;
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.logging.MMLogger;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.dom.util.SAXDocumentFactory;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Document;

/**
 * Renders a printable Battlefield Support (BFS) Asset card, mirroring the physical asset cards from the
 * <i>BattleTech: Mercenaries</i> box. The card is drawn in grayscale on a fixed {@value #WIDTH} x {@value #HEIGHT}
 * canvas so it prints cleanly in black and white, matching the clean line-art style of the retail cards (a stylized
 * cost badge, a double-struck chamfered border with the branding interrupting it, and stats floating on white).
 *
 * <p>The entire layout is drawn by a single {@link #drawCard(Graphics)} routine onto a generic {@link Graphics2D},
 * allowing the same code to render raster previews and vector print output.</p>
 *
 * <p>Fluff art is optional and <b>frequently absent</b> for assets; the card always renders cleanly with no art.</p>
 */
public class BattlefieldSupportCard {

    private static final MMLogger logger = MMLogger.create(BattlefieldSupportCard.class);

    public static final int WIDTH = 1050;
    public static final int HEIGHT = 750;
    /** Printed at 3.5" wide (1/72 dots per inch), matching the Alpha Strike card scale. */
    public static final double PRINT_SCALE = 3.5 * 72 / WIDTH;

    private static final String EM_DASH = "\u2014";

    // Border geometry. The bottom edge is broken by a gap spanning the BATTLETECH logo and the BATTLEFIELD SUPPORT
    // branding; it resumes to the right of the branding, runs along the bottom, then rises into a shelf that frames
    // the copyright at the bottom-right. Every bend is a 45-degree chamfer.
    private static final int BORDER_SHELF = 30;
    private static final int FOOTER_GAP_LEFT = 64;
    private static final int FOOTER_GAP_RIGHT = 560;
    private static final int SHELF_KNEE_X = 640;
    /** The outer border line's inset and corner chamfer; the shelf knee offset is derived relative to these. */
    private static final int OUTER_BORDER_INSET = 12;
    private static final int OUTER_BORDER_CHAMFER = 50;

    private static final String FILENAME_BT_LOGO = "BT_Logo_BW.png";
    private static final Image BT_LOGO = ImageUtil.loadImageFromFile(
          new MegaMekFile(Configuration.miscImagesDir(), FILENAME_BT_LOGO).toString());
    private static ImageIcon scaledBtLogo;

    /** Vector BATTLETECH logos extracted from the record-sheet templates: an all-black B&W one and one whose stylized
     * "A" is the record-sheet gold. Rendered via Batik so the logo is vector (real {@code <path>}s) in the print SVG. */
    private static final String FILENAME_BT_LOGO_BW_SVG = "bt_logo_bw.svg";
    private static final String FILENAME_BT_LOGO_COLOR_SVG = "bt_logo_color.svg";
    private static GraphicsNode btLogoBwNode;
    private static GraphicsNode btLogoColorNode;
    private static boolean btLogoBwLoaded;
    private static boolean btLogoColorLoaded;

    /**
     * Card color modes, mirroring the record-sheet {@code ColorMode}. In {@link #NONE} the whole card (including the
     * logo) is black and white; {@link #LOGO_ONLY} colors just the BATTLETECH logo's "A"; {@link #ALL} is full color
     * (currently the same as LOGO_ONLY - full-color styling of the rest of the card is still to be explored).
     */
    public enum ColorMode {
        NONE, LOGO_ONLY, ALL
    }

    /** Dark gray used to de-emphasize the not-selected value (and its parentheses) in a Regular/Veteran pair. */
    private static final Color INACTIVE_GRAY = new Color(105, 105, 105);

    /** The default color for the current (damaged) Destroy Check in a color mode; matches the record sheets' red. */
    private static final Color DEFAULT_DAMAGE_COLOR = new Color(0xEE, 0x00, 0x00);

    /** Record-sheet logo gold, used as the accent color for the cost badge, border and labels in full-color mode. */
    private static final Color ACCENT_GOLD = new Color(0xE0, 0xAD, 0x2A);

    private final BattlefieldSupportAsset asset;
    private final Image fluffImage;
    private ColorMode colorMode = ColorMode.NONE;
    private Color damageColor = DEFAULT_DAMAGE_COLOR;

    private Font baseFont = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 14);

    private Font titleFont;
    private Font subtitleFont;
    private Font labelFont;
    private Font valueFont;
    private Font movementFont;
    private Font specialsFont;
    private Font specialsHeaderFont;
    private Font costFont;
    private Font thresholdFont;
    private Font brandFont;
    private Font copyrightFont;

    /**
     * Creates a card for the given asset. The asset may be {@code null}, in which case a placeholder card of the
     * correct size is produced.
     *
     * @param asset the Battlefield Support Asset to render, or {@code null}
     */
    public BattlefieldSupportCard(@Nullable BattlefieldSupportAsset asset) {
        this.asset = asset;
        this.fluffImage = (asset != null) ? resolveArt(asset) : null;
    }

    /** Sets the card color mode (default {@link ColorMode#NONE}); controls whether the BATTLETECH logo is colored. */
    public void setColorMode(ColorMode colorMode) {
        this.colorMode = (colorMode != null) ? colorMode : ColorMode.NONE;
    }

    /**
     * Sets the color used for the current Destroy Check when the asset is damaged and the card is drawn in a color mode
     * (in {@link ColorMode#NONE} the current value stays black). Defaults to the record sheets' red; MegaMekLab passes
     * its configured damage color here.
     *
     * @param damageColor the damage color, or {@code null} to restore the default red
     */
    public void setDamageColor(@Nullable Color damageColor) {
        this.damageColor = (damageColor != null) ? damageColor : DEFAULT_DAMAGE_COLOR;
    }

    /** @return the accent color for the cost badge, border and labels: gold in full-color ({@link ColorMode#ALL})
     * mode, otherwise black. LOGO_ONLY colors only the logo, so it keeps the black accents. */
    private Color accentColor() {
        return (colorMode == ColorMode.ALL) ? ACCENT_GOLD : Color.BLACK;
    }

    /**
     * Resolves the image shown in the card's art area. Prefers a fluff image (embedded or by name lookup); when none
     * is available - which is common for assets - falls back to the unit's top-down sprite. Sprite resolution first
     * tries the sprite that would resolve for the asset's linked base unit (by shared chassis/model) and otherwise
     * yields a generic sprite for the unit type (tank, VTOL, emplacement, and so on). Returns {@code null} if nothing
     * can be resolved, in which case the art area is left blank.
     */
    private static @Nullable Image resolveArt(BattlefieldSupportAsset asset) {
        Image fluff = FluffImageHelper.getFluffImage(asset);
        if (fluff != null) {
            return fluff;
        }
        MekTileset tileset = MMStaticDirectoryManager.getMekTileset();
        return (tileset != null) ? tileset.imageFor(asset) : null;
    }

    /** Sets the base font used to derive all card fonts (e.g. for a preview font chooser). */
    public void setFont(Font newFont) {
        this.baseFont = newFont;
    }

    /**
     * @param scale scaling factor applied to the standard card size (1.0 = {@value #WIDTH} px wide)
     *
     * @return an image of the card scaled by the given factor
     */
    public BufferedImage getCardImage(float scale) {
        return getCardImage((int) (WIDTH * scale));
    }

    /**
     * @param width the desired card width in pixels; the height is derived to keep the aspect ratio
     *
     * @return an image of the card fitted to the given width
     */
    public BufferedImage getCardImage(int width) {
        int height = HEIGHT * width / WIDTH;
        BufferedImage result = ImageUtil.createAcceleratedImage(width, height);
        Graphics2D g2D = (Graphics2D) result.getGraphics();
        g2D.scale((double) width / WIDTH, (double) width / WIDTH);
        drawCard(g2D);
        g2D.dispose();
        return result;
    }

    /**
     * Draws the whole card onto the given graphics context at the standard {@value #WIDTH} x {@value #HEIGHT}
     * coordinate space. This is the single source of truth for the layout, used for both raster preview and the
     * vector print pipeline.
     *
     * @param g the graphics context to draw onto
     */
    public void drawCard(Graphics g) {
        initializeFonts();
        Graphics2D g2D = (Graphics2D) g;
        UIUtil.setHighQualityRendering(g);

        paintBorder(g2D);

        if (asset == null) {
            text("No asset or unit type not supported").at(70, 110).font(titleFont).draw(g);
            return;
        }

        drawFluffImage(g2D);
        paintCost(g2D);
        paintTitle(g2D);
        paintMovement(g2D);
        paintStats(g2D);
        paintSpecials(g2D);
        paintFooter(g2D);
    }

    private void initializeFonts() {
        Font plain = baseFont.deriveFont(Font.PLAIN);
        Font bold = baseFont.deriveFont(Font.BOLD);

        titleFont = bold.deriveFont(84f);
        subtitleFont = plain.deriveFont(40f);
        labelFont = bold.deriveFont(42f);
        valueFont = bold.deriveFont(62f);
        movementFont = bold.deriveFont(58f);
        specialsFont = plain.deriveFont(30f);
        specialsHeaderFont = bold.deriveFont(30f);
        costFont = bold.deriveFont(40f);
        thresholdFont = bold.deriveFont(24f);
        brandFont = bold.deriveFont(26f);
        copyrightFont = plain.deriveFont(13f);
    }

    /** Fills the card white and draws the double-struck, 45-degree chamfered border with its footer shelf. */
    private void paintBorder(Graphics2D g) {
        g.setColor(WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(accentColor());
        drawBorderLine(g, OUTER_BORDER_INSET, OUTER_BORDER_CHAMFER, 5f);
        drawBorderLine(g, 25, 40, 2.6f);
    }

    /**
     * Draws one line of the border as an open polyline (open at the logo gap) inset from the card edges, with all
     * corners chamfered at 45 degrees and the right portion of the bottom edge raised into a shelf around the
     * copyright.
     */
    private void drawBorderLine(Graphics2D g, int inset, int chamfer, float stroke) {
        int l = inset;
        int t = inset;
        int r = WIDTH - inset;
        int b = HEIGHT - inset;
        int c = chamfer;
        int sh = BORDER_SHELF;

        // The shelf's 45-degree rise must be inset like the corner chamfers so the two border lines keep an even gap
        // through the diagonal. A tighter (more-inset) line's corner chamfer shrinks; the part of the inset growth not
        // absorbed by that shrink is horizontal drift the shelf must also apply, or the inner knee sits too far right
        // and pinches the gap. Shift the knee left by that leftover.
        int shelfKneeX = SHELF_KNEE_X - ((inset - OUTER_BORDER_INSET) - (OUTER_BORDER_CHAMFER - chamfer));

        // The right-edge corners (top-right and the raised bottom-right shelf) are square 90-degree corners; the
        // left-edge corners and the shelf bump are 45-degree chamfers.
        int[] xs = { FOOTER_GAP_RIGHT, shelfKneeX, shelfKneeX + sh, r, r, l + c, l, l, l + c, FOOTER_GAP_LEFT };
        int[] ys = { b, b, b - sh, b - sh, t, t, t + c, b - c, b, b };
        g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        g.drawPolyline(xs, ys, xs.length);
    }

    /** Creates a StringDrawer that emits real text (an SVG {@code <text>} element when drawn to an SVG canvas). */
    private static StringDrawer text(String value) {
        return new StringDrawer(value).asText();
    }

    private void drawFluffImage(Graphics2D g) {
        if (fluffImage == null) {
            return;
        }
        int boxCenterX = 275;
        int boxCenterY = 300;
        int boxWidth = 470;
        int boxHeight = 320;
        ImageIcon icon = new ImageIcon(ImageUtil.fitImage(fluffImage, boxWidth, boxHeight,
              ImageUtil.IMAGE_SCALE_BICUBIC));
        int posX = boxCenterX - icon.getIconWidth() / 2;
        int posY = boxCenterY - icon.getIconHeight() / 2;
        g.drawImage(icon.getImage(), posX, posY, null);
    }

    /**
     * Draws the stylized "C" cost badge - a squared "C" whose left corners are chamfered at 45 degrees so it nests
     * against the card border. Each arm is split by a vertical gap that lines up with the open mouth to form a
     * continuous white stripe down the centre. The cost value (BSP), standard(veteran), sits in its mouth.
     */
    private void paintCost(Graphics2D g) {
        int x0 = 46;
        int y0 = 42;
        int thickness = 20;
        int height = 88;
        int chamfer = 16;
        int armLength = 70;
        int gapStart = 32;
        int gapWidth = 11;

        // The "C" badge is filled with the accent color (gold in full-color mode) while the cost number stays black.
        g.setColor(accentColor());

        // Left "[" part: spine plus the inner arm stubs, with 45-degree chamfered left corners.
        int[] xs = { x0 + chamfer, x0 + gapStart, x0 + gapStart, x0 + thickness, x0 + thickness, x0 + gapStart,
                     x0 + gapStart, x0 + chamfer, x0, x0 };
        int[] ys = { y0, y0, y0 + thickness, y0 + thickness, y0 + height - thickness, y0 + height - thickness,
                     y0 + height, y0 + height, y0 + height - chamfer, y0 + chamfer };
        g.fillPolygon(xs, ys, xs.length);

        // Outer arm tips, separated from the inner part by the vertical centre gap.
        int tipX = x0 + gapStart + gapWidth;
        int tipWidth = armLength - gapStart - gapWidth;
        g.fillRect(tipX, y0, tipWidth, thickness);
        g.fillRect(tipX, y0 + height - thickness, tipWidth, thickness);

        Integer veteranBsp = asset.getVeteranBsp();
        if (veteranBsp != null) {
            drawActivePair(g, x0 + gapStart + 4, y0 + height / 2, Integer.toString(asset.getBsp()),
                  Integer.toString(veteranBsp), asset.isVeteranCrew(), costFont, false);
        } else {
            text(asset.getCostDisplay()).at(x0 + gapStart + 4, y0 + height / 2)
                  .centerY().font(costFont).color(Color.BLACK).draw(g);
        }
    }

    /**
     * Draws a Regular(Veteran) value pair (used for the cost and skill), rendering the value that matches the crew's
     * current level in the bold {@code activeFont} and the other value plus the parentheses in a lighter plain weight,
     * so the active value stands out. Segments are laid out left-to-right and either centered or left-aligned on
     * {@code anchorX}. Single-value assets never call this (their lone value is drawn plainly bold as before).
     *
     * @param regular       the Regular value text
     * @param veteran       the Veteran value text
     * @param veteranActive true if the crew is Veteran (so the Veteran value is the bold one)
     * @param activeFont    the bold font for the active value
     * @param centered      true to center the pair on {@code anchorX}; false to left-align at it
     */
    private void drawActivePair(Graphics2D g, int anchorX, int y, String regular, String veteran,
          boolean veteranActive, Font activeFont, boolean centered) {
        Font plainFont = activeFont.deriveFont(Font.PLAIN);
        Font regFont = veteranActive ? plainFont : activeFont;
        Font vetFont = veteranActive ? activeFont : plainFont;

        int wReg = g.getFontMetrics(regFont).stringWidth(regular);
        int wOpen = g.getFontMetrics(plainFont).stringWidth("(");
        int wVet = g.getFontMetrics(vetFont).stringWidth(veteran);
        int wClose = g.getFontMetrics(plainFont).stringWidth(")");

        int x = centered ? anchorX - (wReg + wOpen + wVet + wClose) / 2 : anchorX;
        Color regColor = veteranActive ? INACTIVE_GRAY : Color.BLACK;
        Color vetColor = veteranActive ? Color.BLACK : INACTIVE_GRAY;
        text(regular).at(x, y).centerY().font(regFont).color(regColor).draw(g);
        x += wReg;
        text("(").at(x, y).centerY().font(plainFont).color(INACTIVE_GRAY).draw(g);
        x += wOpen;
        text(veteran).at(x, y).centerY().font(vetFont).color(vetColor).draw(g);
        x += wVet;
        text(")").at(x, y).centerY().font(plainFont).color(INACTIVE_GRAY).draw(g);
    }

    private void paintTitle(Graphics2D g) {
        int rightX = WIDTH - 60;
        text(asset.getEffectiveCardTitle().toUpperCase(Locale.ROOT))
              .at(rightX, 104).rightAlign().centerY().font(titleFont).maxWidth(660).scaleX(0.95f)
              .color(Color.BLACK).draw(g);

        String subtitle = asset.getEffectiveCardSubtitle();
        if (subtitle != null && !subtitle.isBlank()) {
            text(subtitle.toUpperCase(Locale.ROOT))
                  .at(rightX, 172).rightAlign().centerY().font(subtitleFont).maxWidth(660)
                  .color(Color.BLACK).draw(g);
        }
    }

    /** Draws the MP and TMM lines. Positioned lower on the card so the unit name above can be larger. */
    private void paintMovement(Graphics2D g) {
        int labelX = 620;
        int valueRightX = WIDTH - 52;
        int mpY = 350;
        int tmmY = 420;

        text("MP").at(labelX, mpY).centerY().font(movementFont).color(accentColor()).draw(g);
        drawMovementValue(g, valueRightX, mpY, asset.getMovementDisplay());

        text("TMM").at(labelX, tmmY).centerY().font(movementFont).color(accentColor()).draw(g);
        text(asset.getTmmDisplay()).at(valueRightX, tmmY).rightAlign().centerY()
              .font(movementFont).maxWidth(240).color(Color.BLACK).draw(g);
    }

    /**
     * Draws a movement value right-aligned at {@code rightX}, rendering the trailing movement-mode letter (e.g. the
     * {@code H} of {@code 8H}) smaller than the number, in a small-caps style sitting on the number's baseline.
     */
    private void drawMovementValue(Graphics2D g, int rightX, int y, String value) {
        int split = 0;
        while (split < value.length() && !Character.isLetter(value.charAt(split))) {
            split++;
        }
        String number = value.substring(0, split);
        String letter = value.substring(split);

        Font smallFont = movementFont.deriveFont(movementFont.getSize2D() * 0.6f);
        int numberWidth = g.getFontMetrics(movementFont).stringWidth(number);
        int letterWidth = letter.isEmpty() ? 0 : g.getFontMetrics(smallFont).stringWidth(letter);
        int startX = rightX - numberWidth - letterWidth;
        int letterDrop = Math.round((movementFont.getSize2D() - smallFont.getSize2D()) * 0.34f);

        text(number).at(startX, y).centerY().font(movementFont).color(Color.BLACK).draw(g);
        if (!letter.isEmpty()) {
            text(letter).at(startX + numberWidth, y + letterDrop).centerY().font(smallFont)
                  .color(Color.BLACK).draw(g);
        }
    }

    /** Draws the RANGE / SKILL / DMG / CHECK stats: evenly spaced labels with each value centered beneath its label. */
    private void paintStats(Graphics2D g) {
        String[] labels = { "RANGE", "SKILL", "DMG", "CHECK" };
        String[] values = {
              asset.getRangeDisplay(),
              asset.getSkillDisplay(),
              asset.getDamageDisplay(),
              Integer.toString(asset.getDestroyCheck())
        };
        // Evenly spaced column centers; labels sit at these centers and each value is centered beneath its label.
        int[] columnCenterX = { 170, 410, 650, 890 };
        int labelY = 504;
        int valueY = 552;
        int checkColumn = labels.length - 1;
        int skillColumn = 1;
        int damageColumn = 2;

        for (int i = 0; i < labels.length; i++) {
            text(labels[i]).at(columnCenterX[i], labelY).center().font(labelFont)
                  .color(accentColor()).draw(g);

            // The SKILL value bolds the Regular or Veteran number matching the crew's current level.
            Integer veteranSkill = asset.getVeteranSkill();
            if ((i == skillColumn) && (veteranSkill != null)) {
                drawActivePair(g, columnCenterX[i], valueY, Integer.toString(asset.getSkill()),
                      Integer.toString(veteranSkill), asset.isVeteranCrew(), valueFont, true);
                continue;
            }

            if (i == damageColumn) {
                drawDamageValue(g, columnCenterX[i], valueY);
                continue;
            }

            // The CHECK value shows persistent damage: an undamaged asset shows a single value, a damaged one shows the
            // struck-through original next to the current value. The play write-in box follows either form.
            if (i == checkColumn) {
                paintCheckColumn(g, columnCenterX[i], valueY);
                continue;
            }

            text(values[i]).at(columnCenterX[i], valueY).center().font(valueFont)
                  .maxWidth(210).color(Color.BLACK).draw(g);
        }

        // Threshold, centered beneath the CHECK column.
        text("THRESHOLD: " + asset.getThreshold()).at(columnCenterX[checkColumn], valueY + 50)
              .center().font(thresholdFont).color(Color.BLACK).draw(g);
    }

    /**
     * Draws damage as {@code perHit DX hits}, with the {@code D} and {@code X} reduced like small capitals. Assets that
     * deal no damage retain the em dash display.
     */
    private void drawDamageValue(Graphics2D g, int centerX, int y) {
        BFSDamage damage = asset.getDamage();
        if (!damage.hasDamage()) {
            text(damage.displayString()).at(centerX, y).center().font(valueFont)
                  .maxWidth(210).color(Color.BLACK).draw(g);
            return;
        }

        String perHit = Integer.toString(damage.perHit());
        String hits = Integer.toString(damage.hits());
        String smallCaps = "DX";
        Font numberFont = valueFont;
        Font smallCapsFont = valueFont.deriveFont(valueFont.getSize2D() * 0.58f);
        int totalWidth = damageValueWidth(g, perHit, hits, smallCaps, numberFont, smallCapsFont);
        if (totalWidth > 210) {
            float scale = 210f / totalWidth;
            numberFont = numberFont.deriveFont(numberFont.getSize2D() * scale);
            smallCapsFont = smallCapsFont.deriveFont(smallCapsFont.getSize2D() * scale);
            totalWidth = damageValueWidth(g, perHit, hits, smallCaps, numberFont, smallCapsFont);
        }

        int perHitWidth = g.getFontMetrics(numberFont).stringWidth(perHit);
        int smallCapsWidth = g.getFontMetrics(smallCapsFont).stringWidth(smallCaps);
        int startX = centerX - totalWidth / 2;
        int smallCapsDrop = Math.round((numberFont.getSize2D() - smallCapsFont.getSize2D()) * 0.34f);

        text(perHit).at(startX, y).centerY().font(numberFont).color(Color.BLACK).draw(g);
        text(smallCaps).at(startX + perHitWidth, y + smallCapsDrop).centerY().font(smallCapsFont)
              .color(Color.BLACK).draw(g);
        text(hits).at(startX + perHitWidth + smallCapsWidth, y).centerY().font(numberFont)
              .color(Color.BLACK).draw(g);
    }

    private static int damageValueWidth(Graphics2D g, String perHit, String hits, String smallCaps, Font numberFont,
          Font smallCapsFont) {
        return g.getFontMetrics(numberFont).stringWidth(perHit)
              + g.getFontMetrics(smallCapsFont).stringWidth(smallCaps)
              + g.getFontMetrics(numberFont).stringWidth(hits);
    }

    /**
     * Draws the CHECK value (centered at {@code centerX}) followed by the play write-in box. When the asset is damaged
     * (its current Destroy Check differs from the as-constructed one), the as-constructed value is shown struck through
     * in grey next to the current value; otherwise the single current value is shown in black.
     */
    private void paintCheckColumn(Graphics2D g, int centerX, int valueY) {
        int original = asset.getODestroyCheck();
        int current = asset.getDestroyCheck();

        Rectangle valueBounds;
        if (current != original) {
            valueBounds = drawDamagedCheck(g, centerX, valueY, Integer.toString(original), Integer.toString(current));
        } else {
            valueBounds = text(Integer.toString(current)).at(centerX, valueY).center().font(valueFont)
                  .maxWidth(210).color(Color.BLACK).draw(g);
        }

        // Degradation write-in box, to the right of the value(s), for play use.
        int writeBoxW = 64;
        int writeBoxH = 50;
        int writeBoxX = valueBounds.x + valueBounds.width + 22;
        int writeBoxY = valueY - writeBoxH / 2;
        g.setColor(WHITE);
        g.fillRect(writeBoxX, writeBoxY, writeBoxW, writeBoxH);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2.4f));
        g.drawRect(writeBoxX, writeBoxY, writeBoxW, writeBoxH);
    }

    /**
     * Draws a damaged Destroy Check as {@code original current}: the as-constructed value struck through in grey, then
     * the current value in black (in {@link ColorMode#NONE}) or the damage color (in a color mode). The pair is centered
     * on {@code centerX}.
     *
     * @return the bounding box of the whole pair, so the caller can position the write-in box after it
     */
    private Rectangle drawDamagedCheck(Graphics2D g, int centerX, int y, String original, String current) {
        int gap = 14;
        int wOriginal = g.getFontMetrics(valueFont).stringWidth(original);
        int wCurrent = g.getFontMetrics(valueFont).stringWidth(current);
        int totalWidth = wOriginal + gap + wCurrent;
        int leftX = centerX - totalWidth / 2;

        // Original: grey, struck through.
        Rectangle originalBounds = text(original).at(leftX, y).centerY().font(valueFont)
              .color(INACTIVE_GRAY).draw(g);
        g.setColor(INACTIVE_GRAY);
        g.setStroke(new BasicStroke(2.4f));
        g.drawLine(leftX, y, leftX + wOriginal, y);

        // Current: black in black-and-white, the damage color in a color mode.
        Color currentColor = (colorMode == ColorMode.NONE) ? Color.BLACK : damageColor;
        text(current).at(leftX + wOriginal + gap, y).centerY().font(valueFont).color(currentColor).draw(g);

        return new Rectangle(leftX, y - originalBounds.height / 2, totalWidth, originalBounds.height);
    }

    private void paintSpecials(Graphics2D g) {
        int textY = 630;
        int headerWidth = text("SPECIALS: ").at(55, textY).centerY()
              .font(specialsHeaderFont).color(accentColor()).draw(g).width + 16;

        String specials = asset.getSpecialsDisplay();
        if (specials == null || specials.isBlank()) {
            specials = EM_DASH;
        }
        text(specials).at(55 + headerWidth, textY).centerY()
              .font(specialsFont).maxWidth(WIDTH - 90 - headerWidth).color(Color.BLACK).draw(g);
    }

    private void paintFooter(Graphics2D g) {
        // BATTLETECH logo, sitting in the gap so it interrupts the bottom border, spanning the two branding lines.
        // Rendered from a vector SVG extracted from the record sheets, so it is real <path> geometry in the print
        // output; in LOGO_ONLY/ALL modes the logo's "A" is the record-sheet gold, otherwise it is all black.
        GraphicsNode logo = (colorMode == ColorMode.NONE) ? bwLogoNode() : colorLogoNode();
        if (logo != null) {
            paintVectorLogo(g, logo, 68, 715, 344);
        } else {
            // Fallback to the raster B&W logo if the vector logo could not be loaded.
            if (scaledBtLogo == null && BT_LOGO != null) {
                scaledBtLogo = new ImageIcon(BT_LOGO.getScaledInstance(340, 59, Image.SCALE_AREA_AVERAGING));
            }
            if (scaledBtLogo != null) {
                g.drawImage(scaledBtLogo.getImage(), 68, 687, null);
            }
        }

        // BATTLEFIELD / SUPPORT on two lines beside the logo, spanning the same vertical space as the logo.
        text("BATTLEFIELD").at(420, 702).centerY().font(brandFont).color(Color.BLACK).draw(g);
        text("SUPPORT").at(420, 734).centerY().font(brandFont).color(Color.BLACK).draw(g);

        // Copyright, nestled in the raised border shelf at the bottom-right.
        text("\u00A9 " + LocalDate.now().getYear() + " The Topps Company. All rights reserved.")
              .at(WIDTH - 40, HEIGHT - 24).rightAlign().centerY().font(copyrightFont).color(Color.BLACK).draw(g);
    }

    private static GraphicsNode bwLogoNode() {
        if (!btLogoBwLoaded) {
            btLogoBwNode = loadSvgNode(FILENAME_BT_LOGO_BW_SVG);
            btLogoBwLoaded = true;
        }
        return btLogoBwNode;
    }

    private static GraphicsNode colorLogoNode() {
        if (!btLogoColorLoaded) {
            btLogoColorNode = loadSvgNode(FILENAME_BT_LOGO_COLOR_SVG);
            btLogoColorLoaded = true;
        }
        return btLogoColorNode;
    }

    /** Parses an SVG file from the misc images directory into a Batik {@link GraphicsNode}, or {@code null} on error. */
    private static @Nullable GraphicsNode loadSvgNode(String filename) {
        java.io.File file = new MegaMekFile(Configuration.miscImagesDir(), filename).getFile();
        if (!file.exists()) {
            logger.warn("BFS card logo SVG not found: {}", file);
            return null;
        }
        try (InputStream is = Files.newInputStream(file.toPath())) {
            SAXDocumentFactory factory = new SAXDocumentFactory(SVGDOMImplementation.getDOMImplementation(),
                  XMLResourceDescriptor.getXMLParserClassName());
            Document doc = factory.createDocument(file.toURI().toASCIIString(), is);
            BridgeContext ctx = new BridgeContext(new UserAgentAdapter());
            ctx.setDynamicState(BridgeContext.STATIC);
            return new GVTBuilder().build(ctx, doc);
        } catch (Exception e) {
            logger.error(e, "Failed to load BFS card logo SVG: " + filename);
            return null;
        }
    }

    /**
     * Paints a vector {@link GraphicsNode} (the BATTLETECH logo) scaled to {@code targetWidth}, preserving its aspect
     * ratio, with its left edge at {@code x} and vertically centered on {@code yCenter}. Because the node paints by
     * issuing shape fills, this yields raster output on a {@link BufferedImage} and vector {@code <path>}s on a Batik
     * SVG canvas.
     */
    private static void paintVectorLogo(Graphics2D g, GraphicsNode node, int x, int yCenter, int targetWidth) {
        Rectangle2D bounds = node.getBounds();
        if ((bounds == null) || (bounds.getWidth() <= 0) || (bounds.getHeight() <= 0)) {
            return;
        }
        double scale = targetWidth / bounds.getWidth();
        double targetHeight = bounds.getHeight() * scale;
        AffineTransform saved = g.getTransform();
        g.translate(x, yCenter - targetHeight / 2.0);
        g.scale(scale, scale);
        g.translate(-bounds.getX(), -bounds.getY());
        node.paint(g);
        g.setTransform(saved);
    }
}
