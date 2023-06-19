/*
* MegaMek - Copyright (C) 2020 - The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/
package megamek.client.ui.swing.boardview;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.common.PlanetaryConditions;
import megamek.common.preference.PreferenceChangeEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * An overlay for the Boardview that displays a selection of Planetary Conditions
 * for the current game situation
 */
public class PlanetaryConditionsOverlay extends AbstractBoardViewOverlay {
    private static final String MSG_TEMPERATURE = Messages.getString("PlanetaryConditionsOverlay.Temperature");
    private static final String MSG_GRAVITY = Messages.getString("PlanetaryConditionsOverlay.Gravity");
    private static final String MSG_LIGHT = Messages.getString("PlanetaryConditionsOverlay.Light");
    private static final String MSG_ATMOSPHERICPREASSURE = Messages.getString("PlanetaryConditionsOverlay.AtmosphericPressure");
    private static final String MSG_EMI = Messages.getString("PlanetaryConditionsOverlay.EMI");
    private static final String MSG_WEATHER = Messages.getString("PlanetaryConditionsOverlay.Weather");
    private static final String MSG_WIND = Messages.getString("PlanetaryConditionsOverlay.Wind");
    private static final String MSG_DIRECTION = Messages.getString("PlanetaryConditionsOverlay.WindDirection");
    private static final String MSG_FOG = Messages.getString("PlanetaryConditionsOverlay.Fog");
    private static final String MSG_BLOWINGSAND = Messages.getString("PlanetaryConditionsOverlay.BlowingSand");

    /**
     * An overlay for the Boardview that displays a selection of Planetary Conditions
     * for the current game situation.
     */
    public PlanetaryConditionsOverlay(BoardView boardView) {
        super(boardView, new Font("SansSerif", Font.PLAIN, 13));
    }

    @Override
    protected String getHeaderText() {
        return Messages.getString("PlanetaryConditionsOverlay.heading",  KeyCommandBind.getDesc(KeyCommandBind.PLANETARY_CONDITIONS));
    }

