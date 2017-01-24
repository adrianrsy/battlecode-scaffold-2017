package testplayer;
import battlecode.common.*;
import testplayer.RobotPlayer;

public strictfp class Gardener {
    static RobotController rc;
    
    //Active turn limit
    static int PHASE_1_ACTIVE_TURN_LIMIT = 60;
    
    //Turns it will move away until inactive
    static int MOVE_AWAY_TURNS = 5;
    
    /**
     * For Phase 1<br>
     * A gardener will identify which archon group it belongs to via checking the three sets of archon locations and 
     * identifying which it is closest to.<br>
     * For the first 60 turns, it will be an active gardener and move in the direction of its leader archon every other turn 
     * by reading the movement direction of said archon while trying to build other robots behind it for other purposes, 
     * e.g. lumberjacks for terrain clearing, scouts for harassing/scouting, soldiers for basic defense.<br>
     * While active, it will check for the phase number each turn and perform these tasks/switch to a different task.
     * After 60 turns, it will stop being an active gardener by signaling to the broadcast that it is no longer active, 
     * i.e. decrement the active gardener counter. Upon being inactive, it will spend 5 turns moving away from the archon 
     * then planting trees around itself in a hexagonal fashion.<br>
     * When inactive, it will water the adjacent tree with the lowest hp.<br>
     *<br>
     *
     * Channels:<br>
     * <ul>
     * <li>1 - 3 -> Phase number
     * <li>4 - 6 -> Archon movement direction
     * <li>7 - 9 -> Archon location x
     * <li>10-12 -> Archon location y
     * <li>13-15 -> Living Gardener channel
     * </ul>
     *<br>
     *
     * Movement:<br>
     * Moves once every two turns in the same direction as the archon it belongs to
     *<br>
     */
    
    static void runGardenerPhase1() throws GameActionException{
        System.out.println("I'm a gardener!");
        int turnCount = 0;
        MapLocation loc = rc.getLocation();
        int archonNum = RobotPlayer.getNearestArchon();
        float headedToRadians = ((float) rc.readBroadcast(RobotPlayer.ARCHON_DIRECTION_RADIANS_CHANNEL*3 + archonNum))/Archon.CONVERSION_OFFSET;
        Direction headedTo = new Direction(headedToRadians);
        int phaseNum = 1;
        while(turnCount < PHASE_1_ACTIVE_TURN_LIMIT){
            phaseNum = rc.readBroadcast(RobotPlayer.PHASE_NUMBER_CHANNEL*3+archonNum);
            if(turnCount%2 == 0){
                boolean hasMoved = RobotPlayer.moveTowards(headedTo);
            }
            double randomVar = Math.random();
            if(randomVar < 0.4){
                tryBuild(RobotType.LUMBERJACK,headedTo.opposite());
            }
            else if(randomVar < 0.8){
                tryBuild(RobotType.SCOUT, headedTo.opposite());
            }
            else{
                tryBuild(RobotType.SOLDIER, headedTo.opposite());
            }
            turnCount +=1;
            if(phaseNum != 1) {
                break;
            }
            Clock.yield();
        }
        if(phaseNum == 2){
            runGardenerPhase2();
        }
        turnCount = 0;
        int currentActiveGardenerNum = rc.readBroadcast(RobotPlayer.LIVING_GARDENERS_CHANNEL*3+archonNum);
        rc.broadcast(RobotPlayer.LIVING_GARDENERS_CHANNEL*3 + archonNum, currentActiveGardenerNum - 1);
        while(turnCount < MOVE_AWAY_TURNS){
            RobotPlayer.moveTowards(headedTo.opposite());
            Clock.yield();
        }
        while(true){
            tryPlantHexagonal();
            TreeInfo[] nearbyTeamTrees = rc.senseNearbyTrees((float) 2.5, rc.getTeam());
            int minHPTreeID = 0; //arbitrary id number
            float minHP = 50; //maximal amount of hp
            for(int i = 0; i<Math.max(6, nearbyTeamTrees.length); i++){
                if(rc.canWater(nearbyTeamTrees[i].getID()) && nearbyTeamTrees[i].getHealth()<minHP){
                    minHPTreeID = nearbyTeamTrees[i].getID();
                    minHP = nearbyTeamTrees[i].getHealth();
                }
            }
            if(rc.canWater(minHPTreeID)) 
                rc.water(minHPTreeID);
            Clock.yield();
        }
    }
    
    static void runGardenerPhase2() throws GameActionException{
    
    }
    
    static void tryPlantHexagonal() throws GameActionException{
        for(int i = 0; i< 6; i++){
            Direction dir = Direction.getEast().rotateLeftDegrees(60*i);
            if(rc.canPlantTree(dir))
                rc.plantTree(dir);
        }
    }
    
    static boolean tryBuild(RobotType robotType, Direction dir, float degreeOffset, int checksPerSide) throws GameActionException{
        if(rc.getBuildCooldownTurns() > 0 || rc.getTeamBullets() < robotType.bulletCost)
            return false;
        if(rc.canBuildRobot(robotType, dir)){
            rc.buildRobot(robotType,dir);
            return true;
        }
        //Now try a bunch of similar angles
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            if(rc.canBuildRobot(robotType,dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
                rc.buildRobot(robotType,dir.rotateLeftDegrees(degreeOffset*currentCheck));
                return true;
            }
            // Try the offset on the right side
            if(rc.canBuildRobot(robotType,dir.rotateRightDegrees(degreeOffset*currentCheck))) {
                rc.buildRobot(robotType,dir.rotateRightDegrees(degreeOffset*currentCheck));
                return true;
            }
            // No hire performed, try slightly further
            currentCheck++;
        }

        // A build never happened, so return false.
        return false;
    }
    
    static void tryBuild(RobotType robotType, Direction dir) throws GameActionException{
        tryBuild(robotType,dir,90,6);
    }


}
