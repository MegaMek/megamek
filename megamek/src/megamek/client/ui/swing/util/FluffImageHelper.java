/*
 * Copyright (c) 2009 Jay Lawson
 * Copyright (c) 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
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
package megamek.client.ui.swing.util;

import megamek.common.BTObject;
import megamek.common.Configuration;
import megamek.common.Mech;
import megamek.common.annotations.Nullable;
import megamek.common.preference.PreferenceManager;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides methods for retrieving fluff images, for use in MM, MML and MHQ; also
 * for record sheets (where the fallback image "hud.png" is used).
 */
public final class FluffImageHelper {

    public static final String DIR_NAME_BA = "BattleArmor";
    public static final String DIR_NAME_CONVFIGHTER = "ConvFighter";
    public static final String DIR_NAME_DROPSHIP = "DropShip";
    public static final String DIR_NAME_FIGHTER = "Fighter";
    public static final String DIR_NAME_INFANTRY = "Infantry";
    public static final String DIR_NAME_JUMPSHIP = "JumpShip";
    public static final String DIR_NAME_MECH = "Mech";
    public static final String DIR_NAME_PROTOMEK = "ProtoMek";
    public static final String DIR_NAME_SMALLCRAFT = "Small Craft";
    public static final String DIR_NAME_SPACESTATION = "Space Station";
    public static final String DIR_NAME_VEHICLE = "Vehicle";
    public static final String DIR_NAME_WARSHIP = "WarShip";
    public static final String[] EXTENSIONS_FLUFF_IMAGE_FORMATS = { ".PNG", ".png", ".JPG",
            ".JPEG", ".jpg", ".jpeg", ".GIF", ".gif" };

    /**
     * Returns a fluff image for the given unit/object to be shown e.g. in the unit summary.
     *
     * <P>If a fluff image is stored in the unit/object itself, e.g. if it was part of the
     * unit's file or is created by the unit itself, this is returned. Note that this
     * is not used for canon units, but may be used in custom ones by adding a fluff
     * image to the unit in MML.
     *
     * <P>Otherwise, the fluff images directories are searched. First searches the user dir,
     * then the internal dir. Tries to match the image by chassis + model or chassis alone.
     * Chassis and model names are cleaned from " and / characters before matching. For
     * Meks with clan names, both names and the combinations are searched. The model
     * alone is not used to search.
     *
     * Returns null if no fluff image can be found.
     *
     * @param unit The unit
     * @return a fluff image or null, if no match is found
     */
    public static @Nullable Image getFluffImage(@Nullable BTObject unit) {
        return getFluffImage(unit, false);
    }

    /**
     * Returns a fluff image for the given unit for the record sheet, with a fallback
     * file named "hud.png" if that is present in the right fluff directory, or null if nothing
     * can be found. See {@link #getFluffImage(BTObject)} for further comments on how the fluff
     * image is searched.
     *
     * @param unit The unit
     * @return a fluff image or null, if no match is found
     */
    @SuppressWarnings("unused") // used in MML
    public static @Nullable Image getRecordSheetFluffImage(@Nullable BTObject unit) {
        return getFluffImage(unit, true);
    }

    private static @Nullable Image getFluffImage(@Nullable BTObject unit, boolean recordSheet) {
        if (unit == null) {
            return null;
        }
        Image embeddedFluffImage = unit.getFluffImage();
        if (embeddedFluffImage != null) {
            return embeddedFluffImage;
        } else {
            File fluffImageFile = findFluffFile(unit, recordSheet);
            if (fluffImageFile != null) {
                return new ImageIcon(fluffImageFile.toString()).getImage();
            } else {
                return null;
            }
        }
    }

