/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.phaseDisplay;

import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JRadioButton;

import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.dialogs.buttonDialogs.AbstractButtonDialog;
import megamek.client.ui.panels.FacingPickerPanel;
import megamek.codeUtilities.MathUtility;
import megamek.common.units.Entity;

/**
 * Picks the facing of a multi-hex building being placed (#7858): the six directions around the building's picture,
 * as a tank turret is turned, with the facings that would put a hex off the map greyed out. Being an
 * {@link AbstractButtonDialog} it can be resized and comes back where the player last left it.
 */
public class BuildingFacingDialog extends AbstractButtonDialog {

    /** Returned by {@link #getChosenFacing()} when nothing is selected. */
    public static final int NO_FACING = -1;

    private final ClientGUI clientgui;
    private final Entity building;
    private final Set<Integer> allowedFacings;
    private final List<JRadioButton> facings = new ArrayList<>();
    private final ButtonGroup buttonGroup = new ButtonGroup();

    /**
     * @param frame          the parent frame
     * @param clientgui      the client GUI, for the building's picture
     * @param building       the building being placed
     * @param allowedFacings the facings (0-5) whose whole footprint fits where the building stands
     */
    public BuildingFacingDialog(JFrame frame, ClientGUI clientgui, Entity building, Set<Integer> allowedFacings) {
        super(frame, "BuildingFacingDialog", "DeploymentDisplay.facingChoice");
        this.clientgui = clientgui;
        this.building = building;
        this.allowedFacings = allowedFacings;
        initialize();
    }

    @Override
    protected Container createCenterPane() {
        for (int facing = 0; facing <= 5; facing++) {
            JRadioButton button = new JRadioButton();
            button.setActionCommand(Integer.toString(facing));
            button.setEnabled(allowedFacings.contains(facing));
            button.setSelected(facing == building.getFacing());
            facings.add(button);
            buttonGroup.add(button);
        }
        return new FacingPickerPanel(facings, FacingPickerPanel.previewOnHex(clientgui, building, 0));
    }

    /**
     * @return the facing the player picked (0-5), or {@link #NO_FACING} when none is selected
     */
    public int getChosenFacing() {
        if (buttonGroup.getSelection() == null) {
            return NO_FACING;
        }
        return MathUtility.parseInt(buttonGroup.getSelection().getActionCommand(), NO_FACING);
    }
}
