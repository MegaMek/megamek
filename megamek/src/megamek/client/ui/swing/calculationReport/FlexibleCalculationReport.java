/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing.calculationReport;

import javax.swing.*;

public class FlexibleCalculationReport implements CalculationReport {

    private final HTMLCalculationReport htmlReport = new HTMLCalculationReport();
    private final SwingCalculationReport swingReport = new SwingCalculationReport();
    private final TextCalculationReport textReport = new TextCalculationReport();

    public CalculationReport getHtmlReport() {
        return htmlReport;
    }

    public CalculationReport getSwingReport() {
        return swingReport;
    }

    public CalculationReport getTextReport() {
        return textReport;
    }

    @Override
    public CalculationReport addLine(String type, String calculation, String result) {
        htmlReport.addLine(type, calculation, result);
        swingReport.addLine(type, calculation, result);
        textReport.addLine(type, calculation, result);
        return this;
    }

    @Override
    public CalculationReport addSubHeader(String text) {
        htmlReport.addSubHeader(text);
        swingReport.addSubHeader(text);
        textReport.addSubHeader(text);
        return this;
    }

    @Override
    public CalculationReport addHeader(String text) {
        htmlReport.addHeader(text);
        swingReport.addHeader(text);
        textReport.addHeader(text);
        return this;
    }

    @Override
    public CalculationReport addResultLine(String type, String calculation, String result) {
        htmlReport.addResultLine(type, calculation, result);
        swingReport.addResultLine(type, calculation, result);
        textReport.addResultLine(type, calculation, result);
        return this;
    }

    @Override
    public JComponent toJComponent() {
        return swingReport.toJComponent();
    }

    @Override
    public String toString() {
        return textReport.toString();
    }

}
