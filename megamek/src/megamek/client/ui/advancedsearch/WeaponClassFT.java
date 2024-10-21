package megamek.client.ui.advancedsearch;

public class WeaponClassFT extends EquipmentFilterToken {
    public WeaponClass weaponClass;
    public int qty;

    public WeaponClassFT(WeaponClass in_class, int in_qty) {
        weaponClass = in_class;
        qty = in_qty;
    }

    @Override
    public String toString() {
        if (qty == 1) {
            return qty + " " + weaponClass.toString();
        } else {
            return qty + " " + weaponClass.toString() + "s";
        }
    }
}
