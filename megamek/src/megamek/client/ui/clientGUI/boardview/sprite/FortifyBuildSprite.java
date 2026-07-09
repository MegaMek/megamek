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
 * Displays a fortification-under-construction indicator on the hex where Trench/Fieldworks Engineers - or a vehicle
 * with fieldworks equipment - are building a fortified hex (TO:AUE p.153). Shows the finished fortified-hex graphic
 * (sandbags) as a ghost image whose opacity equals the build progress (stage 1 of 3 = 1/3 visible, the final stage
 * fully visible), so the fortification appears piece by piece over the course of the work, plus the progress as text
 * ("2/3") in the lower portion of the hex.
 */
public class FortifyBuildSprite extends HexSprite {

    private static final MMLogger LOGGER = MMLogger.create(FortifyBuildSprite.class);

    private static final Color TEXT_OUTLINE_COLOR = new Color(40, 40, 50);

    private static final int HEX_CENTER_X = HexTileset.HEX_W / 2;
    private static final int FONT_SIZE = 11;
    private static final int BOTTOM_OFFSET = 24;

    /** Minimum ghost opacity while building, so the planned fortification is faintly visible from stage one. */
    private static final float GHOST_MIN_ALPHA = 0.18f;

    /** The standard tileset fortified-hex (sandbags) image, loaded once and shared by all sprites. */
    private static Image sandbagsImage;
    private static boolean sandbagsImageLoaded;

    private final int stage;
    private final int totalStages;

    /**
     * Creates a new fortification building sprite for the given hex.
     *
     * @param boardView   the parent board view
     * @param loc         the hex the fortification is being built in
     * @param stage       the fortification stage currently reached (1 to {@code totalStages})
     * @param totalStages the total turns of work a finished fortified hex needs (the denominator)
     */
    public FortifyBuildSprite(BoardView boardView, Coords loc, int stage, int totalStages) {
        super(boardView, loc);
        this.stage = stage;
        this.totalStages = totalStages;
    }

    @Override
    public void prepare() {
        Graphics2D graph = spriteSetup();
        drawGhostFortification(graph);
        drawProgressIndicator(graph);
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
     * Draws the finished fortified-hex graphic as a ghost image whose opacity equals the build progress: stage 1 of 3
     * shows it at 1/3 opacity, the final stage at full opacity, so the fortification appears over the course of the
     * work.
     *
     * @param graph the sprite graphics
     */
    private void drawGhostFortification(Graphics2D graph) {
        Image sandbags = getSandbagsImage();
        if (sandbags == null) {
            return;
        }
        float standing = Math.clamp((float) stage / Math.max(1, totalStages), 0f, 1f);
        float alpha = Math.max(GHOST_MIN_ALPHA, standing);

        Composite oldComposite = graph.getComposite();
        graph.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        graph.drawImage(sandbags, 0, 0, HexTileset.HEX_W, HexTileset.HEX_H, null);
        graph.setComposite(oldComposite);
    }

    /**
     * Draws the build progress ("2/3") as text in the lower portion of the hex, where it stays visible next to the unit
     * occupying the hex.
     *
     * @param graph the sprite graphics
     */
    private void drawProgressIndicator(Graphics2D graph) {
        graph.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        String progress = stage + "/" + totalStages;
        Font progressFont = new Font("Sans Serif", Font.BOLD, FONT_SIZE);
        FontMetrics metrics = graph.getFontMetrics(progressFont);
        int textX = HEX_CENTER_X - (metrics.stringWidth(progress) / 2);
        int textY = HexTileset.HEX_H - BOTTOM_OFFSET;

        // White text with dark outline for readability on any terrain
        graph.setFont(progressFont);
        graph.setColor(TEXT_OUTLINE_COLOR);
        graph.drawString(progress, textX - 1, textY);
        graph.drawString(progress, textX + 1, textY);
        graph.drawString(progress, textX, textY - 1);
        graph.drawString(progress, textX, textY + 1);
        graph.setColor(Color.WHITE);
        graph.drawString(progress, textX, textY);
    }

    /**
     * @return The standard tileset fortified-hex image ("boring/sandbags.gif"), or null if it cannot be loaded (the
     *       sprite then shows progress text only). The image is loaded once and shared by all sprites.
     */
    private static @Nullable Image getSandbagsImage() {
        if (!sandbagsImageLoaded) {
            sandbagsImageLoaded = true;
            File imageFile = new File(Configuration.hexesDir(), "boring/sandbags.gif");
            if (imageFile.exists()) {
                // ImageUtil does not guarantee the image is fully loaded; a sprite paints its buffer only once,
                // so a still-decoding image would silently draw nothing. ImageIcon blocks until it is loaded.
                sandbagsImage = new ImageIcon(ImageUtil.loadImageFromFile(imageFile.toString())).getImage();
            } else {
                LOGGER.warn("[Fortify] ghost fortification image not found: {}; showing progress text only",
                      imageFile);
            }
        }
        return sandbagsImage;
    }

    @Override
    public boolean isBehindTerrain() {
        return false;
    }
}
