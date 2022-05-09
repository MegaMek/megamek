package megamek.client.ui.dialogs;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.unitDisplay.UnitDisplay;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class UnitDisplayDialog extends JDialog {
    //region Variable Declarations
    private UnitDisplay unitDisplay;

    private final ClientGUI clientGUI;
    //endregion Variable Declarations

    //region Constructors
    public UnitDisplayDialog(final JFrame frame, final UnitDisplay unitDisplay,
                             final ClientGUI clientGUI) {
        super(frame, Messages.getString("ClientGUI.MechDisplay"), false);
        setUnitDisplay(unitDisplay);
        this.clientGUI = clientGUI;
    }
    //endregion Constructors

    //region Getters/Setters
    public UnitDisplay getUnitDisplay() {
        return unitDisplay;
    }

    public void setUnitDisplay(final UnitDisplay unitDisplay) {
        this.unitDisplay = unitDisplay;
    }
    //endregion Getters/Setters

    /**
     * In addition to the default Dialog processKeyEvent, this method
     * dispatches a KeyEvent to the client gui.
     * This enables all of the gui hotkeys.
     */
    @Override
    protected void processKeyEvent(KeyEvent evt) {
        evt.setSource(clientGUI);
        clientGUI.getMenuBar().dispatchEvent(evt);
        // Make the source be the ClientGUI and not the dialog
        // This prevents a ClassCastException in ToolTipManager
        clientGUI.getCurrentPanel().dispatchEvent(evt);
        if (!evt.isConsumed()) {
            super.processKeyEvent(evt);
        }
    }
}
