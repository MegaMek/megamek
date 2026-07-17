/*
 * Copyright (C) 2006 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2006-2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.client.ui.dialogs.randomArmy;

import megamek.client.ui.Messages;
import megamek.client.ui.models.UnitTableModel;
import megamek.client.ui.panels.phaseDisplay.lobby.LobbyUtility;
import megamek.client.ui.util.LambdaAction;
import megamek.common.loaders.MekSummary;
import megamek.common.units.Entity;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.TableRowSorter;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;

public class RandomArmyUnitTable extends JTable {

    protected TableRowSorter<UnitTableModel> unitsSorter;
    private final JFrame parentFrame;

    public RandomArmyUnitTable(UnitTableModel model) {
        super(model);
        unitsSorter = new TableRowSorter<>(model);
        this.parentFrame = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, this);
        setRowSorter(unitsSorter);
        setIntercellSpacing(new Dimension(0, 0));
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        addMouseListener(new RandomArmyTableMouseAdapter());
    }

    private final Action viewAction = new LambdaAction(Messages.getString("RandomArmyDialog.View"),
          this::viewReadout);
    private final Action bvAction = new LambdaAction(Messages.getString("RandomArmyDialog.ViewBV"),
          this::viewBv);
    private final Action costAction = new LambdaAction(Messages.getString("RandomArmyDialog.ViewCost"),
          this::viewCost);

    private void viewReadout(ActionEvent event) {
        LobbyUtility.mekReadoutAction(loadSelectedEntities(), true, true, parentFrame);
    }

    private void viewBv(ActionEvent event) {
        LobbyUtility.mekBVAction(loadSelectedEntities(), true, true, parentFrame);
    }

    private void viewCost(ActionEvent event) {
        LobbyUtility.mekCostAction(loadSelectedEntities(), true, true, parentFrame);
    }

    private List<Entity> loadSelectedEntities() {
        return LobbyUtility.getSelectedEntities(this).stream()
              .map(row -> ((UnitTableModel) this.getModel()).getUnitAt(row))
              .map(MekSummary::loadEntity)
              .filter(Objects::nonNull)
              .toList();
    }

    public class RandomArmyTableMouseAdapter extends MouseInputAdapter {
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopup(e);
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopup(e);
            }
        }
    }

    private void showPopup(MouseEvent e) {
        var popup = new JPopupMenu();
        popup.add(viewAction);
        popup.add(bvAction);
        popup.add(costAction);
        popup.show(e.getComponent(), e.getX(), e.getY());
    }
}
