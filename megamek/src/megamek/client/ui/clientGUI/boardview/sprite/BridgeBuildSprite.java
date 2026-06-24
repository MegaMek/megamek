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

import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ImageIcon;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.tileset.HexTileset;
import megamek.client.ui.util.UIUtil;
import megamek.common.Configuration;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.util.ImageUtil;
import megamek.logging.MMLogger;

/**
 * Displays a bridge-under-construction indicator on the hex where Bridge-Building Engineers are raising a bridge
 * (TO:AUE). Shows the finished bridge graphic for the build's orientation as a ghost image whose opacity equals the
 * build progress (turn 1 of 6 = 1/6 visible, the final turn fully visible), so the bridge appears piece by piece over
 * the course of the work, plus the progress as text ("2/6") in the lower portion of the hex.
 */
public class BridgeBuildSprite extends HexSprite {

    private static final MMLogger LOGGER = MMLogger.create(BridgeBuildSprite.class);

    private static final Color TEXT_OUTLINE_COLOR = new Color(40, 40, 50);

    /** Stroke width (unscaled hex pixels) of the yellow/black hazard outline drawn around an emphasized hex. */
    private static final float HAZARD_OUTLINE_WIDTH = 4.0f;

    /** Dash length (unscaled hex pixels) for the alternating yellow/black hazard stripes. */
    private static final float HAZARD_DASH_LENGTH = 8.0f;

    /** Solid backing behind the caution-coloured stripes, giving the high-contrast yellow/black hazard-tape look. */
    private static final Color HAZARD_BACKING_COLOR = Color.BLACK;

    private static final int HEX_CENTER_X = HexTileset.HEX_W / 2;
    private static final int FONT_SIZE = 11;
    private static final int BOTTOM_OFFSET = 24;

    /** Minimum ghost opacity while building, so the planned bridge is faintly visible before any turn is banked. */
    private static final float GHOST_MIN_ALPHA = 0.18f;

    /** The standard tileset bridge images by exits bitmask, loaded once and shared by all sprites. */
    private static final Map<Integer, Image> BRIDGE_IMAGES = new HashMap<>();

    private final int turnsWorked;
    private final int turnsRequired;
    private final int exits;
    private final boolean hazardOutline;

    /**
     * Creates a new bridge building sprite for the given hex. The sprite shows how much of the bridge is currently
     * standing, on the build's {@code N / build-required} scale: a build counts this up as turns are banked, a
     * dismantling counts the same number back down to zero.
     *
     * @param boardView     the parent board view
     * @param location      the hex the bridge is being raised in
     * @param turnsWorked   the turns of structure currently standing (0 to {@code turnsRequired})
     * @param turnsRequired the total turns of work a finished bridge needs (the denominator)
     * @param exits         exits bitmask of the two hexsides the finished bridge will connect
     */
    public BridgeBuildSprite(BoardView boardView, Coords location, int turnsWorked, int turnsRequired, int exits) {
        this(boardView, location, turnsWorked, turnsRequired, exits, false);
    }

    /**
     * Creates a new bridge indicator sprite, optionally ringed with a bold yellow/black hazard outline. A short-lived
     * indicator (such as a one-turn Bridge-Layer deployment, whose ghost is easy to overlook - colour-blind players in
     * particular flagged it as hard to see) uses the outline so the target hex is unmistakable; the multi-turn engineer
     * build leaves it off and relies on its growing ghost.
     *
     * @param boardView     the parent board view
     * @param location      the hex the bridge is being raised in
     * @param turnsWorked   the turns of structure currently standing (0 to {@code turnsRequired})
     * @param turnsRequired the total turns of work a finished bridge needs (the denominator)
     * @param exits         exits bitmask of the two hexsides the finished bridge will connect
     * @param hazardOutline whether to ring the hex with a high-visibility yellow/black hazard outline
     */
    public BridgeBuildSprite(BoardView boardView, Coords location, int turnsWorked, int turnsRequired, int exits,
          boolean hazardOutline) {
        super(boardView, location);
        this.turnsWorked = turnsWorked;
        this.turnsRequired = turnsRequired;
        this.exits = exits;
        this.hazardOutline = hazardOutline;
    }

