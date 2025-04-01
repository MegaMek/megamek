/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 2 or (at your option) any later version,
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
 */
package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;
import megamek.common.*;

/**
 * Calculates the unit health (5 values, average, front, left, right, rear).
 * @author Luana Coppio
 */
public class UnitHealthCalculator extends BaseAxisCalculator {

    private static final OverallArmor OVERALL_ARMOR = new OverallArmor();
    private static final FrontSide FRONT_SIDE = new FrontSide();
    private static final LeftSide LEFT_SIDE = new LeftSide();
    private static final RightSide RIGHT_SIDE = new RightSide();
    private static final BackSide BACK_SIDE = new BackSide();

    @Override
    public float[] axis() {
        return new float[5];
    }

    @Override
    public float[] calculateAxis(Pathing pathing, GameState gameState) {
        // This would calculate the health of the unit as a percentage for
        // 0 - average
        // 1 - front
        // 2 - left
        // 3 - right
        // 4 - back

        float[] health = axis();
        Entity unit = pathing.getEntity();

        health[0] = OVERALL_ARMOR.getArmorRemainingPercent(unit);
        health[1] = FRONT_SIDE.getArmorRemainingPercent(unit);
        health[2] = RIGHT_SIDE.getArmorRemainingPercent(unit);
        health[3] = LEFT_SIDE.getArmorRemainingPercent(unit);
        health[4] = BACK_SIDE.getArmorRemainingPercent(unit);

        return health;
    }

    private static class OverallArmor {
        public float getArmorRemainingPercent(Entity unit) {
            if (unit instanceof Warship warship) {
                return getHealth(warship);
            } else if (unit instanceof SpaceStation spaceStation) {
                return getHealth(spaceStation);
            } else if (unit instanceof Jumpship jumpship) {
                return getHealth(jumpship);
            } else if (unit instanceof Aero aero) {
                return getHealth(aero);
            } else if (unit instanceof SuperHeavyTank superHeavyTank) {
                return getHealth(superHeavyTank);
            } else if (unit instanceof GunEmplacement gunEmplacement) {
                return getHealth(gunEmplacement);
            } else if (unit instanceof Tank tank) {
                return getHealth(tank);
            } else if (unit instanceof Mek mek) {
                return getHealth(mek);
            } else if (unit instanceof ProtoMek protoMek) {
                return getHealth(protoMek);
            } else {
                return (float) unit.getArmorRemainingPercent();
            }
        }

        protected float getHealth(Aero unit) {
            return (float) unit.getArmorRemainingPercent();
        }

        protected float getHealth(Tank unit) {
            return (float)  unit.getArmorRemainingPercent();
        }

        protected float getHealth(GunEmplacement unit) {
            return (float) unit.getArmorRemainingPercent();
        }

        protected float getHealth(SuperHeavyTank unit) {
            return (float) unit.getArmorRemainingPercent();
        }

        protected float getHealth(Mek unit) {
            return (float) unit.getArmorRemainingPercent();
        }

        protected float getHealth(ProtoMek unit) {
            return (float) unit.getArmorRemainingPercent();
        }

        protected float getHealth(Jumpship unit) {
            return (float) unit.getArmorRemainingPercent();
        }

        protected float getHealth(SpaceStation unit) {
            return (float) unit.getArmorRemainingPercent();
        }

        protected float getHealth(Warship unit) {
            return (float) unit.getArmorRemainingPercent();
        }
    }

    private static class BackSide extends OverallArmor {

        @Override
        protected float getHealth(Aero unit) {
            return unit.getArmor(Aero.LOC_AFT) / (float) unit.getOArmor(Aero.LOC_AFT);
        }

        @Override
        protected float getHealth(Tank unit) {
            return unit.getArmor(Tank.LOC_REAR) / (float) unit.getOArmor(Tank.LOC_REAR);
        }

        @Override
        protected float getHealth(SuperHeavyTank unit) {
            return (unit.getArmor(SuperHeavyTank.LOC_REAR)) / (float) (unit.getOArmor(SuperHeavyTank.LOC_REAR));
        }

