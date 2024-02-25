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
import megamek.common.enums.Light;
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
            PlanetaryConditions conditions = currentGame.getPlanetaryConditions();
            int temp = conditions.getTemperature();

            if (conditions.isExtremeTemperatureHeat()) {
                tempColor = colorToHex(colorHot);
            } else if (conditions.isExtremeTemperatureCold()) {
                tempColor = colorToHex(colorCold);
            }

            boolean showDefaultConditions = GUIP.getPlanetaryConditionsShowDefaults();

            Boolean showLabel = GUIP.getPlanetaryConditionsShowLabels();
            Boolean showValue = GUIP.getPlanetaryConditionsShowValues();
            Boolean showIndicator = GUIP.getPlanetaryConditionsShowIndicators();

            String tmpStr;

            if (showDefaultConditions || (conditions.isExtremeTemperature())) {
                tmpStr = (showLabel ? MSG_TEMPERATURE + "  " : "");
                tmpStr = tmpStr + (showValue ? temp + "\u00B0C  " : "");
                tmpStr = tmpStr + (showIndicator ? (!showValue ? temp + "\u00B0C   " : "" ) + conditions.getTemperatureIndicator() : "");
                result.add(tempColor + tmpStr);
            }

            if (showDefaultConditions || (conditions.getGravity() != 1.0)) {
                float grav = conditions.getGravity();
                tmpStr = (showLabel ? MSG_GRAVITY + "  " : "");
                tmpStr = tmpStr + (showValue ?  grav + "g   " : "");
                tmpStr = tmpStr + (showIndicator ? (!showValue ? grav + "g  " : "") + conditions.getGravityIndicator() : "");
                result.add(tmpStr);
            }

            Light light = conditions.getLight();
            if (showDefaultConditions || !conditions.isDay()) {
                tmpStr = (showLabel ? MSG_LIGHT + "  " : "");
                tmpStr = tmpStr + (showValue ? light.toString() + "  " : "");
                tmpStr = tmpStr + (showIndicator ? light.getIndicator() : "");
                result.add(tmpStr);
            }

            if (showDefaultConditions || conditions.isStandard()) {
                tmpStr = (showLabel ? MSG_ATMOSPHERICPREASSURE + "  " : "");
                tmpStr = tmpStr + (showValue ? conditions.getAtmosphere().toString() + "  " : "");
                tmpStr = tmpStr + (showIndicator ? conditions.getAtmosphere().getIndicator() : "");
                result.add(tmpStr);
            }

            if (showDefaultConditions || (conditions.hasEMI())) {
                tmpStr = (showLabel ? MSG_EMI + "  " : "");
                tmpStr = tmpStr + (showValue ? conditions.getEMIDisplayableValue() + "  " : "");
                tmpStr = tmpStr + (showIndicator ? conditions.getEMIIndicator() : "");
                result.add(tmpStr);
            }

            if (showDefaultConditions || !conditions.isWeatherNone()) {
                tmpStr = (showLabel ? MSG_WEATHER + "  " : "");
                tmpStr = tmpStr + (showValue ? conditions.getWeather().toString() + "  " : "");
                tmpStr = tmpStr + (showIndicator ? conditions.getWeather().getIndicator() : "");
                result.add(tmpStr);
            }

            if (showDefaultConditions || !conditions.isCalm()) {
                tmpStr = (showLabel ? MSG_WIND + "  " : "");
                tmpStr = tmpStr + (showValue ? conditions.getWind().toString() + "  " : "");
                tmpStr = tmpStr + (showIndicator ? conditions.getWind().getIndicator() : "");
                result.add(tmpStr);
                tmpStr = (showLabel ? MSG_DIRECTION + "  " : "");
                tmpStr = tmpStr + (showValue ? conditions.getWindDirection().toString() + "  " : "");
                tmpStr = tmpStr + (showIndicator ? conditions.getWindDirection().getIndicator() : "");
                result.add(tmpStr);
            }

            if (showDefaultConditions || !conditions.isFogNone()) {
                tmpStr = (showLabel ? MSG_FOG + "  " : "");
                tmpStr = tmpStr + (showValue ? conditions.getFog().toString() + "  " : "");
                tmpStr = tmpStr + (showIndicator ? conditions.getFog().getIndicator() : "");
                result.add(tmpStr);
            }

            if (showDefaultConditions || conditions.isBlowingSand()) {
                tmpStr = (showLabel ? MSG_BLOWINGSAND + "  " : "");
                tmpStr = tmpStr + (showValue ? conditions.getBlowingSand().toString() + "  " : "");
                tmpStr = tmpStr + (showIndicator ? conditions.getBlowingSand().getIndicator() : "");
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