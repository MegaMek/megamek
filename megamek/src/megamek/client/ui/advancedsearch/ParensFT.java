package megamek.client.ui.advancedsearch;

/**
 * FilterTokens subclass that represents parenthesis.
 *
 * @author Arlith
 */
public abstract class ParensFT implements FilterToken {
    public String parens;

    public ParensFT(String p) {
        parens = p;
    }

    @Override
    public String toString() {
        return parens;
    }
}
