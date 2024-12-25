/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.ai.utility;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import javax.swing.*;
import java.awt.*;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "curveType" // The YAML will have something like "curveType: LinearCurve"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = LinearCurve.class, name = "LinearCurve"),
    @JsonSubTypes.Type(value = LogisticCurve.class, name = "LogisticCurve"),
    @JsonSubTypes.Type(value = LogitCurve.class, name = "LogitCurve"),
    @JsonSubTypes.Type(value = ParabolicCurve.class, name = "ParabolicCurve")
})
public interface Curve {
    double evaluate(double x);

    default void drawAxes(Graphics g, int width, int height) {
        // Center lines
        g.setColor(Color.LIGHT_GRAY);
        g.drawLine(0, height/2, width, height/2);   // X-axis
        g.drawLine(width/2, 0, width/2, height);   // Y-axis

        // Restore color to black
        g.setColor(Color.BLACK);
    }

    default void drawCurve(Graphics g, int width, int height, Color color) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(3));
        g2d.setColor(color);

        double step = 0.001;
        double xPrev = 0.0;
        double yPrev = this.evaluate(xPrev);

        for (double x = step; x <= 1.0; x += step) {
            double y = this.evaluate(x);

            int px1 = (int)(xPrev * width);
            int py1 = (int)((1.0 - yPrev) * height);

            int px2 = (int)(x * width);
            int py2 = (int)((1.0 - y) * height);

            g2d.drawLine(px1, py1, px2, py2);

            xPrev = x;
            yPrev = y;
        }
    }

    default void setM(double m) {
        //
    }

    default void setB(double b) {
        //
    }

    default void setK(double k) {
        //
    }

    default void setC(double c) {
        //
    }
}