        @Override
        protected float getHealth(Mek unit) {
            return (unit.getArmor(Mek.LOC_LT, true) + unit.getArmor(Mek.LOC_CT, true) + unit.getArmor(Mek.LOC_RT, true))
                         / (float) (unit.getOArmor(Mek.LOC_LT, true) + unit.getOArmor(Mek.LOC_CT, true) + unit.getOArmor(Mek.LOC_RT, true));
        }

        @Override
        protected float getHealth(Jumpship unit) {
            return (unit.getArmor(Jumpship.LOC_AFT) + unit.getArmor(Jumpship.LOC_ARS)) / (float) (unit.getOArmor(Jumpship.LOC_FRS) + unit.getOArmor(Jumpship.LOC_ARS));
        }

        @Override
        protected float getHealth(SpaceStation unit) {
            return (unit.getArmor(SpaceStation.LOC_FRS) + unit.getArmor(SpaceStation.LOC_ARS)) / (float) (unit.getOArmor(SpaceStation.LOC_FRS) + unit.getOArmor(SpaceStation.LOC_ARS));
        }

        @Override
        protected float getHealth(Warship unit) {
            return (unit.getArmor(Warship.LOC_FRS) + unit.getArmor(Warship.LOC_ARS) + unit.getArmor(Warship.LOC_RBS)) / (float) (unit.getOArmor(Warship.LOC_FRS) + unit.getOArmor(Warship.LOC_ARS) + unit.getOArmor(Warship.LOC_RBS));
        }
    }

    private static class FrontSide extends OverallArmor {

        @Override
        protected float getHealth(Aero unit) {
            return unit.getArmor(Aero.LOC_NOSE) / (float) unit.getOArmor(Aero.LOC_NOSE);
        }

        @Override
        protected float getHealth(Tank unit) {
            return unit.getArmor(Tank.LOC_FRONT) / (float) unit.getOArmor(Tank.LOC_FRONT);
        }

        @Override
        protected float getHealth(SuperHeavyTank unit) {
            return unit.getArmor(SuperHeavyTank.LOC_FRONT) / (float) unit.getOArmor(SuperHeavyTank.LOC_FRONT);
        }

        @Override
        protected float getHealth(Mek unit) {
            return (unit.getArmor(Mek.LOC_CT) + unit.getArmor(Mek.LOC_LT) + unit.getArmor(Mek.LOC_RT))
                         / (float) (unit.getOArmor(Mek.LOC_CT) + unit.getOArmor(Mek.LOC_LT) + unit.getOArmor(Mek.LOC_RT));
        }

        @Override
        protected float getHealth(ProtoMek unit) {
            return unit.getArmor(ProtoMek.LOC_BODY) / (float) unit.getOArmor(ProtoMek.LOC_BODY);
        }

        @Override
        protected float getHealth(Jumpship unit) {
            return unit.getArmor(Jumpship.LOC_NOSE) / (float) unit.getOArmor(Jumpship.LOC_NOSE);
        }

        @Override
        protected float getHealth(SpaceStation unit) {
            return unit.getArmor(SpaceStation.LOC_NOSE) / (float) unit.getOArmor(SpaceStation.LOC_NOSE);
        }

        @Override
        protected float getHealth(Warship unit) {
            return unit.getArmor(Warship.LOC_NONE) / (float) unit.getOArmor(Warship.LOC_NOSE);
        }
    }

    private static class LeftSide extends OverallArmor {

        @Override
        protected float getHealth(Aero unit) {
            return unit.getArmor(Aero.LOC_LWING) / (float) unit.getOArmor(Aero.LOC_LWING);
        }

        @Override
        protected float getHealth(Tank unit) {
            return unit.getArmor(Tank.LOC_LEFT) / (float) unit.getOArmor(Tank.LOC_LEFT);
        }

        @Override
        protected float getHealth(SuperHeavyTank unit) {
            return (unit.getArmor(SuperHeavyTank.LOC_FRONTLEFT) + unit.getArmor(SuperHeavyTank.LOC_REARLEFT))/ (float) (unit.getOArmor(SuperHeavyTank.LOC_FRONTLEFT) + unit.getOArmor(SuperHeavyTank.LOC_REARLEFT));
        }

