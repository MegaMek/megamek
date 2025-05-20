/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.moves;

/**
 * PhasePassSelector is a selector/factory which will return a static reference to the correct phase pass based on the
 * move step type.
 * @author Luana Coppio
 * @since 0.50.07
 */
class PhasePassSelector {

    private static final PhasePass UNLOAD_DISCONNECT_STEP = new UnloadDisconnectStep();
    private static final PhasePass TOW_LOAD_DROP_CARGO_STEP = new TowLoadDropCargoStep();
    private static final PhasePass TURN_STEP = new TurnStep();
    private static final PhasePass BACKWARD_STEP = new BackwardStep();
    private static final PhasePass FORWARD_STEP = new ForwardStep();
    private static final PhasePass CHARGE_STEP = new ChargeStep();
    private static final PhasePass LATERAL_BACKWARD_STEP = new LateralBackwardStep();
    private static final PhasePass LATERAL_STEP = new LateralStep();
    private static final PhasePass GET_UP_STEP = new GetUpStep();
    private static final PhasePass CAREFUL_STAND_STEP = new CarefulStandStep();
    private static final PhasePass GO_PRONE_STEP = new GoProneStep();
    private static final PhasePass START_JUMP_STEP = new StartJumpStep();
    private static final PhasePass UP_STEP = new UpStep();
    private static final PhasePass DOWN_STEP = new DownStep();
    private static final PhasePass HULL_DOWN_STEP = new HullDownStep();
    private static final PhasePass CLIMB_MODE_STEP = new ClimbModeStep();
    private static final PhasePass SHAKE_OFF_SWARMERS_STEP = new ShakeOffSwarmersStep();
    private static final PhasePass LAND_VLAND_STEP = new LandVLandStep();
    private static final PhasePass ACCELERATION_STEP = new AccelerationStep();
    private static final PhasePass EVADE_STEP = new EvadeStep();
    private static final PhasePass STARTUP_STEP = new StartupStep();
    private static final PhasePass SHUTDOWN_STEP = new ShutdownStep();
    private static final PhasePass SELF_DESTRUCT_STEP = new SelfDestructStep();
    private static final PhasePass ROLL_STEP = new RollStep();
    private static final PhasePass LAUNCH_DROP_STEP = new LaunchDropStep();
    private static final PhasePass THRUST_STEP = new ThrustStep();
    private static final PhasePass YAW_STEP = new YawStep();
    private static final PhasePass HOVER_STEP = new HoverStep();
    private static final PhasePass MANEUVER_STEP = new ManeuverStep();
    private static final PhasePass LOOP_STEP = new LoopStep();
    private static final PhasePass CONVERT_MODE_STEP = new ConvertModeStep();
    private static final PhasePass BOOTLEGGER_STEP = new BootleggerStep();
    private static final PhasePass BRACE_STEP = new BraceStep();
    private static final PhasePass DEFAULT_STEP = new DefaultStep();

    /**
     * Returns the correct phase pass for  {@link MoveStep} compilation based on the move step type.
     * @param moveStepType the {@link megamek.common.moves.MovePath.MoveStepType}
     * @return the phase pass {@link PhasePass} for that specific {@link megamek.common.moves.MovePath.MoveStepType}
     */
    static PhasePass getPhasePass(MovePath.MoveStepType moveStepType) {
        return switch (moveStepType) {
            case FORWARDS, DFA, SWIM -> FORWARD_STEP;
            case BACKWARDS -> BACKWARD_STEP;
            case TURN_LEFT, TURN_RIGHT -> TURN_STEP;
            case TOW, LOAD, DROP_CARGO -> TOW_LOAD_DROP_CARGO_STEP;
            case CHARGE -> CHARGE_STEP;
            case LATERAL_LEFT_BACKWARDS, LATERAL_RIGHT_BACKWARDS -> LATERAL_BACKWARD_STEP;
            case LATERAL_LEFT, LATERAL_RIGHT -> LATERAL_STEP;
            case GET_UP -> GET_UP_STEP;
            case UNLOAD, DISCONNECT -> UNLOAD_DISCONNECT_STEP;
            case CAREFUL_STAND -> CAREFUL_STAND_STEP;
            case GO_PRONE -> GO_PRONE_STEP;
            case START_JUMP -> START_JUMP_STEP;
            case UP -> UP_STEP;
            case DOWN -> DOWN_STEP;
            case HULL_DOWN -> HULL_DOWN_STEP;
            case CLIMB_MODE_ON, CLIMB_MODE_OFF -> CLIMB_MODE_STEP;
            case SHAKE_OFF_SWARMERS -> SHAKE_OFF_SWARMERS_STEP;
            case LAND, VLAND -> LAND_VLAND_STEP;
            case EVADE -> EVADE_STEP;
            case ACCN, DECN, ACC, DEC -> ACCELERATION_STEP;
            case SHUTDOWN -> SHUTDOWN_STEP;
            case STARTUP -> STARTUP_STEP;
            case SELF_DESTRUCT -> SELF_DESTRUCT_STEP;
            case ROLL -> ROLL_STEP;
            case LAUNCH, DROP -> LAUNCH_DROP_STEP;
            case THRUST -> THRUST_STEP;
            case YAW -> YAW_STEP;
            case HOVER -> HOVER_STEP;
            case MANEUVER -> MANEUVER_STEP;
            case LOOP -> LOOP_STEP;
            case CONVERT_MODE -> CONVERT_MODE_STEP;
            case BOOTLEGGER -> BOOTLEGGER_STEP;
            case BRACE -> BRACE_STEP;
            default -> DEFAULT_STEP;
        };
    }
}
