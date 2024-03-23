/*
 * Copyright (c) 2022, 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.scenario;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScenarioLoader {

    protected static final String SEPARATOR_COMMA = ",";

    private final File scenarioFile;


    public ScenarioLoader(File f) {
        scenarioFile = f;
    }

    public ScenarioLoader(String filename) {
        this(new File(filename));
    }

    /**
     * Loads and returns the loaded scenario as a {@link ScenarioV1} or {@link ScenarioV2}.
     *
     * @return The loaded scenario
     * @throws ScenarioLoaderException When the file has malformed information and cannot be parsed
     * @throws IOException When the file cannot be accessed
     */
    public Scenario load() throws ScenarioLoaderException, IOException {
        int mmsVersion = findMmsVersion();
        if (mmsVersion == 1) {
            return new ScenarioV1(scenarioFile);
        } else if (mmsVersion == 2) {
            return new ScenarioV2(scenarioFile);
        } else {
            throw new ScenarioLoaderException("ScenarioLoaderException.missingMMSVersion", scenarioFile.toString());
        }
    }

    private void parsePlanetaryConditions(Game g, ScenarioInfo p) {
        if (p.containsKey(PARAM_PLANETCOND_TEMP)) {
            g.getPlanetaryConditions().setTemperature(Integer.parseInt(p.getString(PARAM_PLANETCOND_TEMP)));
        }

        if (p.containsKey(PARAM_PLANETCOND_GRAV)) {
            g.getPlanetaryConditions().setGravity(Float.parseFloat(p.getString(PARAM_PLANETCOND_GRAV)));
        }

        if (p.containsKey(PARAM_PLANETCOND_FOG)) {
            g.getPlanetaryConditions().setFog(Fog.getFog(StringUtil.toInt(p.getString(PARAM_PLANETCOND_FOG), 0)));
        }

        if (p.containsKey(PARAM_PLANETCOND_ATMOS)) {
            g.getPlanetaryConditions().setAtmosphere(Atmosphere.getAtmosphere(StringUtil.toInt(p.getString(PARAM_PLANETCOND_ATMOS),0)));
        }

        if (p.containsKey(PARAM_PLANETCOND_LIGHT)) {
            g.getPlanetaryConditions().setLight(Light.getLight(StringUtil.toInt(p.getString(PARAM_PLANETCOND_LIGHT), 0)));
        }

        if (p.containsKey(PARAM_PLANETCOND_WEATHER)) {
            g.getPlanetaryConditions().setWeather(Weather.getWeather(StringUtil.toInt(p.getString(PARAM_PLANETCOND_WEATHER), 0)));
        }

        if (p.containsKey(PARAM_PLANETCOND_WIND)) {
            g.getPlanetaryConditions().setWind(Wind.getWind(StringUtil.toInt(p.getString(PARAM_PLANETCOND_WIND),0)));
        }

        if (p.containsKey(PARAM_PLANETCOND_WINDDIR)) {
            g.getPlanetaryConditions().setWindDirection(WindDirection.getWindDirection(StringUtil.toInt(p.getString(PARAM_PLANETCOND_WINDDIR),0)));
        }

        if (p.containsKey(PARAM_PLANETCOND_WINDSHIFTINGDIR)) {
            g.getPlanetaryConditions().setShiftingWindDirection(parseBoolean(p, PARAM_PLANETCOND_WINDSHIFTINGDIR, false));
        }

        if (p.containsKey(PARAM_PLANETCOND_WINDSHIFTINGSTR)) {
            g.getPlanetaryConditions().setShiftingWindStrength(parseBoolean(p, PARAM_PLANETCOND_WINDSHIFTINGSTR, false));
        }

        if (p.containsKey(PARAM_PLANETCOND_WINDMIN)) {
            g.getPlanetaryConditions().setWindMin(Wind.getWind(StringUtil.toInt(p.getString(PARAM_PLANETCOND_WINDMIN), 0)));
        }

        if (p.containsKey(PARAM_PLANETCOND_WINDMAX)) {
            g.getPlanetaryConditions().setWindMax(Wind.getWind(StringUtil.toInt(p.getString(PARAM_PLANETCOND_WINDMAX), 0)));
        }

        if (p.containsKey(PARAM_PLANETCOND_EMI)) {
            EMI emi = parseBoolean(p, PARAM_PLANETCOND_EMI, false) ? EMI.EMI : EMI.EMI_NONE;
            g.getPlanetaryConditions().setEMI(emi);
        }

        if (p.containsKey(PARAM_PLANETCOND_TERRAINCHANGES)) {
            g.getPlanetaryConditions().setTerrainAffected(parseBoolean(p, PARAM_PLANETCOND_TERRAINCHANGES, true));
        }

        if (p.containsKey(PARAM_PLANETCOND_BLOWINGSAND)) {
            BlowingSand blowingSand = parseBoolean(p, PARAM_PLANETCOND_BLOWINGSAND, false) ? BlowingSand.BLOWING_SAND : BlowingSand.BLOWING_SAND_NONE;
            g.getPlanetaryConditions().setBlowingSand(blowingSand);
        }
    }

    private Collection<Entity> buildFactionEntities(ScenarioInfo p, Player player) throws ScenarioLoaderException {
        String faction = player.getName();
        Pattern unitPattern = Pattern.compile(String.format("^Unit_\\Q%s\\E_[^_]+$", faction));
        Pattern unitDataPattern = Pattern.compile(String.format("^(Unit_\\Q%s\\E_[^_]+)_([A-Z][^_]+)$", faction));

        Map<String, Entity> entities = new HashMap<>();

        // Gather all defined units
        for (String key : p.keySet()) {
            if (unitPattern.matcher(key).matches() && (p.getNumValues(key) > 0)) {
                if (p.getNumValues(key) > 1) {
                    LogManager.getLogger().error(String.format("Scenario loading: Unit declaration %s found %d times",
                            key, p.getNumValues(key)));
                    throw new ScenarioLoaderException("multipleUnitDeclarations", key);
                }
                entities.put(key, parseEntityLine(p.getString(key)));
            }
        }

        // Add other information
        for (String key: p.keySet()) {
            Matcher dataMatcher = unitDataPattern.matcher(key);
            if (dataMatcher.matches()) {
                String unitKey = dataMatcher.group(1);
                if (!entities.containsKey(unitKey)) {
                    LogManager.getLogger().warn("Scenario loading: Data for undeclared unit encountered, ignoring: " + key);
                    continue;
                }
                Entity e = entities.get(unitKey);
                switch (dataMatcher.group(2)) {
                    case PARAM_DAMAGE:
                        for (String val : p.get(key)) {
                            damagePlans.add(new DamagePlan(e, Integer.parseInt(val)));
                        }
                        break;
                    case PARAM_SPECIFIC_DAMAGE:
                        DamagePlan dp = new DamagePlan(e);
                        for (String val : p.getString(key).split(SEPARATOR_COMMA, -1)) {
                            dp.addSpecificDamage(val);
                        }
                        damagePlans.add(dp);
                        break;
                    case PARAM_CRITICAL_HIT:
                        CritHitPlan chp = new CritHitPlan(e);
                        for (String val : p.getString(key).split(SEPARATOR_COMMA, -1)) {
                            chp.addCritHit(val);
                        }
                        critHitPlans.add(chp);
                        break;
                    case PARAM_AMMO_AMOUNT:
                        SetAmmoPlan amountSap = new SetAmmoPlan(e);
                        for (String val : p.getString(key).split(SEPARATOR_COMMA, -1)) {
                            amountSap.addSetAmmoTo(val);
                        }
                        ammoPlans.add(amountSap);
                        break;
                    case PARAM_AMMO_TYPE:
                        SetAmmoPlan typeSap = new SetAmmoPlan(e);
                        for (String val : p.getString(key).split(SEPARATOR_COMMA, -1)) {
                            typeSap.addSetAmmoType(val);
                        }
                        ammoPlans.add(typeSap);
                        break;
                    case PARAM_PILOT_HITS:
                        int hits = Integer.parseInt(p.getString(key));
                        e.getCrew().setHits(Math.min(hits, 5), 0);
                        break;
                    case PARAM_EXTERNAL_ID:
                        e.setExternalIdAsString(p.getString(key));
                        break;
                    case PARAM_ADVANTAGES:
                        parseAdvantages(e, p.getString(key, SEPARATOR_SPACE));
                        break;
                    case PARAM_AUTO_EJECT:
                        parseAutoEject(e, p.getString(key));
                        break;
                    case PARAM_COMMANDER:
                        parseCommander(e, p.getString(key));
                        break;
                    case PARAM_DEPLOYMENT_ROUND:
                        int round = Integer.parseInt(p.getString(key));
                        if (round > 0) {
                            LogManager.getLogger().debug(String.format("%s will be deployed before round %d",
                                e.getDisplayName(), round));
                            e.setDeployRound(round);
                            e.setDeployed(false);
                            e.setNeverDeployed(false);
                            e.setPosition(null);
                        }
                        break;
                    case PARAM_CAMO:
                        final Camouflage camouflage = parseCamouflage(p.getString(key));
                        if (!camouflage.isDefault()) {
                            e.setCamouflage(camouflage);
                        }
                        break;
                    case PARAM_ALTITUDE:
                        int altitude = Math.min(Integer.parseInt(p.getString(key)), 10);
                        if (e.isAero()) {
                            e.setAltitude(altitude);
                            if (altitude <= 0) {
                                ((IAero) e).land();
                            }
                        } else {
                            LogManager.getLogger().warn(String.format("Altitude setting for a non-aerospace unit %s; ignoring",
                                    e.getShortName()));
                        }
                        break;
                    default:
                        LogManager.getLogger().error("Scenario loading: Unknown unit data key " + key);
                }
            }
        }

        return entities.values();
    }

    private Entity parseEntityLine(String s) throws ScenarioLoaderException {
        try {
            String[] parts = s.split(SEPARATOR_COMMA, -1);
            int i;
            MechSummary ms = MechSummaryCache.getInstance().getMech(parts[0]);
            if (ms == null) {
                throw new ScenarioLoaderException("missingRequiredEntity", parts[0]);
            }
            LogManager.getLogger().debug(String.format("Loading %s", ms.getName()));
            Entity e = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();

            // The following section is used to determine if part 4 of the string includes gender or not
            // The regex is used to match a number that might be negative. As the direction is never
            // a number, if a number is found it must be gender.
            // The i value must be included to ensure that the correct indexes are used for the
            // direction calculation below.
            if ((parts.length > 4) && parts[4].matches("-?\\d+")) {
                e.setCrew(new Crew(e.getCrew().getCrewType(), parts[1], 1,
                        Integer.parseInt(parts[2]), Integer.parseInt(parts[3]),
                        Gender.parseFromString(parts[4]), Boolean.parseBoolean(parts[5]), null));
                i = 6; // direction will be part 6, as the scenario has the gender of its pilots included
            } else {
                e.setCrew(new Crew(e.getCrew().getCrewType(), parts[1], 1,
                        Integer.parseInt(parts[2]), Integer.parseInt(parts[3]),
                        RandomGenderGenerator.generate(), e.isClan(), null));
                i = 4; // direction will be part 4, as the scenario does not contain gender
            }


    /**
     * @return The MMS version (1 or 2) or -1 if no version can be found
     * @throws FileNotFoundException When the current file doesn't exist
     */
    private int findMmsVersion() throws IOException {
        Scanner scanner = new Scanner(scenarioFile);
        Pattern versionPattern = Pattern.compile("^\\s*"+ Scenario.MMSVERSION +"\\s*[:=]\\s*(\\d)");
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            Matcher versionMatcher = versionPattern.matcher(line);
            if (!isCommentLine(line) && versionMatcher.find()) {
                return Integer.parseInt(versionMatcher.group(1));
            }
        }
        return -1;
    }

    private boolean isCommentLine(String line) {
        return line.trim().startsWith("#");
    }
}