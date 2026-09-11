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
package megamek.client.ui.panels;

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;

import megamek.client.ui.clientGUI.ClientGUI;
import megamek.common.Hex;
import megamek.common.units.Entity;

/**
 * Six facing choices laid out around a unit's picture the way the hex sides sit on the map: north on top, south at
 * the bottom, the other four on the sides. The turret dialogs and the building facing dialog share it, so a building
 * is turned the way a turret is.
 */
public class FacingPickerPanel extends JPanel {
    @Serial
    private static final long serialVersionUID = 6421838219703351744L;

    /** The hex-shaped preview is drawn at the size of one map hex. */
    private static final int PREVIEW_WIDTH = 84;
    private static final int PREVIEW_HEIGHT = 72;
    private static final double DEGREES_PER_FACING = 60.0;

    /**
     * @param facings      the six radio buttons, in facing order 0 to 5, already grouped, selected and enabled by the
     *                     caller
     * @param centreImage  the picture shown in the middle, normally {@link #previewOnHex(ClientGUI, Entity, int)}
     */
    public FacingPickerPanel(List<JRadioButton> facings, Image centreImage) {
        super(new BorderLayout());
        JPanel north = new JPanel(new GridBagLayout());
        JPanel west = new JPanel(new BorderLayout());
        JPanel east = new JPanel(new BorderLayout());
        JPanel south = new JPanel(new GridBagLayout());
        north.add(facings.getFirst());
        south.add(facings.get(3));
        west.add(facings.get(5), BorderLayout.NORTH);
        west.add(facings.get(4), BorderLayout.SOUTH);
        east.add(facings.get(1), BorderLayout.NORTH);
        east.add(facings.get(2), BorderLayout.SOUTH);

        JLabel picture = new JLabel(new ImageIcon(centreImage));
        picture.setHorizontalAlignment(SwingConstants.CENTER);
        picture.setOpaque(false);

        add(north, BorderLayout.NORTH);
        add(west, BorderLayout.WEST);
        add(picture, BorderLayout.CENTER);
        add(east, BorderLayout.EAST);
        add(south, BorderLayout.SOUTH);
    }

    /**
     * The unit's preview picture drawn on a map hex, turned to the given facing so the six choices line up with what
     * the player sees on the board.
     *
     * @param clientgui the client GUI that supplies the unit and hex images
     * @param unit      the unit to draw
     * @param facing    the facing to turn the picture to; {@code 0} leaves it pointing north
     *
     * @return the composed picture
     */
    public static Image previewOnHex(ClientGUI clientgui, Entity unit, int facing) {
        JLabel loader = new JLabel();
        clientgui.loadPreviewImage(loader, unit);
        Image unitImage = ((ImageIcon) loader.getIcon()).getImage();
        Image hexImage = clientgui.getTilesetManager().baseFor(new Hex());
        BufferedImage composed = new BufferedImage(PREVIEW_WIDTH, PREVIEW_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = composed.createGraphics();
        graphics.drawImage(hexImage, 0, 0, null);
        graphics.rotate(Math.toRadians(facing * DEGREES_PER_FACING), PREVIEW_WIDTH / 2.0, PREVIEW_HEIGHT / 2.0);
        graphics.drawImage(unitImage, 0, 0, null);
        graphics.dispose();
        return composed;
    }
}
