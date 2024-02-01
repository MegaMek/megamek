/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing.tileset;

import megamek.common.Entity;
import megamek.common.FighterSquadron;
import megamek.common.icons.Camouflage;
import megamek.common.util.ImageUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This is a specialized version of EntityImage for FighterSquadrons that assembles its icon from the
 * icons of the fighters that make up the squadron.
 */
public class FighterSquadronIcon extends EntityImage {

    // The following are lists of places within the hex that the images of the fighters are placed on. For
    // some numbers of fighters there is more than one list. One of the lists and thus one
    // of the arrangements is chosen. The choice is made based upon the hash value of a string
    // that is all the chassis contained in the squadron sorted and chained together, i.e. the same squadron
    // will always end up using the same arrangement. See positionHash.
    // To add a new arrangement, add a list of points and include that new list in the positions Map below
    // with the key corresponding to the number of points in the list (i.e. the number of fighters for which
    // it should be used - if it has 5 points, add it to the list for key 5.)

    private static final List<Point> positions1 = List.of(new Point(0, 0));
    private static final List<Point> positions2A = List.of(new Point(-7, -18), new Point(7, 18));
    private static final List<Point> positions2B = List.of(new Point(7, -18), new Point(-7, 18));
    private static final List<Point> positions3A = List.of(new Point(4, -20), new Point(20, 12),
            new Point(-20, 0));
    private static final List<Point> positions3B = List.of(new Point(-4, -20), new Point(-20, 12),
            new Point(20, 0));
    private static final List<Point> positions4A = List.of(new Point(4, -20), new Point(20, 4),
            new Point(-20, -4), new Point(-4, 20));
    private static final List<Point> positions4B = List.of(new Point(-4, -20), new Point(-20, 4),
            new Point(20, -4), new Point(4, 20));
    private static final List<Point> positions4C = List.of(new Point(0, -20), new Point(-20, 0),
            new Point(20, 0), new Point(0, 20));
    private static final List<Point> positions5A = List.of(new Point(0, -20), new Point(13, 20),
            new Point(-13, 20), new Point(-20, -2), new Point(20, -2));
    private static final List<Point> positions5B = List.of(new Point(0, 20), new Point(13, -20),
            new Point(-13, -20), new Point(-20, 2), new Point(20, 2));
    private static final List<Point> positions5C = List.of(new Point(0, 0), new Point(13, -20),
            new Point(-13, -20), new Point(-13, 20), new Point(13, 20));
    private static final List<Point> positions6A = List.of(new Point(-13, -20), new Point(13, -20),
            new Point(13, 20), new Point(-13, 20), new Point(-20, -2), new Point(20, -2));
    private static final List<Point> positions6B = List.of(new Point(-13, -20), new Point(13, -20),
            new Point(13, 20), new Point(-13, 20), new Point(-25, 0), new Point(0, 0));
    private static final List<Point> positions7A = List.of(new Point(-13, -20), new Point(13, -20),
            new Point(13, 20), new Point(-13, 20), new Point(-25, 0), new Point(0, 0),
            new Point(25, 0));
    private static final List<Point> positions8A = List.of(new Point(-13, -24), new Point(13, -24),
            new Point(13, 24), new Point(-13, 24), new Point(-25, -8), new Point(0, -8),
            new Point(25, 8), new Point(0, 8));
    private static final List<Point> positions9A = List.of(new Point(-13, -24), new Point(13, -24),
            new Point(13, 24), new Point(-13, 24), new Point(-25, -8), new Point(0, -8),
            new Point(25, 8), new Point(0, 8), new Point(-25, 8));
    private static final List<Point> positions10A = List.of(new Point(-13, -24), new Point(13, -24),
            new Point(13, 24), new Point(-13, 24), new Point(-25, -8), new Point(0, -8),
            new Point(25, -8), new Point(0, 8), new Point(-25, 8), new Point(25, 8));

