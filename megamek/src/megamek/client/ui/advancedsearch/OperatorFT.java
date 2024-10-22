package megamek.client.ui.advancedsearch;

import megamek.common.MekSearchFilter;

/**
 * FilterTokens subclass that represents a boolean operation.
 *
 * @author Arlith
 */
public abstract class OperatorFT implements FilterToken {

    public MekSearchFilter.BoolOp op;

    protected OperatorFT(MekSearchFilter.BoolOp o) {
        op = o;
    }

    @Override
    public String toString() {
        return switch (op) {
            case AND -> "and";
            case OR -> "or";
            default -> "";
        };
    }
}