    private static @Nullable File findFluffFile(BTObject unit, boolean recordSheet) {
        List<File> fileCandidates = new ArrayList<>();
        var fluffDir = new File(Configuration.fluffImagesDir(), FluffImageHelper.getFluffPath(unit));
        var rsFluffSuperDir = new File(Configuration.fluffImagesDir(), "rs");
        var rsFluffDir = new File(rsFluffSuperDir, FluffImageHelper.getFluffPath(unit));

        List<String> nameCandidates = nameCandidates(unit);

        // UserDir matches
        // For internal use: in [user dir]/data/images/fluff/rs/<type> images for record sheets can be placed
        // These will be preferentially loaded when the recordSheet paremeter is true
        String userDir = PreferenceManager.getClientPreferences().getUserDir();
        if (!userDir.isBlank() && new File(userDir).isDirectory()) {
            var fluffUserDir = new File(userDir, fluffDir.toString());
            var rsFluffUserDir = new File(userDir, rsFluffDir.toString());

            if (recordSheet) {
                for (String nameCandidate : nameCandidates) {
                    for (String ext : EXTENSIONS_FLUFF_IMAGE_FORMATS) {
                        fileCandidates.add(new File(rsFluffUserDir, nameCandidate + ext));
                    }
                }
            }
            for (String nameCandidate : nameCandidates) {
                for (String ext : EXTENSIONS_FLUFF_IMAGE_FORMATS) {
                    fileCandidates.add(new File(fluffUserDir, nameCandidate + ext));
                }
            }
        }

        // Internal fluff path matches
        for (String nameCandidate : nameCandidates) {
            for (String ext : EXTENSIONS_FLUFF_IMAGE_FORMATS) {
                fileCandidates.add(new File(fluffDir, nameCandidate + ext));
            }
        }

        // Fallback
        if (recordSheet) {
            fileCandidates.add(new File(fluffDir, "hud.png"));
        }

        for (File possibleFile : fileCandidates) {
            if (possibleFile.exists() && !possibleFile.isDirectory()) {
                return possibleFile;
            }
        }
        return null;
    }

    private static String sanitize(String original) {
        return original.replace("\"", "").replace("/", "");
    }

    private static List<String> nameCandidates(BTObject unit) {
        List<String> nameCandidates = new ArrayList<>();

        String sanitizedChassis = sanitize(unit.generalName());
        String sanitizedModel = sanitize(unit.specificName());
        nameCandidates.add((sanitizedChassis + " " + sanitizedModel).trim());
        if (unit instanceof Mech && !((Mech) unit).getClanChassisName().isBlank()) {
            Mech mek = (Mech) unit;
            String fullChassis = sanitize(mek.getFullChassis());
            nameCandidates.add((fullChassis + " " + sanitizedModel).trim());
            String clanChassis = sanitize(mek.getClanChassisName());
            nameCandidates.add((clanChassis + " " + sanitizedModel).trim());
            nameCandidates.add(fullChassis);
            nameCandidates.add(clanChassis);
        }
        nameCandidates.add(sanitizedChassis);
        return nameCandidates;
    }

    private FluffImageHelper() { }

    /**
     * Returns the subdirectory in the fluff images directory suitable for the given
     * unit, i.e. "ConvFighter" for CF and FWS units.
     *
     * @param unit The unit
     * @return The unit type subdirectory for fluff images
     */
    public static String getFluffPath(BTObject unit) {
        if (unit.isWarShip()) {
            return DIR_NAME_WARSHIP;
        } else if (unit.isSpaceStation()) {
            return DIR_NAME_SPACESTATION;
        } else if (unit.isJumpShip()) {
            return DIR_NAME_JUMPSHIP;
        } else if (unit.isConventionalFighter() || unit.isFixedWingSupport()) {
            return DIR_NAME_CONVFIGHTER;
        } else if (unit.isDropShip()) {
            return DIR_NAME_DROPSHIP;
        } else if (unit.isSmallCraft()) {
            return DIR_NAME_SMALLCRAFT;
        } else if (unit.isFighter()) {
            return DIR_NAME_FIGHTER;
        } else if (unit.isBattleArmor()) {
            return DIR_NAME_BA;
        } else if (unit.isConventionalInfantry()) {
            return DIR_NAME_INFANTRY;
        } else if (unit.isProtoMek()) {
            return DIR_NAME_PROTOMEK;
        } else if (unit.isVehicle()) {
            return DIR_NAME_VEHICLE;
        } else {
            return DIR_NAME_MECH;
        }
    }
}