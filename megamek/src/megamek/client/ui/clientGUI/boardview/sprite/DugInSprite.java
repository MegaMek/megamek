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
import java.io.File;
import javax.swing.ImageIcon;

import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.tileset.HexTileset;
import megamek.client.ui.util.UIUtil;
import megamek.common.Configuration;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.util.ImageUtil;
import megamek.logging.MMLogger;

/**
 * Displays a trench overlay on the hex of an infantry platoon that is digging in or dug in (TO:AR p.106). Distinct from
 * {@link FortifyBuildSprite}, which shows the sandbags of a multi-turn fortified-hex build: a dug-in platoon is a
 * one-turn personal posture, so this just draws the trench graphic - faintly while the platoon is still digging in (not
 * yet protected) and fully once it is dug in.
 */
public class DugInSprite extends HexSprite {

    private static final MMLogger LOGGER = MMLogger.create(DugInSprite.class);

    private static final Color TEXT_OUTLINE_COLOR = new Color(40, 40, 50);
    private static final int HEX_CENTER_X = HexTileset.HEX_W / 2;
    private static final int FONT_SIZE = 11;
    private static final int BOTTOM_OFFSET = 24;

    /** The trench overlay image, loaded once and shared by all sprites. */
    private static Image trenchImage;
    private static boolean trenchImageLoaded;

    private final float alpha;
    private final String label;

    /**
     * Creates a new dug-in trench sprite for the given hex.
     *
     * @param boardView the parent board view
     * @param loc       the hex the platoon is dug in / digging in
     * @param alpha     the opacity to draw the trench at (e.g. faint while digging in, full once dug in)
     * @param label     the status text to show in the hex (e.g. "Digging in" / "Dug in")
     */
    public DugInSprite(BoardView boardView, Coords loc, float alpha, String label) {
        super(boardView, loc);
        this.alpha = alpha;
        this.label = label;
    }

    @Override
    public void prepare() {
        Graphics2D graph = spriteSetup();
        drawTrenches(graph);
        drawLabel(graph);
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

    private void drawTrenches(Graphics2D graph) {
        Image trenches = getTrenchImage();
        if (trenches == null) {
            return;
        }
        Composite oldComposite = graph.getComposite();
        graph.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.clamp(alpha, 0f, 1f)));
        graph.drawImage(trenches, 0, 0, HexTileset.HEX_W, HexTileset.HEX_H, null);
        graph.setComposite(oldComposite);
    }

    /**
     * Draws the dig-in status text ("Digging in" / "Dug in") low in the hex, where it stays visible next to the platoon
     * occupying the hex - mirroring the progress text on {@link FortifyBuildSprite}.
     *
     * @param graph the sprite graphics
     */
    private void drawLabel(Graphics2D graph) {
        if ((label == null) || label.isBlank()) {
            return;
        }
        graph.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Font labelFont = new Font("Sans Serif", Font.BOLD, FONT_SIZE);
        FontMetrics metrics = graph.getFontMetrics(labelFont);
        int textX = HEX_CENTER_X - (metrics.stringWidth(label) / 2);
        int textY = HexTileset.HEX_H - BOTTOM_OFFSET;

        // White text with dark outline for readability on any terrain
        graph.setFont(labelFont);
        graph.setColor(TEXT_OUTLINE_COLOR);
        graph.drawString(label, textX - 1, textY);
        graph.drawString(label, textX + 1, textY);
        graph.drawString(label, textX, textY - 1);
        graph.drawString(label, textX, textY + 1);
        graph.setColor(Color.WHITE);
        graph.drawString(label, textX, textY);
    }

    /**
     * @return The trench overlay image ("boring/trenches.png"), or null if it cannot be loaded (the sprite then draws
     *       nothing). The image is loaded once and shared by all sprites.
     */
    private static @Nullable Image getTrenchImage() {
        if (!trenchImageLoaded) {
            trenchImageLoaded = true;
            File imageFile = new File(Configuration.hexesDir(), "boring/trenches.png");
            if (imageFile.exists()) {
                // ImageUtil does not guarantee the image is fully loaded; a sprite paints its buffer only once,
                // so a still-decoding image would silently draw nothing. ImageIcon blocks until it is loaded.
                trenchImage = new ImageIcon(ImageUtil.loadImageFromFile(imageFile.toString())).getImage();
            } else {
                LOGGER.warn("[Fortify] trench overlay image not found: {}; dug-in infantry will show no overlay",
                      imageFile);
            }
        }
        return trenchImage;
    }

    /**
     * Returns {@code true} so the trench draws as part of the hex pass (on top of the base terrain) but before the
     * entity sprites, which are drawn afterwards. This keeps the occupying platoon's icon on top of its trench instead
     * of being hidden underneath it.
     */
    @Override
    public boolean isBehindTerrain() {
        return true;
    }
}
