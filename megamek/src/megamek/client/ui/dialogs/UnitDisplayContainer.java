package megamek.client.ui.dialogs;

import megamek.client.ui.swing.unitDisplay.UnitDisplay;

import javax.swing.JDialog;

public interface UnitDisplayContainer {
    UnitDisplay getUnitDisplay();
    JDialog getDialog();
}
