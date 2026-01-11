package megamek.client.ui.dialogs.randomArmy;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * This Random Army Dialog is shown in MM's main menu when "Reinforce from RAT" is selected. It allows generating armies
 * but they cannot be added anywhere.
 */
public class MMMainMenuRandomArmyDialog extends AbstractRandomArmyDialog {

    public MMMainMenuRandomArmyDialog(JFrame parent) {
        super(parent);
    }

    @Override
    protected JComponent createButtonsPanel() {
        return new JPanel();
    }
}