    /** @return an ArrayList of all text lines to be shown. */
    @Override
    protected List<String> assembleTextLines() {
        List<String> result = new ArrayList<>();
        addHeader(result);
        Color colorHot = GUIP.getPlanetaryConditionsColorHot();
        Color colorCold = GUIP.getPlanetaryConditionsColorCold();

        if (clientGui != null && !currentGame.getBoard().inSpace()) {
            // In a game, not the Board Editor

            String tempColor = "";
            int temp = currentGame.getPlanetaryConditions().getTemperature();

            if (currentGame.getPlanetaryConditions().isExtremeTemperatureHeat()) {
                tempColor = colorToHex(colorHot);
            } else if (currentGame.getPlanetaryConditions().isExtremeTemperatureCold()) {
                tempColor = colorToHex(colorCold);
            }

            boolean showDefaultConditions = GUIP.getPlanetaryConditionsShowDefaults();

            Boolean showLabel = GUIP.getPlanetaryConditionsShowLabels();
            Boolean showValue = GUIP.getPlanetaryConditionsShowValues();
            Boolean showIndicator = GUIP.getPlanetaryConditionsShowIndicators();

            String tmpStr;

            if (showDefaultConditions || (currentGame.getPlanetaryConditions().isExtremeTemperature())) {
                tmpStr = (showLabel ? MSG_TEMPERATURE + "  " : "");
                tmpStr = tmpStr + (showValue ? temp + "\u00B0C  " : "");
                tmpStr = tmpStr + (showIndicator ? (!showValue ? temp + "\u00B0C   " : "" ) + currentGame.getPlanetaryConditions().getTemperatureIndicator() : "");
                result.add(tempColor + tmpStr);
            }

            if (showDefaultConditions || (currentGame.getPlanetaryConditions().getGravity() != 1.0)) {
                float grav = currentGame.getPlanetaryConditions().getGravity();
                tmpStr = (showLabel ? MSG_GRAVITY + "  " : "");
                tmpStr = tmpStr + (showValue ?  grav + "g   " : "");
                tmpStr = tmpStr + (showIndicator ? (!showValue ? grav + "g  " : "") + currentGame.getPlanetaryConditions().getGravityIndicator() : "");
                result.add(tmpStr);
            }

            if (showDefaultConditions || (currentGame.getPlanetaryConditions().getLight() != PlanetaryConditions.L_DAY)) {
                tmpStr = (showLabel ? MSG_LIGHT + "  " : "");
                tmpStr = tmpStr + (showValue ? currentGame.getPlanetaryConditions().getLightDisplayableName() + "  " : "");
                tmpStr = tmpStr + (showIndicator ? currentGame.getPlanetaryConditions().getLightIndicator() : "");
                result.add(tmpStr);
            }

            if (showDefaultConditions || (currentGame.getPlanetaryConditions().getAtmosphere() != PlanetaryConditions.ATMO_STANDARD)) {
                tmpStr = (showLabel ? MSG_ATMOSPHERICPREASSURE + "  " : "");
                tmpStr = tmpStr + (showValue ? currentGame.getPlanetaryConditions().getAtmosphereDisplayableName() + "  " : "");
                tmpStr = tmpStr + (showIndicator ? currentGame.getPlanetaryConditions().getAtmosphereIndicator() : "");
                result.add(tmpStr);
            }

            if (showDefaultConditions || (currentGame.getPlanetaryConditions().hasEMI())) {
                tmpStr = (showLabel ? MSG_EMI + "  " : "");
                tmpStr = tmpStr + (showValue ? currentGame.getPlanetaryConditions().getEMIDisplayableValue() + "  " : "");
                tmpStr = tmpStr + (showIndicator ? currentGame.getPlanetaryConditions().getEMIIndicator() : "");
                result.add(tmpStr);
            }

            if (showDefaultConditions || (currentGame.getPlanetaryConditions().getWeather() != PlanetaryConditions.WE_NONE)) {
                tmpStr = (showLabel ? MSG_WEATHER + "  " : "");
                tmpStr = tmpStr + (showValue ? currentGame.getPlanetaryConditions().getWeatherDisplayableName() + "  " : "");
                tmpStr = tmpStr + (showIndicator ? currentGame.getPlanetaryConditions().getWeatherIndicator() : "");
                result.add(tmpStr);
            }

            if (showDefaultConditions || (currentGame.getPlanetaryConditions().getWindStrength() != PlanetaryConditions.WI_NONE)) {
                tmpStr = (showLabel ? MSG_WIND + "  " : "");
                tmpStr = tmpStr + (showValue ? currentGame.getPlanetaryConditions().getWindDisplayableName() + "  " : "");
                tmpStr = tmpStr + (showIndicator ? currentGame.getPlanetaryConditions().getWindStrengthIndicator() : "");
                result.add(tmpStr);
                tmpStr = (showLabel ? MSG_DIRECTION + "  " : "");
                tmpStr = tmpStr + (showValue ? currentGame.getPlanetaryConditions().getWindDirDisplayableName() + "  " : "");
                tmpStr = tmpStr + (showIndicator ? currentGame.getPlanetaryConditions().getWindDirectionIndicator() : "");
                result.add(tmpStr);
            }

            if (showDefaultConditions || (currentGame.getPlanetaryConditions().getFog() != PlanetaryConditions.FOG_NONE)) {
                tmpStr = (showLabel ? MSG_FOG + "  " : "");
                tmpStr = tmpStr + (showValue ? currentGame.getPlanetaryConditions().getFogDisplayableName() + "  " : "");
                tmpStr = tmpStr + (showIndicator ? currentGame.getPlanetaryConditions().getFogIndicator() : "");
                result.add(tmpStr);
            }

            if (showDefaultConditions || (currentGame.getPlanetaryConditions().isSandBlowing())) {
                tmpStr = (showLabel ? MSG_BLOWINGSAND + "  " : "");
                tmpStr = tmpStr + (showValue ? currentGame.getPlanetaryConditions().getSandBlowingDisplayableValue() + "  " : "");
                tmpStr = tmpStr + (showIndicator ? currentGame.getPlanetaryConditions().getSandBlowingIndicator() : "");
                result.add(tmpStr);
            }
        }

        return result;
    }

    @Override
    protected boolean getVisibilityGUIPreference() {
        return GUIP.getShowPlanetaryConditionsOverlay();
    }

    @Override
    protected int getDistTop(Rectangle clipBounds,  int overlayHeight) {
        return 30;
    }

    @Override
    protected int getDistSide(Rectangle clipBounds, int overlayWidth) {
        return clipBounds.width - (overlayWidth + 100);
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        if (e.getName().equals(GUIPreferences.SHOW_PLANETARYCONDITIONS_OVERLAY)) {
            setVisible((boolean) e.getNewValue());
            scheduleBoardViewRepaint();
        }
        super.preferenceChange(e);
    }
}