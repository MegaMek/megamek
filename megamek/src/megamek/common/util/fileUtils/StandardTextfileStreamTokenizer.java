/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.util.fileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a specialized StreamTokenizer that uses a configuration that many of MM's text files share.
 * This configuration uses # for comments, double quotes for strings and information
 * is processed line-wise. This class adds some methods to simplify line parsing.
 */
public final class StandardTextfileStreamTokenizer extends StreamTokenizer {

    public static final String INCLUDE_KEY = "include";
    private static final int DOUBLE_QUOTE = '"';

    private boolean isFinished = false;

    /**
     * Creates a StreamTokenizer for the given Reader.
     *
     * @param r The Reader to tokenize such as a {@link java.io.FileReader}
     */
    public StandardTextfileStreamTokenizer(Reader r) {
        super(r);
        eolIsSignificant(true);
        commentChar('#');
        quoteChar(DOUBLE_QUOTE);
        wordChars('_', '_');
    }

    /**
     * Creates a StreamTokenizer for the given InputStream.
     *
     * @param is The InputStream to tokenize
     */
    public StandardTextfileStreamTokenizer(InputStream is) {
        super(is);
        eolIsSignificant(true);
        commentChar('#');
        quoteChar(DOUBLE_QUOTE);
        wordChars('_', '_');
    }

    /**
     * Returns true when the line given as its tokens is a valid line giving an include file. The tokens
     * should be obtained from {@link #getLineTokens()}. A valid include line has exactly two tokens,
     * the first being "include" and the second a filename.
     *
     * @param lineTokens The tokens representing an input stream line
     * @return True when the tokens represent a valid include statement
     * @see #getLineTokens()
     * @see #INCLUDE_KEY
     */
    public static boolean isValidIncludeLine(List<String> lineTokens) {
        return (lineTokens.size() == 2) && (lineTokens.get(0).equals(INCLUDE_KEY));
    }

    /**
     * Returns a list of tokens making up the next available input stream line. Empty lines are skipped,
     * i.e. the tokens list will normally not be empty. It may be empty when the end of the input stream
     * is reached. Use {@link #isFinished()} to test if the stream is at its end.
     *
     * @return The tokens of the next available line that is not empty
     * @see #isFinished()
     */
    public List<String> getLineTokens() throws IOException {
        List<String> tokens = new ArrayList<>();
        while (true) {
            nextToken();
            if (ttype == StreamTokenizer.TT_NUMBER) {
                tokens.add(Double.toString(nval));
            } else if (ttype == StreamTokenizer.TT_EOF) {
                isFinished = tokens.isEmpty();
                return tokens;
            } else if (ttype == StreamTokenizer.TT_EOL) {
                if (!tokens.isEmpty()) {
                    // only return the current line if it isn't empty, otherwise simply skip it
                    return tokens;
                }
            } else { // TT_WORD or the quote character or a single character token
                tokens.add(sval);
            }
        }
    }

    /**
     * @return True when the end of the input stream is reached. When this method returns true, the tokens
     * list returned by {@link #getLineTokens()} is empty. The finished state is updated in
     * {@link #getLineTokens()}.
     */
    public boolean isFinished() {
        return isFinished;
    }
}
