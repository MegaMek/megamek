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
import javax.swing.JLabel;

/**
 * This is a Calculation Report that can be used when the report that comes with a calculation isn't going to be used.
 * It saves time and memory by doing nothing with any passed parameters. Its toString() and toJComponent() methods yield
 * a short explanatory message as String and JLabel, respectively, and so can be called safely.
 */
public class DummyCalculationReport implements CalculationReport {

    @Override
    public CalculationReport addLine(String type, String calculation, String result) {
        return this;
    }

    @Override
    public CalculationReport addLine(String type, String calculation, String resultPrefix, double result) {
        return this;
    }

    @Override
    public CalculationReport addLine(String type, String resultPrefix, double result) {
        return this;
    }

    @Override
    public CalculationReport addResultLine(String type, String calculation, String resultPrefix, double result) {
        return this;
    }

    @Override
    public CalculationReport addResultLine(String type, String resultPrefix, double result) {
        return this;
    }

    @Override
    public CalculationReport addResultLine(String resultPrefix, double result) {
        return this;
    }

    @Override
    public CalculationReport addEmptyLine() {
        return this;
    }

    @Override
    public CalculationReport addLine(String type, String result) {
        return this;
    }

    @Override
    public CalculationReport addLine(String result) {
        return this;
    }

    @Override
    public CalculationReport addSubHeader(String text) {
        return this;
    }

    @Override
    public CalculationReport addHeader(String text) {
        return this;
    }

    @Override
    public CalculationReport addResultLine(String type, String calculation, String result) {
        return this;
    }

    @Override
    public JComponent toJComponent() {
        return new JLabel("This is an intentionally empty calculation report.");
    }

    @Override
    public void startTentativeSection() {}

    @Override
    public void endTentativeSection() {}

    @Override
    public void discardTentativeSection() {}

    @Override
    public String toString() {
        return "This is an intentionally empty calculation report.";
    }
}
