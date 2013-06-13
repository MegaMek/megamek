/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.client.ui.AWT.util;

import java.awt.Color;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;

public class ColorParser {

    private static String[] colorNames = { "black", "blue", "cyan", "darkgray",
            "gray", "green", "lightgray", "magenta", "orange", "pink", "red",
            "white", "yellow" };

    private static Color[] colorValues = { Color.black, Color.blue, Color.cyan,
            Color.darkGray, Color.gray, Color.green, Color.lightGray,
            Color.magenta, Color.orange, Color.pink, Color.red, Color.white,
            Color.yellow };

    protected StreamTokenizer st;
    protected int currentToken;
    protected ParseException exception;
    protected Color color;

    public Color getColor() {
        return color;
    }

    public boolean parse(String color) {
        clear();
        StringReader s = new StringReader(color);
        st = new StreamTokenizer(s);
        return parse();
    }

    public boolean parse(StreamTokenizer st) {
        clear();
        this.st = st;
        return parse();
    }

    public ParseException getException() {
        return exception;
    }

    protected void clear() {
        color = null;
        exception = null;
        st = null;
    }

    protected boolean parse() {
        boolean hasErrors = false;
        nextToken();
        try {
            parseColor();
        } catch (ParseException e) {
            hasErrors = true;
            exception = e;
        }
        return hasErrors;
    }

    protected void parseColor() throws ParseException {
        if (currentToken == StreamTokenizer.TT_WORD) {
            String sName = st.sval;
            for (int x = 0; x < colorNames.length; x++) {
                if (colorNames[x].equalsIgnoreCase(sName)) {
                    color = colorValues[x];
                    return;
                }
            }
            throw new ParseException("Unrecognized color: " + sName);
        } else if (currentToken == StreamTokenizer.TT_NUMBER) {
            int red = (int) st.nval;
            nextToken();
            if (currentToken != StreamTokenizer.TT_NUMBER) {
                throw new ParseException("green color value expected");
            }
            int green = (int) st.nval;
            nextToken();
            if (currentToken != StreamTokenizer.TT_NUMBER) {
                throw new ParseException("blue color value expected");
            }
            int blue = (int) st.nval;
            nextToken();
            color = new Color(red, green, blue);
        } else {
            throw new ParseException(
                    "color name or integer read component value expected");
        }
    }

    protected void nextToken() {
        try {
            currentToken = st.nextToken();
        } catch (IOException e1) {
            currentToken = StreamTokenizer.TT_EOF;
        }
    }

    public static class ParseException extends Exception {
        /**
         * 
         */
        private static final long serialVersionUID = -4543806307959221311L;

        public ParseException(String message) {
            super(message);
        }
    }

}
