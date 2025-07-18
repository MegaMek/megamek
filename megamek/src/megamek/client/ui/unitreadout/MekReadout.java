/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.unitreadout;

import megamek.client.ui.Messages;
import megamek.client.ui.util.ViewFormatting;
import megamek.common.EquipmentType;
import megamek.common.LandAirMek;
import megamek.common.Mek;
import megamek.common.QuadVee;

import java.util.ArrayList;
import java.util.List;

class MekReadout extends GeneralEntityReadout2 {

    private final Mek mek;

    protected MekReadout(Mek mek, boolean showDetail, boolean useAlternateCost, boolean ignorePilotBV,
          ViewFormatting formatting) {

        super(mek, showDetail, useAlternateCost, ignorePilotBV, formatting);
        this.mek = mek;
    }

    @Override
    protected ViewElement createTotalInternalElement() {
        String internal = mek.getTotalInternal()
              + Messages.getString("MekView." + EquipmentType.getStructureTypeName(mek.getStructureType()));
        return new LabeledElement(Messages.getString("MekView.Internal"), internal);
    }

    @Override
    protected List<ViewElement> createSystemsElements() {
        List<ViewElement> result = new ArrayList<>();
        StringBuilder hsString = new StringBuilder();
        hsString.append(mek.heatSinks());
        if (!mek.formatHeat().equals(Integer.toString(mek.heatSinks()))) {
            hsString.append(" [").append(mek.formatHeat()).append("]");
        }
        if (mek.hasRiscHeatSinkOverrideKit()) {
            hsString.append(" w/ RISC Heat Sink Override Kit");
        }
        if (mek.damagedHeatSinks() > 0) {
            hsString.append(" ").append(ViewElement.warningStart(formatting)).append("(")
                  .append(mek.damagedHeatSinks())
                  .append(" damaged)").append(ViewElement.warningEnd(formatting));
        }
        result.add(new LabeledElement(mek.getHeatSinkTypeName() + "s", hsString.toString()));
        result.add(new LabeledElement(Messages.getString("MekView.Cockpit"),
              mek.getCockpitTypeString()
                    + (mek.hasArmoredCockpit() ? " (armored)" : "")));

        String gyroString = mek.getGyroTypeString();
        if (mek.getGyroHits() > 0) {
            gyroString += " " + ViewElement.warningStart(formatting) + "(" + mek.getGyroHits()
                  + " hits)" + ViewElement.warningEnd(formatting);
        }
        if (mek.hasArmoredGyro()) {
            gyroString += " (armored)";
        }
        result.add(new LabeledElement(Messages.getString("MekView.Gyro"), gyroString));
        return result;
    }

    @Override
    protected List<ViewElement> createConversionModeMovementElements() {
        List<ViewElement> result = new ArrayList<>();

        if (mek instanceof QuadVee) {
            mek.setConversionMode(QuadVee.CONV_MODE_VEHICLE);
            result.add(new LabeledElement(Messages.getString("MovementType." + mek.getMovementModeAsString()),
                  mek.getWalkMP() + "/" + mek.getRunMPasString()));

        } else if (mek instanceof LandAirMek lam) {
            if (lam.getLAMType() == LandAirMek.LAM_STANDARD) {
                result.add(new LabeledElement(Messages.getString("MovementType.AirMek"),
                      lam.getAirMekWalkMP() + "/"
                            + lam.getAirMekRunMP() + "/"
                            + lam.getAirMekCruiseMP() + "/"
                            + lam.getAirMekFlankMP()));
            }

            mek.setConversionMode(LandAirMek.CONV_MODE_FIGHTER);
            result.add(new LabeledElement(Messages.getString("MovementType.Fighter"),
                  mek.getWalkMP() + "/" + mek.getRunMP()));
        }
        return result;
    }
}
