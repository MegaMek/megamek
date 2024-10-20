package megamek.client.ui.advancedsearch;

/**
 * FilterTokens subclass that represents parenthesis.
 *
 * @author Arlith
 */
public class ParensFT extends FilterTokens {
    public String parens;

    public ParensFT(String p) {
        parens = p;
    }

    @Override
    public String toString() {
        return parens;
    }
}
