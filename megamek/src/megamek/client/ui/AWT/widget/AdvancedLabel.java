/*
 * MegaMek - Copyright (C) 2000,2001,2002,2004 Ben Mazur (bmazur@sev.org)
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

package megamek.client.ui.AWT.widget;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.StringTokenizer;
import java.util.Vector;

/*
 * Author: Ryan McConnell (oscarmm)
 */

public class AdvancedLabel extends Component {

    /**
     *
     */
    private static final long serialVersionUID = -3424428063742960330L;
    private Vector<String> stringVector = new Vector<String>();
    private Color[] colorArray;

    private int lineHeight;
    private int maxLineWidth;
    private int descent;

    private boolean sized = false;

    private static final int leftMargin = 2;
    private static final int rightMargin = 2;

    public AdvancedLabel(String text) {
        colorArray = null;
        setText(text);
    }

    public AdvancedLabel(String text, Color[] lineColors) {
        colorArray = lineColors;
        setText(text);
    }

    @Override
    public void paint(Graphics g) {
        getSizes();
        for (int i = 0; i < stringVector.size(); i++) {
            if (colorArray != null) {
                g.setColor(colorArray[i]);
            }
            g.drawString(stringVector.elementAt(i), leftMargin, lineHeight
                    * (i + 1));
        }
    }

    public void setText(String text) {
        stringVector.removeAllElements();
        StringTokenizer st = new StringTokenizer(text, "\r\n"); //$NON-NLS-1$
        while (st.hasMoreTokens()) {
            stringVector.addElement(st.nextToken());
        }
    }

    /**
     * Sets important dimension properties for our label.
     */
    private void getSizes() {
        FontMetrics fm = getFontMetrics(getFont());
        lineHeight = fm.getHeight();
        for (int i = 0; i < stringVector.size(); i++) {
            maxLineWidth = Math.max(maxLineWidth, fm.stringWidth(stringVector
                    .elementAt(i)));
        }
        // ascent = fm.getAscent(); //ascent is never used so I removed it.
        descent = fm.getDescent();
        sized = true;
    }

    @Override
    public Dimension getPreferredSize() {
        return getMinimumSize();
    }

    @Override
    public Dimension getMinimumSize() {
        if (!sized) {
            getSizes();
        }
        int totalWidth = maxLineWidth + leftMargin + rightMargin;
        int totalHeight = stringVector.size() * lineHeight
        + stringVector.size() * descent;
        Dimension d = new Dimension(totalWidth, totalHeight);
        return d;
    }
}
