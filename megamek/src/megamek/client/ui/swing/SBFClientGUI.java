/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing;

import megamek.client.Client;
import megamek.client.IClient;
import megamek.client.SBFClient;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.common.InGameObject;

import java.awt.*;

public class SBFClientGUI extends AbstractClientGUI {

    private final SBFClient client;

    public SBFClientGUI(IClient client, MegaMekController c) {
        if (!(client instanceof SBFClient)) {
            throw new IllegalArgumentException("SBF ClientGUI must use SBF Client!");
        }
        this.client = (SBFClient) client;
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(new UnderConstructionPanel(), BorderLayout.CENTER);
        frame.setVisible(true);
    }

    @Override
    protected boolean saveGame() {
        //TODO
        return true;
    }

    @Override
    public InGameObject getSelectedUnit() {
        return null;
    }
}
