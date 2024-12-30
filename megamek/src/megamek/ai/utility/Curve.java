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
    @JsonSubTypes.Type(value = ParabolicCurve.class, name = "ParabolicCurve"),
    @JsonSubTypes.Type(value = BandPassCurve.class, name = "BandPassCurve"),
    @JsonSubTypes.Type(value = BandFilterCurve.class, name = "BandFilterCurve"),
})
public interface Curve {
    double evaluate(double x);

    Curve copy();

    default void drawAxes(Graphics g, int width, int height) {
        // Draw axis labels (0 to 1 with 0.05 increments)
        int padding = 10; // Padding for text from the axis lines
        double increment = 0.05;

        // Draw Y-axis labels
        for (double y = 0.0; y <= 1.0; y += increment) {
            int yPos = (int) (height - (y * height)); // Map 0-1 range to pixel coordinates
            g.setColor(Color.BLACK);
            g.drawLine(0, yPos, width, yPos);   // X-axis

            g.setColor(Color.WHITE);
            g.drawString(String.format("%.2f", y), padding, yPos + 5); // Label

        }

        // Draw X-axis labels
        for (double x = 0.0; x <= 1.0; x += increment) {
            int xPos = (int) (x * width); // Map 0-1 range to pixel coordinates
            g.setColor(Color.BLACK);
            g.drawLine(xPos, 0, xPos, height);   // Y-axis

            g.setColor(Color.WHITE);
            g.drawString(String.format("%.2f", x), xPos - 10, height); // Label
        }



        // Restore color
        g.setColor(Color.BLACK);
    }

    default void drawPoint(Graphics g,  int width, int height, Color color, double xPosNormalized) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(1));
        double y = this.evaluate(xPosNormalized);

        int px1 = (int)(xPosNormalized * width);
        int py1 = 0;

        g2d.drawLine(px1, py1, px1, height);
        g2d.drawString(
            String.format("Input: %.2f, Eval: %.2f", xPosNormalized, y),
            40, 20 // Position of the text
        );
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

    default double getM() {
        return 0.0;
    }

    default double getB() {
        return 0.0;
    }

    default double getK() {
        return 0.0;
    }

    default double getC() {
        return 0.0;
    }
}
