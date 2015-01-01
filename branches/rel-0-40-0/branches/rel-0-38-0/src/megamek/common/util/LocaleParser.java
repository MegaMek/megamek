/*
 * MegaMek -
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.util;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;

public class LocaleParser {

    protected StreamTokenizer st;
    protected String language = "", country = "", variant = "";
    protected int currentToken;
    protected ParseException exception;

    public String getLanguage() {
        return language;
    }

    public String getCountry() {
        return country;
    }

    public String getVariant() {
        return variant;
    }

    public boolean parse(String locstring) {
        clear();
        StringReader s = new StringReader(locstring);
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
        language = "";
        country = "";
        variant = "";
        exception = null;
        st = null;
    }

    protected boolean parse() {
        boolean hasErrors = false;
        nextToken();
        try {
            parseLocale();
        } catch (ParseException e) {
            hasErrors = true;
            exception = e;
        }
        return hasErrors;
    }

    protected void parseLocale() throws ParseException {
        if (currentToken != StreamTokenizer.TT_WORD) {
            throw new ParseException("language expected");
        }
        language = st.sval;
        nextToken();
        if (currentToken == '_') {
            nextToken();
            parseCountry();
        }
    }

    protected void parseCountry() throws ParseException {
        if (currentToken != StreamTokenizer.TT_WORD) {
            throw new ParseException("country expected");
        }
        country = st.sval;
        nextToken();
        if (currentToken == '_') {
            parseVariant();
        }
    }

    protected void parseVariant() throws ParseException {
        if (currentToken != StreamTokenizer.TT_WORD) {
            throw new ParseException("variant expected");
        }
        variant = st.sval;
        nextToken();
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
        private static final long serialVersionUID = -8008986744358951377L;

        public ParseException(String message) {
            super(message);
        }
    }

}