    // This map is queried when constructing the icon. The key is the number of fighters in the squadron,
    // thus the lists in the value for a key should all have that number of points in them; e.g. the lists for
    // key 3 (positions3A, 3B etc.) all have exactly 3 points
    private static final Map<Integer, List<List<Point>>> positions = Map.of(
            1, List.of(positions1),
            2, List.of(positions2A, positions2B),
            3, List.of(positions3A, positions3B),
            4, List.of(positions4A, positions4B, positions4C),
            5, List.of(positions5A, positions5B, positions5C),
            6, List.of(positions6A, positions6B),
            7, List.of(positions7A),
            8, List.of(positions8A),
            9, List.of(positions9A),
            10, List.of(positions10A));

    private final FighterSquadron squadron;

    /**
     * The positionHash is used to determine the specific arrangement of the fighter icons. It should remain the
     * same for any specific squadron through saving/loading and not change from damage. It can change when fighters
     * are removed as the arrangement of icons changes then anyway. Everything else is a matter of design (which
     * squadrons should use the same arrangement?)
     */
    private final int positionHash;

    public FighterSquadronIcon(Image base, Image wreck, Camouflage camouflage, Component comp, Entity entity, int secondaryPos, boolean preview) {
        super(base, wreck, camouflage, comp, entity, secondaryPos, preview);
        if (entity instanceof FighterSquadron) {
            squadron = (FighterSquadron) entity;
        } else {
            throw new IllegalArgumentException("Can only be used for FighterSquadrons");
        }
        // The positionHash is constructed by sorting and chaining the chassis names and taking the hash of the result
        // Similar squadrons with different models will thus use the same arrangement. This could be changed by including
        // the models. Overall it's probably difficult to notice the difference, so this solution is good enough
        String hashString = squadron.getSubEntities().stream().map(Entity::getChassis).sorted().collect(Collectors.joining());
        positionHash = Math.abs(hashString.hashCode());
    }

    @Override
    public Image loadPreviewImage(boolean showDamage) {
        base = ImageUtil.createAcceleratedImage(84, 72);
        Graphics2D g = (Graphics2D) getBase().getGraphics();
        int index = 0;
        int totalUnitCount = squadron.getActiveSubEntities().size();
        for (Entity fighter : squadron.getActiveSubEntities()) {
            final Image base = MMStaticDirectoryManager.getMechTileset().imageFor(fighter);
            var ei = EntityImage.createIcon(base, fighter.getCamouflageOrElseOwners(), new JPanel(), fighter)
                    .loadPreviewImage(true);
            int height = getSize(totalUnitCount);
            if ((ei.getHeight(null) > 0) && (ei.getWidth(null) > 0)) {
                int width = height * ei.getWidth(null) / ei.getHeight(null);
                BufferedImage imm = ImageUtil.getScaledImage(ei, width, height);
                Point position = getPosition(index, totalUnitCount);
                g.drawImage(imm, 42 + position.x - imm.getWidth() / 2, 36 + position.y - imm.getHeight() / 2, null);
            }
            index++;
        }
        g.dispose();
        return getBase();
    }

    /**
     * @return The pixel position in the hex of the fighter of the given index (from 0 to n-1 where n is the number
     * of fighters in the squadron) for the given total count (n) of fighters in the squadron
     */
    private Point getPosition(int entityIndex, int totalUnitCount) {
        if (positions.containsKey(totalUnitCount)) {
            int variationCount = positions.get(totalUnitCount).size();
            int variation = 0;
            if (variationCount > 1) {
                variation = positionHash % variationCount;
            }
            return positions.get(totalUnitCount).get(variation).get(entityIndex);
        } else {
            return new Point(entityIndex * 10, entityIndex * 8);
        }
    }

    /** @return The pixel size of each fighter icon for the given count of fighters in the squadron */
    private int getSize(int totalUnitCount) {
        if (totalUnitCount <= 3) {
            return 32;
        } else if (totalUnitCount == 4) {
            return 30;
        } else if (totalUnitCount <= 7) {
            return 25;
        } else {
            return 18;
        }
    }

    @Override
    public void loadFacings() {
        loadPreviewImage(false);

        // Save a small icon (without damage decals) for the unit overview
        icon = ImageUtil.getScaledImage(base, 56, 48);

        for (int i = 0; i < 6; i++) {
            // Generate rotated images for the unit and for a wreck
            facings[i] = rotateImage(getBase(), i);
        }
    }
}