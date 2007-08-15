/**
 * 
 */
package megamek.client.commands;

import megamek.client.Client;
import megamek.client.ui.swing.MovementDisplay;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IEntityMovementMode;
import megamek.common.MovePath;

/**
 * @author dirk
 *
 */
public class MoveCommand extends ClientCommand {
    
    public static final int GEAR_LAND = 0;
    public static final int GEAR_BACKUP = 1;
    public static final int GEAR_JUMP = 2;
    public static final int GEAR_CHARGE = 3;
    public static final int GEAR_DFA = 4;
    public static final int GEAR_TURN = 5;
    public static final int GEAR_SWIM = 6;
    
    // considering movement data
    private MovePath cmd;
    // considered entity
    private Entity entity;
    int gear;

    public MoveCommand(Client client) {
        super(client, "move", "Move your units.");
    }
    
    public String run(String[] args) {
        if(args.length > 1) {
            Coords target = null;
            if(args[1].equalsIgnoreCase("ABORT")) {
                entity = null;
                clearAllMoves();
                return "Move aborted, all movement data cleared.";
            } else if(args[1].equalsIgnoreCase("COMMIT")) {
                moveTo(cmd);
                return "Move sent.";
            } else if(args[1].equalsIgnoreCase("SELECT")) {
                try {
                    clearAllMoves();
                    int id = Integer.parseInt(args[2]);
                    entity = client.getEntity(id);
                    cmd = new MovePath(client.game, entity);
                    
                    return "Entity " + entity.toString() + " selected for movement.";
                } catch(Exception e) {
                    return "Not an entity ID or valid number." + e.toString();
                }
            } else if(args[1].equalsIgnoreCase("JUMP")) {
                clearAllMoves();
                if (!cmd.isJumping()) {
                    cmd.addStep(MovePath.STEP_START_JUMP);
                }
                gear = GEAR_JUMP;
                
                return "Entity " + entity.toString() + " is going to jump.";
            } else if(args[1].equalsIgnoreCase("BACK")) {
                if (gear == MovementDisplay.GEAR_JUMP) {
                    clearAllMoves();
                }
                gear = GEAR_BACKUP;
            } else if(args[1].equalsIgnoreCase("WALK")) {
                if (gear == MovementDisplay.GEAR_JUMP) {
                    clearAllMoves();
                }
                gear = GEAR_LAND;
            } else if(args[1].equalsIgnoreCase("TURN")) {
                gear = GEAR_TURN;
            } else if(args[1].equalsIgnoreCase("CLIP")) {
                cmd.clipToPossible();
                return "Path cliped to whats actually possible. " + entity.toString() + " is now in gear " + gearName(gear) + " heading towards " + cmd.getFinalCoords().toFriendlyString() + " with a final facing of " + getDirection(cmd.getFinalFacing()) + ". Total mp used: " + cmd.getMpUsed() + " for a movement of: " + cmd.getHexesMoved();
            } else if(args[1].equalsIgnoreCase("GETUP")) {
                if (cmd.getFinalProne() || cmd.getFinalHullDown()) {
                    cmd.addStep(MovePath.STEP_GET_UP);
                    return "Mech will try to stand up. this requieres a piloting roll.";
                }
                
                return "Trying to get up but the mech is not prone.";
            } else {
                target = new Coords(Integer.parseInt(args[1]) - 1, Integer.parseInt(args[2]) - 1);
            }
            
            if(target == null && args.length > 3) {
               target = new Coords(Integer.parseInt(args[2]) - 1, Integer.parseInt(args[3]) - 1);
            }
                
            currentMove(target);
            
            return "Commands accepted " + entity.toString() + " is now in gear " + gearName(gear) + " heading towards " + cmd.getFinalCoords().toFriendlyString() + ". Total mp used: " + cmd.getMpUsed() + " for a movement of: " + cmd.getHexesMoved();
        }
        clearAllMoves();
        return "No arguments given, or there was an error parsing the arguments. All movement data cleared.";
    }
    
    /**
     * Returns new MovePath for the currently selected movement type
     */
    private void currentMove(Coords dest) {
        if(dest != null) {
            if (gear == GEAR_TURN) {
                cmd.rotatePathfinder(cmd.getFinalCoords().direction(dest));
            } else if (gear == GEAR_LAND || gear == GEAR_JUMP) {
                cmd.findPathTo(dest, MovePath.STEP_FORWARDS);
            } else if (gear == GEAR_BACKUP) {
                cmd.findPathTo(dest, MovePath.STEP_BACKWARDS);
            } else if (gear == GEAR_CHARGE) {
                cmd.findPathTo(dest, MovePath.STEP_CHARGE);
            } else if (gear == GEAR_DFA) {
                cmd.findPathTo(dest, MovePath.STEP_DFA);
            } else if (gear == GEAR_SWIM) {
                cmd.findPathTo(dest, MovePath.STEP_SWIM);
            }
        }
    }
    
    private String gearName(int gear) {
        if (gear == GEAR_TURN) {
            return "turning";
        } else if (gear == GEAR_LAND) {
            return "walking";
        } else if (gear == GEAR_BACKUP) {
            return "backup";
        } else if (gear == GEAR_CHARGE) {
            return "charging";
        } else if (gear == GEAR_DFA) {
            return "death from aboveing";
        } else if (gear == GEAR_SWIM) {
            return "swiming";
        } else if(gear == GEAR_JUMP) {
            return "jumping";
        }
        
        return "Unknown";
    }
    

    /**
     * Clears out the currently selected movement data and
     * resets it.
     */
    private void clearAllMoves() {
        //switch back from swimming to normal mode.
        if(entity != null){
            if (entity.getMovementMode() == IEntityMovementMode.BIPED_SWIM)
                entity.setMovementMode(IEntityMovementMode.BIPED);
            else if (entity.getMovementMode() == IEntityMovementMode.QUAD_SWIM)
                entity.setMovementMode(IEntityMovementMode.QUAD);

            cmd = new MovePath(client.game, entity);
        } else {
            cmd = null;
        }
        
        gear = GEAR_LAND;
    }
    
    /**
     * Sends a data packet indicating the chosen movement.
     */
    private synchronized void moveTo(MovePath md) {
        md.clipToPossible();
        
        if (entity.hasUMU()) {
            client.sendUpdateEntity(entity);
        }
        client.moveEntity(entity.getId(), md);
    }
}
