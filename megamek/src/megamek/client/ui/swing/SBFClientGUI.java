package megamek.client.ui.swing;

import megamek.client.Client;
import megamek.client.ui.swing.util.MegaMekController;

import javax.swing.*;

public class SBFClientGUI extends AbstractClientGUI {

    public SBFClientGUI(Client client, MegaMekController c) {
        frame.getContentPane().add(new JLabel("SBF games are currently under construction!"));
    }

    @Override
    public void initialize() {

    }

    @Override
    public void die() {

    }

    @Override
    protected boolean saveGame() {
        //TODO
        return true;
    }
}
