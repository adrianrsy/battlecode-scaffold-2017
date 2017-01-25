package testplayer;
import battlecode.common.*;
import testplayer.RobotPlayer;

public strictfp class Archon {
    static RobotController rc;
    
    //Phase turn limits
    static int PHASE_1_TURN_LIMIT = 200;
    
    //Limits for active gardener production, after some number of turn gardeners may stay behind and 
    //become inactive, a.k.a plant trees and stall
    static int PHASE_1_ACTIVE_GARDENER_LIMIT = 6;
    static int PHASE_2_ACTIVE_GARDENER_LIMIT = 10;
    
    //Direction variables
    static Direction NORTH = Direction.getNorth();
    static Direction SOUTH = Direction.getSouth();
    static Direction EAST = Direction.getEast();
    static Direction WEST = Direction.getWest();
    static Direction NORTH_EAST = new Direction((float) ((NORTH.radians + EAST.radians)/2.0));
    static Direction NORTH_WEST = new Direction((float) ((NORTH.radians + WEST.radians)/2.0));
    static Direction SOUTH_EAST = new Direction((float) ((SOUTH.radians + EAST.radians)/2.0));
    static Direction SOUTH_WEST = new Direction((float) ((SOUTH.radians + WEST.radians)/2.0));
    
    /**
     * For Phase 1<br>
     * In order, each turn an archon will attempt to move towards a corner, signal its movement direction and location, <br>
     * signal a target if there is an obstacle (neutral trees/enemy robots) preventing it from moving, <br>
     * build gardeners and increment the number of gardeners behind it if possible until a limit is reached, <br> 
     * then sense broadcasting enemies and attempt to predict and broadcast enemy archon location. (not implemented)<br>
     *<br>
     *
     * Channels:<br>
     * <ul>
     * <li>1 - 3 -> Phase number
     * <li>4 - 6 -> Archon movement direction
     * <li>7 - 9 -> Archon location x
     * <li>10-12 -> Archon location y
     * <li>13-15 -> Living Gardener channel
     * <li>16-18 -> Immediate Target x
     * <li>19-21 -> Immediate Target y
     * <li>22-24 -> Target type (1 for tree, 2 for robot)
     * <li>25-27 -> Target id 
     * <li>1000  -> round number
     * </ul>
     *<br>
     *
     * Movement:<br>
     * <ul>
     * <li>If there is only one archon, it will attempt to move southwest.
     * <li>If there are two archons, the first will move southwest, the second will move southeast.
     * <li>If there are three archons, first will move southwest, second will move southeast, third will head northwest.
     * </ul>
     *<br>
     *
     * Signal movement direction:<br>
     * Each archon will signal the direction it moved in radians.<br>
     * Spawned units will follow this direction and will be spawned approximately opposite this direction<br>
     */
    
    static void runArchonPhase1() throws GameActionException{
        System.out.println("I'm an archon!");
        int archonNum;
        int phaseNum = 1;
        
        //If first channel has not yet been assigned, this must be the first archon
        if(rc.readBroadcast(RobotPlayer.PHASE_NUMBER_CHANNEL*3 + 1) != 1){
            rc.broadcast(RobotPlayer.PHASE_NUMBER_CHANNEL*3 + 1, phaseNum);
            archonNum = 1;
        }
        //If second channel has not yet been assigned, this must be the second archon
        else if (rc.readBroadcast(RobotPlayer.PHASE_NUMBER_CHANNEL*3 + 2) != 1){
            rc.broadcast(RobotPlayer.PHASE_NUMBER_CHANNEL*3 + 2, 1);
            archonNum =2;
        }
        //Otherwise, it is the third archon
        else{
            rc.broadcast(RobotPlayer.PHASE_NUMBER_CHANNEL*3 + 3, 1);
            archonNum = 3;
        }
        
        int numRoundsRemaining;
        if(archonNum == 1){
            numRoundsRemaining = rc.getRoundLimit() - rc.getRoundNum();
            //rc.broadcast(RobotPlayer.ROUND_NUMBER_CHANNEL, numRoundsRemaining);
        }
        else{
            //numRoundsRemaining = rc.readBroadcast(RobotPlayer.ROUND_NUMBER_CHANNEL);
        }
        
        Direction headedTo;
        switch(archonNum){
        case 1:
            headedTo = SOUTH_WEST;
            break;
        case 2:
            headedTo = SOUTH_EAST;
            break;
        case 3:
            headedTo = NORTH_WEST;
            break;
        default:
            System.out.println("Invalid archon number");
            headedTo = SOUTH_WEST;
            break;
        }
        int turnCount = 0;
        while(true){
            try{
                rc.broadcast(RobotPlayer.ARCHON_DIRECTION_RADIANS_CHANNEL*3 + archonNum, (int) (headedTo.radians*RobotPlayer.CONVERSION_OFFSET));
                boolean hasMoved = RobotPlayer.moveTowards(headedTo);
                MapLocation loc = rc.getLocation();
                rc.broadcast(RobotPlayer.ARCHON_LOCATION_X_CHANNEL*3 + archonNum, (int) (loc.x*RobotPlayer.CONVERSION_OFFSET));
                rc.broadcast(RobotPlayer.ARCHON_LOCATION_Y_CHANNEL*3 + archonNum, (int) (loc.y*RobotPlayer.CONVERSION_OFFSET));
                if(!hasMoved){
                    TreeInfo[] nearbyTrees = rc.senseNearbyTrees();
                    boolean obstacleIsTree = false;
                    for(TreeInfo nearbyTree: nearbyTrees){
                        MapLocation treeLoc = nearbyTree.getLocation();
                        if(!nearbyTree.getTeam().equals(rc.getTeam()) &&
                           loc.distanceTo(treeLoc) < RobotType.ARCHON.strideRadius &&
                           loc.directionTo(treeLoc).degreesBetween(headedTo) < 45){
                            if(rc.canShake(treeLoc)) rc.shake(treeLoc);
                            rc.broadcast(RobotPlayer.IMMEDIATE_TARGET_X_CHANNEL*3 + archonNum, (int) (treeLoc.x*RobotPlayer.CONVERSION_OFFSET));
                            rc.broadcast(RobotPlayer.IMMEDIATE_TARGET_Y_CHANNEL*3 + archonNum, (int) (treeLoc.y*RobotPlayer.CONVERSION_OFFSET));
                            rc.broadcast(RobotPlayer.TARGET_TYPE*3 + archonNum, RobotPlayer.TARGET_IS_TREE);
                            rc.broadcast(RobotPlayer.TARGET_ID*3 + archonNum, nearbyTree.getID());
                            obstacleIsTree = true;
                            break;
                        }
                    }
                    if(!obstacleIsTree){
                        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
                        for(RobotInfo nearbyRobot: nearbyRobots){
                            MapLocation treeLoc = nearbyRobot.getLocation();
                            if(!nearbyRobot.getTeam().equals(rc.getTeam()) &&
                               loc.distanceTo(treeLoc) < RobotType.ARCHON.strideRadius &&
                               loc.directionTo(treeLoc).degreesBetween(headedTo) < 45){
                                rc.broadcast(RobotPlayer.IMMEDIATE_TARGET_X_CHANNEL*3 + archonNum, (int) (treeLoc.x*RobotPlayer.CONVERSION_OFFSET));
                                rc.broadcast(RobotPlayer.IMMEDIATE_TARGET_Y_CHANNEL*3 + archonNum, (int) (treeLoc.y*RobotPlayer.CONVERSION_OFFSET));
                                rc.broadcast(RobotPlayer.TARGET_TYPE*3 + archonNum, RobotPlayer.TARGET_IS_ROBOT);
                                rc.broadcast(RobotPlayer.TARGET_ID*3 + archonNum, nearbyRobot.getID());
                                break;
                            }
                        }
                    }   
                }
                int numActiveGardeners = rc.readBroadcast(RobotPlayer.LIVING_GARDENERS_CHANNEL*3 + archonNum);
                if (numActiveGardeners < PHASE_1_ACTIVE_GARDENER_LIMIT){
                    boolean hasHired = tryHireGardener(headedTo.opposite(), 90, 6);
                    if(hasHired){
                        rc.broadcast(RobotPlayer.LIVING_GARDENERS_CHANNEL*3 + archonNum, numActiveGardeners + 1);
                    }
                }
                
                turnCount+=1;
                if(turnCount > PHASE_1_TURN_LIMIT){
                    rc.broadcast(RobotPlayer.PHASE_NUMBER_CHANNEL*3 + archonNum, 2);
                    break;
                }
            }catch (Exception e) {
            System.out.println("Archon Exception");
            e.printStackTrace();
            } 
        }
        runArchonPhase2();
    }
    /**
     * For Phase 2<br>
     * An archon will increment a counter used as a turn counter for the gardeners. <br>
     * It will try to build gardeners whenever the number of unsettled gardeners drops below a limit 
     * (using the living gardener channel), and each gardener above the limit halves the chance of it 
     * continuing to build gardeners.<br>
     * @throws GameActionException
     */
    static void runArchonPhase2() throws GameActionException{
    
    }
    
    static boolean tryHireGardener(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException{
        if(rc.getBuildCooldownTurns() > 0 || rc.getTeamBullets() < RobotType.GARDENER.bulletCost){
            return false;
        }
        if(rc.canHireGardener(dir)){
            rc.hireGardener(dir);
            return true;
        }
        // Now try a bunch of similar angles
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            if(rc.canHireGardener(dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
                rc.hireGardener(dir.rotateLeftDegrees(degreeOffset*currentCheck));
                return true;
            }
            // Try the offset on the right side
            if(rc.canHireGardener(dir.rotateRightDegrees(degreeOffset*currentCheck))) {
                rc.hireGardener(dir.rotateRightDegrees(degreeOffset*currentCheck));
                return true;
            }
            // No hire performed, try slightly further
            currentCheck++;
        }

        // A hire never happened, so return false.
        return false;
    }

}
