/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.Point;
import java.awt.Polygon;
import java.io.Serial;

/**
 * This class calculates and stores points of polygon shaped as straight arrow. Minimum required arguments are two Point
 * elements - start and end of arrow.
 * <p> Special feature of this class is last boolean argument. It defines if it
 * will be full shaped arrow or left half only. Private Polygon hotArea contains same points as an arrow itself except
 * when arrow is changed to halved hotArea stays if full arrow shape. It was done in order to get only one tooltip for
 * two arrows in case of mutual attack.
 *
 * @author Slava Zipunov (zipp32)
 */
public class StraightArrowPolygon extends Polygon {
    @Serial
    private static final long serialVersionUID = 6865457471619747091L;
    private final Polygon hotArea = new Polygon();
    private final Point startPoint;
    private final Point endPoint;
    private int headLength = 30;
    private int headWidth = 5;
    private int arrowWidthAtHead = 3;
    private int tailWidth = 3;
    private final boolean halved;

    /**
     * Most extensive constructor with all parameters given
     */
    public StraightArrowPolygon(Point startPoint, Point endPoint, int headLength, int headWidth, int arrowWidthAtHead,
          int tailWidth, int tailLength, boolean halved) {
        super();
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.headLength = headLength;
        this.headWidth = headWidth;
        this.arrowWidthAtHead = arrowWidthAtHead;
        this.tailWidth = tailWidth;
        this.halved = halved;
        buildPointsArrays();
    }

    /**
     * Short constructor. Two points and boolean value.
     */
    public StraightArrowPolygon(Point startPoint, Point endPoint, boolean halved) {
        super();
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.halved = halved;
        buildPointsArrays();
    }

    /**
     * One more constructor
     */
    public StraightArrowPolygon(Point startPoint, Point endPoint, int width, boolean halved) {
        super();
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.headWidth = width + 2;
        this.arrowWidthAtHead = width;
        this.tailWidth = width;
        this.halved = halved;
        buildPointsArrays();
    }

    /**
     * I know, it is annoying, but another constructor
     */
    public StraightArrowPolygon(Point startPoint, Point endPoint, int width, int headWidth, boolean halved) {
        super();
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.headWidth = headWidth;
        this.arrowWidthAtHead = width;
        this.tailWidth = width;
        this.halved = halved;
        buildPointsArrays();
    }

    /**
     * Calculating and adding points to Polygon class. Some trigonometry.
     */
    private void buildPointsArrays() {
        int dX = endPoint.x - startPoint.x;
        int dY = endPoint.y - startPoint.y;
        double arrowLength = Math.sqrt(dX * dX + dY * dY);
        double sin = dY / arrowLength;
        double cos = dX / arrowLength;
        this.addPoint(startPoint.x, startPoint.y);
        int tailLength = 0;
        this.addPoint((int) Math.round(startPoint.x + tailWidth * sin
              - tailLength * cos), (int) Math.round(startPoint.y - tailWidth
              * cos - tailLength * sin));
        this.addPoint((int) Math.round(endPoint.x - headLength * cos
              + arrowWidthAtHead * sin), (int) Math.round(endPoint.y
              - headLength * sin - arrowWidthAtHead * cos));
        this.addPoint((int) Math.round(endPoint.x - headLength * cos
              + headWidth * sin), (int) Math.round(endPoint.y - headLength
              * sin - headWidth * cos));
        this.addPoint(endPoint.x, endPoint.y);
        if (!halved) {
            this.addPoint((int) Math.round(endPoint.x - headLength * cos
                  - headWidth * sin), (int) Math.round(endPoint.y
                  - headLength * sin + headWidth * cos));
            this.addPoint((int) Math.round(endPoint.x - headLength * cos
                  - arrowWidthAtHead * sin), (int) Math.round(endPoint.y
                  - headLength * sin + arrowWidthAtHead * cos));
            this.addPoint((int) Math.round(startPoint.x - tailWidth * sin
                  - tailLength * cos), (int) Math.round(startPoint.y
                  + tailWidth * cos - tailLength * sin));
        }
        hotArea.addPoint(startPoint.x, startPoint.y);
        hotArea.addPoint((int) Math.round(startPoint.x + tailWidth * sin
              - tailLength * cos), (int) Math.round(startPoint.y - tailWidth
              * cos - tailLength * sin));
        hotArea.addPoint((int) Math.round(endPoint.x - headLength * cos
              + arrowWidthAtHead * sin), (int) Math.round(endPoint.y
              - headLength * sin - arrowWidthAtHead * cos));
        hotArea.addPoint((int) Math.round(endPoint.x - headLength * cos
              + headWidth * sin), (int) Math.round(endPoint.y - headLength
              * sin - headWidth * cos));
        hotArea.addPoint(endPoint.x, endPoint.y);
        hotArea.addPoint((int) Math.round(endPoint.x - headLength * cos
              - headWidth * sin), (int) Math.round(endPoint.y - headLength
              * sin + headWidth * cos));
        hotArea.addPoint((int) Math.round(endPoint.x - headLength * cos
              - arrowWidthAtHead * sin), (int) Math.round(endPoint.y
              - headLength * sin + arrowWidthAtHead * cos));
        hotArea.addPoint((int) Math.round(startPoint.x - tailWidth * sin
              - tailLength * cos), (int) Math.round(startPoint.y + tailWidth
              * cos - tailLength * sin));
    }

    @Override
    public boolean contains(int x, int y) {
        return hotArea.contains(x, y);
    }

    @Override
    public boolean contains(Point p) {
        return hotArea.contains(p);
    }

    @Override
    public boolean contains(double x, double y) {
        return hotArea.contains((int) Math.round(x), (int) Math.round(y));
    }

    @Override
    public void translate(int deltaX, int deltaY) {
        super.translate(deltaX, deltaY);
        hotArea.translate(deltaX, deltaY);
    }

}