    @Override
    public void prepare() {
        Graphics2D graph = spriteSetup();
        drawGhostBridge(graph);
        if (hazardOutline) {
            drawHazardOutline(graph);
        }
        drawProgressIndicator(graph);
        graph.dispose();
    }

    /**
     * Rings the hex with a bold yellow/black hazard outline so the target is unmistakable even when the ghost bridge is
     * hard to make out - in particular for colour-blind players, who flagged the plain ghost as hard to see. The yellow
     * stripes use the themeable caution colour (so a player can recolour them), drawn as dashes over a solid black
     * backing so the alternating yellow/black hazard-tape pattern stays high-contrast on any terrain.
     *
     * @param graph the sprite graphics
     */
    private void drawHazardOutline(Graphics2D graph) {
        graph.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Stroke oldStroke = graph.getStroke();
        // Solid black base, then caution-yellow dashes on top: the gaps reveal the black underneath for hazard tape.
        graph.setColor(HAZARD_BACKING_COLOR);
        graph.setStroke(new BasicStroke(HAZARD_OUTLINE_WIDTH));
        graph.drawPolygon(BoardView.getHexPoly());
        graph.setColor(GUIPreferences.getInstance().getCautionColor());
        graph.setStroke(new BasicStroke(HAZARD_OUTLINE_WIDTH, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
              new float[] { HAZARD_DASH_LENGTH, HAZARD_DASH_LENGTH }, 0.0f));
        graph.drawPolygon(BoardView.getHexPoly());
        graph.setStroke(oldStroke);
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
     * Draws the finished bridge's graphic as a ghost image whose opacity equals the build progress: turn 1 of 6 shows
     * it at 1/6 opacity, the final turn at full opacity, so the bridge appears over the course of the work.
     *
     * @param graph the sprite graphics
     */
    private void drawGhostBridge(Graphics2D graph) {
        Image bridgeImage = getBridgeImage(exits);
        if (bridgeImage == null) {
            return;
        }
        // Opacity tracks the standing structure: it grows as a build banks turns and shrinks as a dismantling counts
        // them back down. A faint floor keeps the planned/last sliver of bridge visible.
        float standing = Math.clamp((float) turnsWorked / Math.max(1, turnsRequired), 0f, 1f);
        float alpha = Math.max(GHOST_MIN_ALPHA, standing);

        Composite oldComposite = graph.getComposite();
        graph.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        graph.drawImage(bridgeImage, 0, 0, HexTileset.HEX_W, HexTileset.HEX_H, null);
        graph.setComposite(oldComposite);
    }

    /**
     * Draws the build progress ("2/6") as text in the lower portion of the hex, where it stays visible next to units
     * occupying the hex.
     *
     * @param graph the sprite graphics
     */
    private void drawProgressIndicator(Graphics2D graph) {
        graph.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        String progress = turnsWorked + "/" + turnsRequired;
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
     * @param exits exits bitmask of the bridge under construction
     *
     * @return The standard tileset bridge image for that orientation ("bridge/bridge_NN.gif"), or null if it cannot be
     *       loaded. Images are cached and shared by all sprites.
     */
    private static @Nullable Image getBridgeImage(int exits) {
        return BRIDGE_IMAGES.computeIfAbsent(exits & 63, exitsKey -> {
            File imageFile = new File(Configuration.hexesDir(), String.format("bridge/bridge_%02d.gif", exitsKey));
            if (!imageFile.exists()) {
                LOGGER.warn("[BuildBridge] ghost bridge image not found: {}; showing progress text only",
                      imageFile);
                return null;
            }
            // ImageUtil does not guarantee the image is fully loaded; a sprite paints its buffer only once, so
            // a still-decoding image would silently draw nothing. ImageIcon blocks until the image is loaded.
            return new ImageIcon(ImageUtil.loadImageFromFile(imageFile.toString())).getImage();
        });
    }

    @Override
    public boolean isBehindTerrain() {
        return false;
    }
}
