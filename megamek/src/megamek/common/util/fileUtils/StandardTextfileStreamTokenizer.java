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

public final class StandardTextfileStreamTokenizer extends StreamTokenizer {

    public static final String INCLUDE_KEY = "include";
    private static final int DOUBLE_QUOTE = '"';

    private boolean isFinished = false;

    public StandardTextfileStreamTokenizer(Reader r) {
        super(r);
        eolIsSignificant(true);
        commentChar('#');
        quoteChar(DOUBLE_QUOTE);
        wordChars('_', '_');
    }

    public StandardTextfileStreamTokenizer(InputStream is) {
        super(is);
        eolIsSignificant(true);
        commentChar('#');
        quoteChar(DOUBLE_QUOTE);
        wordChars('_', '_');
    }

    public static boolean isValidIncludeLine(List<String> lineTokens) {
        return (lineTokens.size() == 2) && (lineTokens.get(0).equals(INCLUDE_KEY));
    }

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

    public boolean isFinished() {
        return isFinished;
    }
}
