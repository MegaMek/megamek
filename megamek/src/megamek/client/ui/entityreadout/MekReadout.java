/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.entityreadout;

import java.util.ArrayList;
import java.util.List;

import megamek.client.ui.Messages;
import megamek.common.EquipmentType;
import megamek.common.LandAirMek;
import megamek.common.Mek;
import megamek.common.QuadVee;

class MekReadout extends GeneralEntityReadout {

    private final Mek mek;

    protected MekReadout(Mek mek, boolean showDetail, boolean useAlternateCost, boolean ignorePilotBV) {

        super(mek, showDetail, useAlternateCost, ignorePilotBV);
        this.mek = mek;
    }

    @Override
    protected ViewElement createTotalInternalElement() {
        String internal = mek.getTotalInternal()
              + Messages.getString("MekView." + EquipmentType.getStructureTypeName(mek.getStructureType()));
        return new LabeledLine(Messages.getString("MekView.Internal"), internal);
    }

    @Override
    protected List<ViewElement> createSystemsElements() {
        List<ViewElement> result = new ArrayList<>();

        var joined = new JoinedViewElement();
        joined.add(mek.heatSinks());
        if (!mek.formatHeat().equals(Integer.toString(mek.heatSinks()))) {
            joined.add(" [%s]".formatted(mek.formatHeat()));
        }
        if (mek.hasRiscHeatSinkOverrideKit()) {
            joined.add(" w/ RISC Heat Sink Override Kit");
        }
        if (mek.damagedHeatSinks() > 0) {
            joined.add(new DamagedElement(" (%d damaged)".formatted(mek.damagedHeatSinks())));
        }
        result.add(new LabeledLine(mek.getHeatSinkTypeName() + "s", joined));

        result.add(new LabeledLine(Messages.getString("MekView.Cockpit"),
              mek.getCockpitTypeString()
                    + (mek.hasArmoredCockpit() ? " (armored)" : "")));

        JoinedViewElement gyro = new JoinedViewElement(mek.getGyroTypeString());
        result.add(new LabeledLine(Messages.getString("MekView.Gyro"), gyro));
        if (mek.getGyroHits() > 0) {
            gyro.add(new DamagedElement(" (%d hits)".formatted(mek.getGyroHits())));
        }
        if (mek.hasArmoredGyro()) {
            gyro.add(" (armored)");
        }

        return result;
    }

    @Override
    protected List<ViewElement> createConversionModeMovementElements() {
        List<ViewElement> result = new ArrayList<>();

        if (mek instanceof QuadVee) {
            mek.setConversionMode(QuadVee.CONV_MODE_VEHICLE);
            result.add(new LabeledLine(Messages.getString("MovementType." + mek.getMovementModeAsString()),
                  mek.getWalkMP() + "/" + mek.getRunMPasString()));

        } else if (mek instanceof LandAirMek lam) {
            if (lam.getLAMType() == LandAirMek.LAM_STANDARD) {
                result.add(new LabeledLine(Messages.getString("MovementType.AirMek"),
                      lam.getAirMekWalkMP() + "/"
                            + lam.getAirMekRunMP() + "/"
                            + lam.getAirMekCruiseMP() + "/"
                            + lam.getAirMekFlankMP()));
            }

            mek.setConversionMode(LandAirMek.CONV_MODE_FIGHTER);
            result.add(new LabeledLine(Messages.getString("MovementType.Fighter"),
                  mek.getWalkMP() + "/" + mek.getRunMP()));
        }
        return result;
    }
}