        @Override
        protected float getHealth(Mek unit) {
            return (unit.getArmor(Mek.LOC_LT) + unit.getArmor(Mek.LOC_LARM) + unit.getArmor(Mek.LOC_LLEG))
                         / (float) (unit.getOArmor(Mek.LOC_LT) + unit.getOArmor(Mek.LOC_LARM) + unit.getOArmor(Mek.LOC_LLEG));
        }

        @Override
        protected float getHealth(Jumpship unit) {
            return (unit.getArmor(Jumpship.LOC_FLS) + unit.getArmor(Jumpship.LOC_ALS)) / (float) (unit.getOArmor(Jumpship.LOC_FLS) + unit.getOArmor(Jumpship.LOC_ALS));
        }

        @Override
        protected float getHealth(SpaceStation unit) {
            return (unit.getArmor(SpaceStation.LOC_FLS) + unit.getArmor(SpaceStation.LOC_ALS)) / (float) (unit.getOArmor(SpaceStation.LOC_FLS) + unit.getOArmor(SpaceStation.LOC_ALS));
        }

        @Override
        protected float getHealth(Warship unit) {
            return (unit.getArmor(Warship.LOC_FLS) + unit.getArmor(Warship.LOC_ALS) + unit.getArmor(Warship.LOC_LBS)) / (float) (unit.getOArmor(Warship.LOC_FLS) + unit.getOArmor(Warship.LOC_ALS) + unit.getOArmor(Warship.LOC_LBS));
        }

    }

    private static class RightSide extends OverallArmor {

        @Override
        protected float getHealth(Aero unit) {
            return unit.getArmor(Aero.LOC_RWING) / (float) unit.getOArmor(Aero.LOC_RWING);
        }

        @Override
        protected float getHealth(Tank unit) {
            return unit.getArmor(Tank.LOC_RIGHT) / (float) unit.getOArmor(Tank.LOC_RIGHT);
        }

        @Override
        protected float getHealth(SuperHeavyTank unit) {
            return (unit.getArmor(SuperHeavyTank.LOC_FRONTRIGHT) + unit.getArmor(SuperHeavyTank.LOC_REARRIGHT)) / (float) (unit.getOArmor(SuperHeavyTank.LOC_FRONTRIGHT) + unit.getOArmor(SuperHeavyTank.LOC_REARRIGHT));
        }

        @Override
        protected float getHealth(Mek unit) {
            return (unit.getArmor(Mek.LOC_RT) + unit.getArmor(Mek.LOC_RARM) + unit.getArmor(Mek.LOC_RLEG))
                         / (float) (unit.getOArmor(Mek.LOC_RT) + unit.getOArmor(Mek.LOC_RARM) + unit.getOArmor(Mek.LOC_RLEG));
        }

        @Override
        protected float getHealth(Jumpship unit) {
            return (unit.getArmor(Jumpship.LOC_FRS) + unit.getArmor(Jumpship.LOC_ARS)) / (float) (unit.getOArmor(Jumpship.LOC_FRS) + unit.getOArmor(Jumpship.LOC_ARS));
        }

        @Override
        protected float getHealth(SpaceStation unit) {
            return (unit.getArmor(SpaceStation.LOC_FRS) + unit.getArmor(SpaceStation.LOC_ARS)) / (float) (unit.getOArmor(SpaceStation.LOC_FRS) + unit.getOArmor(SpaceStation.LOC_ARS));
        }

        @Override
        protected float getHealth(Warship unit) {
            return (unit.getArmor(Warship.LOC_FRS) + unit.getArmor(Warship.LOC_ARS) + unit.getArmor(Warship.LOC_RBS)) / (float) (unit.getOArmor(Warship.LOC_FRS) + unit.getOArmor(Warship.LOC_ARS) + unit.getOArmor(Warship.LOC_RBS));
        }
    }
}
