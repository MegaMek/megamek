/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * 
 *  This program is free software; you can redistribute it and/or modify it 
 *  under the terms of the GNU General Public License as published by the Free 
 *  Software Foundation; either version 2 of the License, or (at your option) 
 *  any later version.
 * 
 *  This program is distributed in the hope that it will be useful, but 
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 *  for more details.
 */

package megamek.client.util;

import java.awt.*;
import java.util.*;

/*
 * Author: Ryan McConnell (oscarmm)
 */

public class AdvancedLabel extends Component {

    private Vector stringVector = new Vector();
    private Color[] colorArray;

    private int lines = 0;

    private int lineHeight;
    private int maxLineWidth;
    private int ascent;
    private int descent;

    private boolean sized = false;

    final private int leftMargin = 2;
    final private int rightMargin = 2;

    public AdvancedLabel(String text) {
        this(text, null);
        colorArray = new Color[lines];
        for (int i = 0; i < lines; i++) {
            colorArray[i] = getForeground();
        }
    }

    public AdvancedLabel(String text, Color[] lineColors) {
        setText(text);
        colorArray = lineColors;
    }

    public void paint(Graphics g) {
        getSizes();
        for (int i = 0; i < stringVector.size(); i++) {
            g.setColor(colorArray[i]);
            g.drawString((String) stringVector.elementAt(i), leftMargin, lineHeight * (i + 1));
        }
    }

    public void setText(String text) {
        lines=0;
        StringTokenizer st = new StringTokenizer(text, "\n");
        while (st.hasMoreTokens()) {
            stringVector.addElement(st.nextToken());
            lines++;
        }
    }

    /*
     * Sets important dimension properties for our label.
     */
    private void getSizes() {
        FontMetrics fm = getFontMetrics(getFont());
        lineHeight = (int) fm.getHeight();
        for (int i = 0; i < stringVector.size(); i++) {
            maxLineWidth = Math.max(maxLineWidth, (int) fm.stringWidth((String) stringVector.elementAt(i)));
        }
        ascent = (int) fm.getAscent();
        descent = (int) fm.getDescent();
        sized = true;
    }

    public Dimension getPreferredSize() {
        return getMinimumSize();
    }

    public Dimension getMinimumSize() {
        if (!sized) {
            getSizes();
        }
        int totalWidth = maxLineWidth + leftMargin + rightMargin;
        int totalHeight = lines * lineHeight + lines * descent;
        Dimension d = new Dimension(totalWidth, totalHeight);
        return d;
    }
}
