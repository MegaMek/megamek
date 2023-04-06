/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.alphaStrike;

import freemarker.template.Template;
import megamek.common.annotations.Nullable;
import megamek.common.templates.TemplateConfiguration;
import org.apache.logging.log4j.LogManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to create a short text summary of an Alpha Strike element (or MechSummary) for export. The
 * format is the same as that used in Mordel's unit overview.
 */
public class ASStatsExporter {

    private final static String TEMPLATE_FILENAME = "alphaStrike/as_stats_export.ftl";

    private final ASCardDisplayable element;
    private Template template;
    private Map<String, Object> model;

    /**
     * Creates a new stats exporter for the given Alpha Strike element or MechSummary. The stats can be obtained
     * by calling {@link #getStats()}.
     *
     * @param element The Alpha Strike element or MechSummary to create a stats text for
     */
    public ASStatsExporter(@Nullable ASCardDisplayable element) {
        this.element = element;
        try {
            template = TemplateConfiguration.getInstance().getTemplate(TEMPLATE_FILENAME);
        } catch (final IOException e) {
            LogManager.getLogger().error("", e);
        }
    }

    private void prepareData() {
        model = new HashMap<>();
        if (element != null) {
            model.put("chassis", element.getChassis());
            model.put("model", element.getModel());
            model.put("PV", element.getPointValue());
            model.put("TP", element.getASUnitType().toString());
            model.put("SZ", element.getSize());
            model.put("TMM", element.getTMM());
            model.put("OV", element.getOV());
            model.put("usesOV", element.usesOV());
            model.put("MV", AlphaStrikeHelper.getMovementAsString(element));
            model.put("usesArcs", element.usesArcs());
            model.put("dmg", dmgData(element.getStandardDamage()));
            model.put("usesE", element.usesSMLE());
            model.put("Arm", element.getFullArmor());
            model.put("Th", element.getThreshold());
            model.put("usesTh", element.usesThreshold());
            model.put("Str", element.getFullStructure());
            model.put("specials", element.getSpecialAbilities().getSpecialsDisplayString(", ", element));
            if (element.usesArcs()) {
                model.put("frontArc", arcData(element.getFrontArc()));
                model.put("leftArc", arcData(element.getLeftArc()));
                model.put("rightArc", arcData(element.getRightArc()));
                model.put("rearArc", arcData(element.getRearArc()));
            }
        }
    }

    private HashMap<String, Object> arcData(ASSpecialAbilityCollection arc) {
        HashMap<String, Object> arcData = new HashMap<>();
        arcData.put("STD", dmgData(arc.getStdDamage()));
        arcData.put("CAP", dmgData(arc.getCAP()));
        arcData.put("SCAP", dmgData(arc.getSCAP()));
        arcData.put("MSL", dmgData(arc.getMSL()));
        arcData.put("specials", arc.getSpecialsDisplayString(element));
        return arcData;
    }

    private HashMap<String, Object> dmgData(ASDamageVector dmg) {
        HashMap<String, Object> dmgData = new HashMap<>();
        dmgData.put("dmgS", dmg.S.toStringWithZero());
        dmgData.put("dmgM", dmg.M.toStringWithZero());
        dmgData.put("dmgL", dmg.L.toStringWithZero());
        dmgData.put("dmgE", dmg.E.toStringWithZero());
        return dmgData;
    }

    /**
     * Returns a stats summary of the Alpha Strike element (or MechSummary). If there is an error with
     * generating the summary the returned string contains this error.
     *
     * @return The stats summary of the Alpha Strike element
     */
    public String getStats() {
        if (element == null) {
            return "No stats: a null unit was passed to ASStatsExporter.";
        }
        if (model == null) {
            prepareData();
        }
        try (final ByteArrayOutputStream os = new ByteArrayOutputStream(); final Writer out = new OutputStreamWriter(os)) {
            template.process(model, out);
            return os.toString();
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
            return "Error: could not create the Alpha Strike stats text.";
        }
    }
}