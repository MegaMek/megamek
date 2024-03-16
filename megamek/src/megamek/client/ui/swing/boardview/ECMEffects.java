/*
 * MegaMek -
 * Copyright © 2015 Nicholas Walczak (walczak@cs.umn.edu)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing.boardview;

import megamek.client.ui.swing.GUIPreferences;
import megamek.common.ECMInfo;
import megamek.common.Player;
import megamek.common.annotations.Nullable;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * This class contains a collection of <code>ECMInfo</code> instances that all
 * effect a particular location.
 *
 * This is used by BoardView1 to keep track of what kindof E(C)CM is affecting
 * a particular Coords, and determine how to color a Hex based on that
 * information.
 *
 * @author arlith
 */
public class ECMEffects {
    /**
     * A collection of <code>ECMInfo</code> instances that affect a location.
     */
    protected LinkedList<ECMInfo> ecmEffects;

    /**
     * Flag that determines if the dominant effect for the location is ECCM.
     * This is set by the <code>getHexColor()</code> method.
     */
    protected boolean isECCM = false;

    ECMEffects() {
        ecmEffects = new LinkedList<>();
    }

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    /**
     * Added another ECMInfo to the effects for a location.
     * @param info
     */
    public void addECM(ECMInfo info) {
        ecmEffects.add(info);
    }

    /**
     * Once all of the ECMInfo has been collected for this location, we need to
     * determine how to color the Hex.  Each player that has an E(C)CM presense
     * in this hex must have their color represented somehow.  Opposing ECM and
     * ECCM effects should also be considered.  This method should also update
     * the isECCM state variable, so we can determine whether ECM or ECCM
     * shading should be used.
     *
     * @return  The color to use to represent the ECM effects in this hex
     */
    public @Nullable Color getHexColor() {
        Color c = null;
        Map<Player, ECMInfo> ecmEffectsForPlayer = new HashMap<>();
        // Total the ECM effects for each Player
        for (ECMInfo ecmInfo : ecmEffects) {
            ECMInfo playerECM = ecmEffectsForPlayer.get(ecmInfo.getOwner());
            if (playerECM == null) {
                playerECM = new ECMInfo(ecmInfo);
                ecmEffectsForPlayer.put(ecmInfo.getOwner(), playerECM);
            } else {
                playerECM.addAlliedECMEffects(ecmInfo);
            }
        }
        // Each Player that has an active E(C)CM effect will have a color
        // to contribute to this location
        List<Color> ecmColors = new LinkedList<>();
        List<Color> eccmColors = new LinkedList<>();
        for (Player p : ecmEffectsForPlayer.keySet()) {
            ECMInfo playerECM = new ECMInfo(ecmEffectsForPlayer.get(p));
            for (Player other : ecmEffectsForPlayer.keySet()) {
                // Don't add info for p again
                if (Objects.equals(p, other)) {
                    continue;
                }
                playerECM.addAlliedECMEffects(ecmEffectsForPlayer.get(other));
            }
            if (playerECM.isECM()) {
                ecmColors.add(getECMColor(p));
            } else if (playerECM.isECCM()) {
                eccmColors.add(getECMColor(p));
            }
        }

        // It's possible all effects cancel each other out; then return null
        if (ecmColors.isEmpty() && eccmColors.isEmpty()) {
            return null;
        }
        // If there is ECCM present, but no ECM, then shade as ECCM.
        // ECM shading subsumes ECCM shading, so if ECM is present,
        // ECCM shading isn't needed
        if ((ecmColors.size() < 1) && !eccmColors.isEmpty()) {
            isECCM = true;
            c = getColorAverage(eccmColors);
        } else {
            isECCM = false;
            c = getColorAverage(ecmColors);
        }
        return c;
    }

    public boolean isECCM() {
        return isECCM;
    }

    /**
     * Given a collection of colors, which represents all of ECM colors for
     * different players, create an average color to be used.
     *
     * @param colors
     * @return
     */
    public static Color getColorAverage(List<Color> colors) {
        final int alpha = GUIP.getECMTransparency();

        int red, green, blue;
        red = green = blue = 0;
        for (Color c : colors) {
            red += c.getRed();
            green += c.getGreen();
            blue += c.getBlue();
        }
        red = red / colors.size();
        green = green / colors.size();
        blue = blue / colors.size();

        return new Color(red, green, blue, alpha);
    }

    /**
     * Used to determine the color that should be used to indicate ECM effects
     * for a given player
     *
     * @param player
     * @return
     */
    public static Color getECMColor(Player player) {
        final int alpha = GUIP.getECMTransparency();
        Color tint = (player == null) ? Color.GRAY : player.getColour().getColour();
        // Create a new color by adding transparency to the tint
        return new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), alpha);
    }

}
