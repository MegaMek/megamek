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

public class FighterSquadronIcon extends EntityImage {

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

    private static final Map<Integer, List<List<Point>>> positions = Map.of(1, List.of(positions1),
            2, List.of(positions2A, positions2B), 3, List.of(positions3A, positions3B),
            4, List.of(positions4A, positions4B, positions4C),
            5, List.of(positions5A, positions5B, positions5C),
            6, List.of(positions6A, positions6B));

    private final FighterSquadron squadron;
    private final int positionHash;

    public FighterSquadronIcon(Image base, Camouflage camouflage, Component comp, Entity entity) {
        this(base, null, camouflage, comp, entity, -1, true);
    }

    public FighterSquadronIcon(Image base, Image wreck, Camouflage camouflage, Component comp, Entity entity, int secondaryPos) {
        this(base, wreck, camouflage, comp, entity, secondaryPos, false);
    }

    public FighterSquadronIcon(Image base, Image wreck, Camouflage camouflage, Component comp, Entity entity, int secondaryPos, boolean preview) {
        super(base, wreck, camouflage, comp, entity, secondaryPos, preview);
        if (entity instanceof FighterSquadron) {
            squadron = (FighterSquadron) entity;
        } else {
            throw new IllegalArgumentException("Can only be used for FighterSquadrons");
        }
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
            var ei = new EntityImage(base, fighter.getCamouflageOrElseOwners(), new JPanel(), squadron).loadPreviewImage(true);
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

    private int getSize(int totalUnitCount) {
        if (totalUnitCount < 4) {
            return 32;
        } else if (totalUnitCount == 4) {
            return 30;
        } else {
            return 22;
        }
    }

    @Override
    public void loadFacings() {
        // Save a small icon (without damage decals) for the unit overview
        icon = ImageUtil.getScaledImage(base,  56, 48);

        for (int i = 0; i < 6; i++) {
            // Generate rotated images for the unit and for a wreck
            facings[i] = rotateImage(getBase(), i);
        }
    }
}