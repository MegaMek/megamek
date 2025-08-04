/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.clientGUI.calculationReport;

import javax.swing.JComponent;

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
    public void startTentativeSection() {
        htmlReport.startTentativeSection();
        swingReport.startTentativeSection();
        textReport.startTentativeSection();
    }

    @Override
    public void endTentativeSection() {
        htmlReport.endTentativeSection();
        swingReport.endTentativeSection();
        textReport.endTentativeSection();
    }

    @Override
    public void discardTentativeSection() {
        htmlReport.discardTentativeSection();
        swingReport.discardTentativeSection();
        textReport.discardTentativeSection();
    }

    @Override
    public String toString() {
        return textReport.toString();
    }

}
