package megamek.client.ui.advancedsearch;

/**
 * FilterTokens subclass that represents equipment.
 *
 * @author Arlith
 */
public class EquipmentFT extends FilterTokens {
    public String internalName;
    public String fullName;
    public int qty;

    public EquipmentFT(String in, String fn, int q) {
        internalName = in;
        fullName = fn;
        qty = q;
    }

    @Override
    public String toString() {
        return qty + " " + fullName + ((qty != 1) ? "s" : "");
    }
}
