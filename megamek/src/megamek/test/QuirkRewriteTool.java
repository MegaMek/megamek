/*
 * MegaMek - Copyright (C) 2003,2004 Ben Mazur (bmazur@sev.org)
 *  Copyright © 2016 Nicholas Walczak (walczak@cs.umn.edu)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import megamek.common.EquipmentType;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.QuirksHandler;

/**
 * This program is a tool to help rewrite a quirks file that does not have
 * unitType tags. If not unitType tag is present, then "Mech" is assumed. This
 * program knows about the possible unit types, and checks to see if a quirk
 * entry would match another unit type, and if it does, it adds a quirks entry
 * with the same quirks for that unit type.
 * 
 * @author arlith
 * @date April 2016
 */
public class QuirkRewriteTool implements MechSummaryCache.Listener {

    private static MechSummaryCache mechSummaryCache = null;

    private static String[] quirkETypes = { "Mech", "Aero", "VTOL", "Tank",
            "Infantry", "Protomech" };

    public static void main(String[] args) {
        EquipmentType.initializeTypes();

        QuirkRewriteTool qc = new QuirkRewriteTool();
        mechSummaryCache = MechSummaryCache.getInstance(true);
        mechSummaryCache.addListener(qc);
    }

    @Override
    public void doneLoading() {
        MechSummary[] ms = mechSummaryCache.getAllMechs();

        try {
            QuirksHandler.initQuirksList();
        } catch (IOException e) {
            System.err.println("Error initializing quirks!");
            e.printStackTrace();
            return;
        }

        Set<String> quirkIds = QuirksHandler.getCanonQuirkIds();

        System.out.println("Matching canon quirks!");
        // Iterate through each quirk id
        for (String quirkId : quirkIds) {
            // Going to check if which entities it may match...
            boolean matched = false;
            for (MechSummary summary : ms) {
                // Munged quirk ID: removes eType for comparison
                String mungedId = QuirksHandler.replaceUnitType(quirkId, "");
                // Quirk ID for the current MechSummary/Unit, without eType
                String unitId = QuirksHandler.getUnitId(summary.getChassis(),
                        summary.getModel(), "");
                String unitIdNoModel = QuirksHandler.getUnitId(
                        summary.getChassis(), "all", "");
                // If there's a match, add a new custom quirk entry
                if (mungedId.equals(unitId) || mungedId.equals(unitIdNoModel)) {
                    String newId = QuirksHandler.getUnitId(
                            summary.getChassis(),
                            QuirksHandler.getModel(quirkId),
                            MechSummary.determineETypeName(summary));
                    QuirksHandler.mungeQuirks(quirkId, newId);
                    matched = true;
                }
            }
            if (!matched) {
                System.out.println("\t" + quirkId + " did not match anything!");
            }
        }
        System.out.println("\n");

        System.out.println("Writing new custom quirks!");
        // Save the munged quirks in the custom quirks list
        try {
            QuirksHandler.saveCustomQuirksList();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("\n");

        System.out.println("Comparing Quirks!");
        Set<String> quirksNotInCustom = new HashSet<>();
        for (String quirkId : quirkIds) {
            boolean match = false;
            for (String eType : quirkETypes) {
                String mungedId = QuirksHandler.replaceUnitType(quirkId, eType);
                match |= QuirksHandler.customQuirksContain(mungedId);
            }
            if (!match) {
                quirksNotInCustom.add(quirkId);
            }
        }
        for (String quirkId : quirksNotInCustom) {
            System.out.println("\t" + quirkId + " is not in Custom Quirks!");
        }
    }

}
