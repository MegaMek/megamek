package megamek.client.ui.advancedsearch;

import megamek.common.MekSearchFilter;

/**
 * FilterTokens subclass that represents a boolean operation.
 *
 * @author Arlith
 */
public class OperationFT extends FilterTokens {
    public MekSearchFilter.BoolOp op;

    public OperationFT(MekSearchFilter.BoolOp o) {
        op = o;
    }

    @Override
    public String toString() {
        if (op == MekSearchFilter.BoolOp.AND) {
            return "And";
        } else if (op == MekSearchFilter.BoolOp.OR) {
            return "Or";
        } else {
            return "";
        }
    }
}
