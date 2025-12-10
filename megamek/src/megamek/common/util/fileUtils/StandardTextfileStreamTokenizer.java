/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.util.fileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a specialized StreamTokenizer that uses a configuration that many of MM's text files share. This
 * configuration uses # for comments, double quotes for strings and information is processed line-wise. This class adds
 * some methods to simplify line parsing.
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
     * Returns true when the line given as its tokens is a valid line giving an include file. The tokens should be
     * obtained from {@link #getLineTokens()}. A valid include line has exactly two tokens, the first being "include"
     * and the second a filename.
     *
     * @param lineTokens The tokens representing an input streamline
     *
     * @return True when the tokens represent a valid include statement
     *
     * @see #getLineTokens()
     * @see #INCLUDE_KEY
     */
    public static boolean isValidIncludeLine(List<String> lineTokens) {
        return (lineTokens.size() == 2) && (lineTokens.get(0).equals(INCLUDE_KEY));
    }

    /**
     * Returns a list of tokens making up the next available input streamline. Empty lines are skipped, i.e. the tokens
     * list will normally not be empty. It may be empty when the end of the input stream is reached. Use
     * {@link #isFinished()} to test if the stream is at its end.
     *
     * @return The tokens of the next available line that is not empty
     *
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
     * @return True when the end of the input stream is reached. When this method returns true, the tokens list returned
     *       by {@link #getLineTokens()} is empty. The finished state is updated in {@link #getLineTokens()}.
     */
    public boolean isFinished() {
        return isFinished;
    }
}
