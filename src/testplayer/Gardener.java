package testplayer;
import battlecode.common.*;
import testplayer.RobotPlayer;

public strictfp class Gardener {
    RobotController rc;
    
    public Gardener(RobotController rc){
        this.rc = rc;
    }
    
    //Active turn limit
    int PHASE_1_ACTIVE_TURN_LIMIT = 60;
     
    //Scout limit
    static int LIVING_SCOUT_LIMIT = 15;
    
    //Turns it will move away until inactive
    static int MOVE_AWAY_TURNS = 10;
    
    /**
     * For Phase 1<br>
     * A gardener will identify which archon group it belongs to via checking the three sets of archon locations and 
     * identifying which it is closest to.<br>
     * For the first 60 turns, it will be an active gardener and move away from the direction of its leader archon every turn 
     * by reading the movement direction of said archon while trying to build other robots in front of it for other purposes, 
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
    
    void runGardenerPhase1() throws GameActionException{
        System.out.println("I'm a gardener!");
        MapLocation ownLoc = rc.getLocation();
        int turnCount = 0;
        int archonNum = RobotPlayer.getNearestArchon();
        int phaseNum = 1;
        while(turnCount < PHASE_1_ACTIVE_TURN_LIMIT){
            try{
                Direction headedTo = RobotPlayer.getArchonLoc(archonNum).directionTo(ownLoc);
                phaseNum = rc.readBroadcast(RobotPlayer.PHASE_NUMBER_CHANNEL*3+archonNum);
                //RobotPlayer.moveTowards(headedTo, rc);
                headedTo = tryMoveInGeneralDirection(headedTo, 110, 11);
                double randomVar = Math.random();
                if(randomVar < 0.6){
                    System.out.println("Try build lumberjack");
                    boolean builtLumberjack = tryBuild(RobotType.LUMBERJACK,headedTo);
                    System.out.println("Built Lumberjack: " + builtLumberjack);
                }
                else if(randomVar < 0.75){
                    System.out.println("Try build scout");
                    int numScouts = rc.readBroadcast(RobotPlayer.LIVING_SCOUT_CHANNEL * 3 + archonNum);
                    if(numScouts < LIVING_SCOUT_LIMIT && tryBuild(RobotType.SCOUT, headedTo)){
                        rc.broadcast(RobotPlayer.LIVING_SCOUT_CHANNEL*3 + archonNum, numScouts + 1);
                        System.out.println("Built Scout: " + true);
                    }
                    else
                        System.out.println("Built Scout: " + false);
                }
                else{
                    System.out.println("Try build soldier");
                    if (tryBuild(RobotType.SOLDIER, headedTo))
                        System.out.println("Built Soldier: " + true);
                    else
                        System.out.println("Built Soldier: " + false);
                }
                turnCount +=1;
                if(phaseNum != 1) {
                    break;
                }
                Clock.yield();
            }catch (Exception e) {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            } 
        }
        
        if(phaseNum == 2){
            runGardenerPhase2(archonNum);
        }
        
        turnCount = 0;
        int currentActiveGardenerNum = rc.readBroadcast(RobotPlayer.LIVING_GARDENERS_CHANNEL*3+archonNum);
        rc.broadcast(RobotPlayer.LIVING_GARDENERS_CHANNEL*3 + archonNum, currentActiveGardenerNum - 1);
        while(turnCount < MOVE_AWAY_TURNS){
            RobotInfo[] nearbyTeamMates = rc.senseNearbyRobots(4, rc.getTeam());
            
            Direction headedTo;
            if(nearbyTeamMates.length > 0)
                headedTo = nearbyTeamMates[0].getLocation().directionTo(rc.getLocation());
            else
                headedTo = RobotPlayer.getArchonLoc(archonNum).directionTo(rc.getLocation());
            RobotPlayer.moveTowards(headedTo, rc);
            turnCount ++;
            Clock.yield();
        }
        while(true){
            try{
                tryPlantHexagonal();
                tryWaterHexagonal();
                Clock.yield();
            }catch (Exception e) {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            } 
        }
    }
    
    /**
     * For Phase 2 <br>
     * A gardener will move away from the general direction of the archon until it finds a place where it can 
     * plant at least 4 trees and there is a two unit wide open space after at least 2 of those planned locations.<br>
     * A tank builder will try to build tanks while continuing to move away from the archon.<br>
     * Once it can settle down, it becomes inactive (signalling to the living gardener channel) and it will plant trees 
     * around itself and water the one with the lowest hp.<br>
     * If it has not yet settled down, it will attempt to build scouts/soldiers in the opposite direction of the 
     * archon's location with respect to its current location.<br>
     * 
     * Every 1st of 3 turns, if it is not yet dying, the gardeners will account which is the farthest from the archon
     * by posting a distance and its id if its distance is greater than the previous distances.<br>
     * Every 2nd of 3 turns (maintained by a channel incremented by the archon), each of these gardeners (unsettled) will 
     * read if they are the farthest gardener from their own archon by reading a channel where gardeners post their 
     * distance and id from the archon, if it matches their id, they will serve as a tank builder.<br>
     * Every 3rd of 3 turns, the first gardener to read the channel will clear the channels used to share those messages.<br>
     * If it is dying at any point, it signals that it is dying by decrementing the gardener counter.<br>
     * <br>
     * @throws GameActionException
     */
    void runGardenerPhase2(int archonNum) throws GameActionException{
        MapLocation archonLoc = RobotPlayer.getArchonLoc(archonNum);
        MapLocation ownLoc = rc.getLocation();
        Direction headedTo = archonLoc.directionTo(ownLoc);
        boolean hasBroadcastedDying = false;
        boolean isTankBuilder = false;
        boolean hasSettled = false;
        Direction plantDir = Direction.getEast();
        while(!hasSettled && !hasBroadcastedDying){
            try{
                RobotPlayer.dodge(4);
                headedTo = tryMoveInGeneralDirection(headedTo, 110, 11);
                int gardenerTurnCounter = rc.readBroadcast(RobotPlayer.GARDENER_TURN_COUNTER*3 + archonNum);
                if(isTankBuilder){
                    tryBuild(RobotType.TANK,headedTo);
                }
                else{
                    SettleDirection settle = canSettle(archonNum);
                    if(settle.getCanSettle()){
                        hasSettled = true;
                        plantDir = settle.getDir();
                        int currentLivingGardeners = rc.readBroadcast(RobotPlayer.LIVING_GARDENERS_CHANNEL*3 + archonNum);
                        rc.broadcast(RobotPlayer.LIVING_GARDENERS_CHANNEL*3 + archonNum, currentLivingGardeners -1);
                    }
                    else if(RobotPlayer.isDying()){
                        hasBroadcastedDying = true;
                        int currentLivingGardeners = rc.readBroadcast(RobotPlayer.LIVING_GARDENERS_CHANNEL*3 + archonNum);
                        rc.broadcast(RobotPlayer.LIVING_GARDENERS_CHANNEL*3 + archonNum, currentLivingGardeners -1);
                    }
                    else{
                        double randomVar = Math.random();
                        if(randomVar<0.4){
                            tryBuild(RobotType.SOLDIER,headedTo);
                        }
                        else if(randomVar < 0.7){
                            tryBuild(RobotType.LUMBERJACK, headedTo);
                        }
                        else{
                            tryBuild(RobotType.SCOUT,headedTo);
                        }
                    }
                }
                System.out.println("Gardener turn count is: " + gardenerTurnCounter);
                if(gardenerTurnCounter == 1){
                    float archonDist = rc.getLocation().distanceTo(RobotPlayer.getArchonLoc(archonNum));
                    float currentLargestDist = rc.readBroadcastFloat(RobotPlayer.GARDENER_MAX_DIST_CHANNEL*3 + archonNum);
                    if(archonDist > currentLargestDist){
                        rc.broadcastFloat(RobotPlayer.GARDENER_MAX_DIST_CHANNEL*3 + archonNum, archonDist);
                        rc.broadcast(RobotPlayer.GARDENER_MAX_DIST_ID_CHANNEL*3 + archonNum, rc.getID());
                    }
                }
                if(gardenerTurnCounter == 2){
                    int farthestId = rc.readBroadcast(RobotPlayer.GARDENER_MAX_DIST_ID_CHANNEL*3 + archonNum);
                    if (farthestId == rc.getID()){
                        isTankBuilder = true;
                        System.out.println("I am the tank builder.");
                    }
                    else
                        isTankBuilder = false;   
                }
                if(gardenerTurnCounter == 0){
                    if(rc.readBroadcast(RobotPlayer.GARDENER_MAX_DIST_CHANNEL*3 + archonNum) != -1){
                        rc.broadcast(RobotPlayer.GARDENER_MAX_DIST_CHANNEL*3 + archonNum, -1);
                        rc.broadcast(RobotPlayer.GARDENER_MAX_DIST_ID_CHANNEL*3 + archonNum, -1);
                    }
                }
                Clock.yield();
            }catch(Exception e) {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            } 
        }
        if(hasSettled){
            int turnCount = 0;
            while(true){
                try{
                    turnCount ++;
                    if(turnCount%10 ==0){
                        float vcost = rc.getVictoryPointCost();
                        for(int i=0; i<10; i++){
                            if(rc.getTeamBullets() > vcost){
                                rc.donate(vcost);
                            }
                        }
                    }
                    tryPlantHexagonal(plantDir);
                    tryWaterHexagonal();
                    Clock.yield();
                }catch (Exception e) {
                    System.out.println("Gardener Exception");
                    e.printStackTrace();
                } 
            }
        }
        else{
            int turnCount = 0;
            while(turnCount < MOVE_AWAY_TURNS){
                RobotPlayer.tryMoveInGeneralDirection(headedTo);
                turnCount ++;
                Clock.yield();
            }
            turnCount = 0;
            while(true){
                try{
                    turnCount ++;
                    if(turnCount%10 ==0){
                        float vcost = rc.getVictoryPointCost();
                        for(int i=0; i<10; i++){
                            if(rc.getTeamBullets() > vcost){
                                rc.donate(vcost);
                            }
                        }
                    }
                    tryPlantHexagonal();
                    tryWaterHexagonal();
                    Clock.yield();
                }catch (Exception e) {
                    System.out.println("Gardener Exception");
                    e.printStackTrace();
                } 
            }
        }
        
    }
    
    /**
     * 
     * @param archonNum 
     * @return the direction at which to start planting so at least 4 will fit
     * @throws GameActionException
     */
    SettleDirection canSettle(int archonNum) throws GameActionException{
        if(RobotPlayer.getArchonLoc(archonNum).distanceTo(rc.getLocation()) < 6.5){
            return new SettleDirection(false, RobotPlayer.randomDirection());
        }
        Direction randomDir = RobotPlayer.randomDirection();
        int plantableTrees = 0;
        int hasSpace = 0;
        for(int i = 0; i<6; i++){
            Direction plantDir = randomDir.rotateLeftDegrees(60*i);
            if(rc.canPlantTree(plantDir)){
                plantableTrees ++;
                if(!rc.isLocationOccupied(rc.getLocation().add(plantDir, 4)) &&
                   !rc.isLocationOccupied(rc.getLocation().add(plantDir, 5))){
                    hasSpace ++;
                }
            }
        }
        if(plantableTrees < 4){
            return new SettleDirection(false,randomDir);
        }
        else if(hasSpace <2){
            return new SettleDirection(false,randomDir);
        }
        else{
            return new SettleDirection(true, randomDir);
        }
    }
    
    private class SettleDirection{
        boolean canSettle;
        Direction dir;
        
        public SettleDirection(boolean canSettle, Direction dir){
            this.canSettle = canSettle;
            this.dir = dir;
        }
        public boolean getCanSettle(){
            return canSettle;
        }
        public Direction getDir(){
            return dir;
        }
    }
    
    /**
     * Tries to plant trees in a hexagonal formation around itself.
     * @return true if a tree is planted
     * @throws GameActionException
     */
    boolean tryPlantHexagonal() throws GameActionException{
        for(int i = 0; i< 6; i++){
            Direction dir = Direction.getEast().rotateLeftDegrees(60*i);
            if(rc.canPlantTree(dir)){
                rc.plantTree(dir);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Tries to plant trees in a hexagonal formation around itself.
     * @throws GameActionException
     */
    void tryPlantHexagonal(Direction givenDir) throws GameActionException{
        for(int i = 0; i< 6; i++){
            Direction dir = givenDir.rotateLeftDegrees(60*i);
            if(rc.canPlantTree(dir))
                rc.plantTree(dir);
        }
    }
    
    /**
     * Tries watering at most 6 nearby bullet trees of own team.
     * @throws GameActionException
     */
    void tryWaterHexagonal() throws GameActionException{
        TreeInfo[] nearbyTeamTrees = rc.senseNearbyTrees((float) 4, rc.getTeam());
        int minHPTreeID = 0; //arbitrary id number
        float minHP = GameConstants.BULLET_TREE_MAX_HEALTH; //maximal amount of hp
        for(int i = 0; i<Math.min(6, nearbyTeamTrees.length); i++){
            if(rc.canWater(nearbyTeamTrees[i].getID()) && nearbyTeamTrees[i].getHealth()<minHP){
                minHPTreeID = nearbyTeamTrees[i].getID();
                minHP = nearbyTeamTrees[i].getHealth();
            }
        }
        if(rc.canWater(minHPTreeID)) 
            rc.water(minHPTreeID);
    }
    
    /**
     * Tries to build a certain robot type around a certain direction
     * @param robotType
     * @param dir
     * @param degreeOffset
     * @param checksPerSide
     * @return true if it is successfully built
     * @throws GameActionException
     */
    boolean tryBuild(RobotType robotType, Direction dir, float degreeOffset, int checksPerSide) throws GameActionException{
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
    
    /**
     * Tries to build a certain robot type around a certain direction
     * @param robotType
     * @param dir
     * @throws GameActionException
     */
    boolean tryBuild(RobotType robotType, Direction dir) throws GameActionException{
         return tryBuild(robotType,dir,90,6);
    }
    
    /**
     * Attempts to move randomly in the general direction of dir at most degreeOffset away, trying to move
     * numChecks times
     * @param dir direction to attempt moving towards
     * @param degreeOffset 
     * @param numChecks
     * @return the direction it ended up heading towards.
     * @throws GameActionException 
     */
    Direction tryMoveInGeneralDirection(Direction dir, float degreeOffset, int numChecks) throws GameActionException{
        if(rc.hasMoved()){
            return dir;
        }
        int attempts = 0;
        while(attempts < numChecks){
            float multiplier = (float) (2*Math.random()) - 1;
            Direction randomDir = dir.rotateLeftDegrees(multiplier * degreeOffset);
            if(!rc.hasMoved() && rc.canMove(randomDir)){
                rc.move(randomDir);
                return randomDir;
            }
            attempts +=1;
        }
        return dir;
    }


}
