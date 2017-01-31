package testplayer;
import battlecode.common.*;

/*
 * TODO:
 * Add limit for number of scouts (needs to be fixed) (not yet tested)
 * If enemy archon dies, needs to give new target for robots. (done)
 * Make lumberjacks during phase 2 just to cut down gardener clumps? (not yet implemented)
 * Make gardeners move away from archon before planting tree clusters. (implemented for phase 1, doesn't seem to work very well 
 * for phase 2)
 * Make gardeners move away from closest friendly robot (at least 2 unites edge to edge) before trying to plant trees down
 * Fix lumberjack (it doesn't stay to attack trees)
 * Movement fails really badly if they try going towards a fixed direction then get stuck
 * Purchasing of victory points does not seem to be working
 */

public strictfp class RobotPlayer {
    static RobotController rc;
    
    //For channel numbers, get channel number, multiply by 3, then add archon number (from 1 to 3)
    static final int PHASE_NUMBER_CHANNEL = 0;
    static final int ARCHON_DIRECTION_RADIANS_CHANNEL = 1;
    static final int ARCHON_LOCATION_X_CHANNEL = 2;
    static final int ARCHON_LOCATION_Y_CHANNEL = 3;
    static final int LIVING_GARDENERS_CHANNEL = 4;
    static final int IMMEDIATE_TARGET_X_CHANNEL = 5;
    static final int IMMEDIATE_TARGET_Y_CHANNEL = 6;
    static final int TARGET_TYPE = 7; //1 for tree, 2 for robot
    static final int TARGET_ID = 8;
    static final int ENEMY_ARCHON_ID_CHANNEL = 9; //All three to be read by all shooting units at the start of the turn
    static final int ENEMY_ARCHON_X_CHANNEL = 10;
    static final int ENEMY_ARCHON_Y_CHANNEL = 11;
    static final int GARDENER_TURN_COUNTER = 12;
    static final int GARDENER_MAX_DIST_CHANNEL = 13;
    static final int GARDENER_MAX_DIST_ID_CHANNEL = 14;
    static final int LIVING_SCOUT_CHANNEL = 15;
    
    
    static final int ENEMY_ROBOT_CHANNEL_1 = 16;
    static final int NUM_TARGETING_ROBOT_CHANNEL_1 = 17;
    static final int ENEMY_ROBOT_1_X_CHANNEL = 18;
    static final int ENEMY_ROBOT_1_Y_CHANNEL = 19;
    
    static final int ENEMY_ROBOT_CHANNEL_2 = 20;
    static final int NUM_TARGETING_ROBOT_CHANNEL_2 = 21;
    static final int ENEMY_ROBOT_2_X_CHANNEL = 22;
    static final int ENEMY_ROBOT_2_Y_CHANNEL = 23;
       
    static final int TREE_TARGET_CHANNEL_1 = 24;
    static final int NUM_TARGETING_TREE_CHANNEL_1 = 25;
    static final int ENEMY_TREE_1_X_CHANNEL = 26;
    static final int ENEMY_TREE_1_Y_CHANNEL = 27;
    
    static final int TREE_TARGET_CHANNEL_2 = 28;
    static final int NUM_TARGETING_TREE_CHANNEL_2 = 29;
    static final int ENEMY_TREE_2_X_CHANNEL = 30;
    static final int ENEMY_TREE_2_Y_CHANNEL = 31;
    
    static final int TARGET_IS_TREE = 1;
    static final int TARGET_IS_ROBOT = 2;
    
    static final double DYING_GARDENER_HP_THRESHOLD = 0.19 * RobotType.GARDENER.maxHealth;
    static final double DYING_SOLDIER_HP_THRESHOLD = 0.19 * RobotType.SOLDIER.maxHealth;
    static final double DYING_LUMBERJACK_HP_THRESHOLD = 0.19 * RobotType.LUMBERJACK.maxHealth;
    static final double DYING_TANK_HP_THRESHOLD = 0.19 * RobotType.TANK.maxHealth;
    static final double DYING_SCOUT_HP_THRESHOLD = 0.19 * RobotType.SCOUT.maxHealth;
    static final int MAX_ARCHONS = 3;
    static final int VICTORY_POINTS_TO_WIN = 1000;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        // Here, we've separated the controls into a different method for each RobotType.
        // You can add the missing ones or rewrite this into your own control structure.
        switch (rc.getType()) {
            case ARCHON:
                Archon archon = new Archon(rc);
                archon.runArchonPhase1();
                break;
            case GARDENER:
                Gardener gardener = new Gardener(rc);
                gardener.runGardenerPhase1();
                break;
            case SOLDIER:
                Soldier soldier = new Soldier(rc);
                soldier.runSoldier();
                break;
            case LUMBERJACK:
                Lumberjack lumberjack = new Lumberjack(rc);
                lumberjack.runLumberjack();
                break;
            case TANK:
                Tank tank = new Tank(rc);
                tank.runTank();
            case SCOUT:
                Scout scout = new Scout(rc);
                scout.runScout();
        }
    }
    
    /**
     * Allows the robot to the archon it is closest to
     * @return the archon closest to the robot running getNearestArchon
     * @throws GameActionException
     */
    static int getNearestArchon() throws GameActionException {
        MapLocation loc = rc.getLocation();
        int archonNum = 1;
        float min_distance = Float.MAX_VALUE;
        for(int i = 1; i <= rc.getInitialArchonLocations(rc.getTeam()).length; i++){
            float archonX = rc.readBroadcastFloat(ARCHON_LOCATION_X_CHANNEL*3+i);
            float archonY = rc.readBroadcastFloat(ARCHON_LOCATION_Y_CHANNEL*3+i);
            MapLocation archonLoc = new MapLocation(archonX, archonY);
            float dist = loc.distanceTo(archonLoc);
            if(dist < min_distance){
                min_distance = dist;
                archonNum = i;
            }
        }
        return archonNum;
    }
    
    /**
     * Reads the broadcast channel to get the location of a specific archon
     * @param archonNum
     * @return MapLocation of archon
     * @throws GameActionException
     */
    static MapLocation getArchonLoc(int archonNum) throws GameActionException{
        float archonX = rc.readBroadcastFloat(ARCHON_LOCATION_X_CHANNEL*3+archonNum);
        float archonY = rc.readBroadcastFloat(ARCHON_LOCATION_Y_CHANNEL*3+archonNum);
        return new MapLocation(archonX, archonY);
    }
    
    /**
     * Reads the broadcast channel to get the direction a specific archon is headed towards
     * @param archonNum
     * @return Direction of archon
     * @throws GameActionException
     */
    static Direction getArchonDirection(int archonNum) throws GameActionException{
        float headedToRadians = rc.readBroadcastFloat(ARCHON_DIRECTION_RADIANS_CHANNEL*3 + archonNum);
        return new Direction(headedToRadians);
    }
    
    /**
     * Adds locations of neutral and enemy trees to broadcasting channels
     * @param archonNum 
     * @return true if an update happened
     * @throws GameActionException
     */
    static boolean updateTreeLocs(int archonNum) throws GameActionException{
        TreeInfo[] nearbyNeutralTrees = rc.senseNearbyTrees(-1,Team.NEUTRAL);
        TreeInfo[] nearbyEnemyTrees = rc.senseNearbyTrees(-1, rc.getTeam().opponent());
        TreeInfo[] nearbyTrees;
        if(nearbyNeutralTrees.length > 0 && nearbyEnemyTrees.length > 0){
            float dist1 = rc.getLocation().distanceTo(nearbyNeutralTrees[0].getLocation());
            float dist2 = rc.getLocation().distanceTo(nearbyEnemyTrees[0].getLocation());
            if(dist1 < dist2)
                nearbyTrees = new TreeInfo[] {nearbyNeutralTrees[0]};
            else
                nearbyTrees = new TreeInfo[] {nearbyEnemyTrees[0]};
        }
        else if(nearbyNeutralTrees.length > 0){
            nearbyTrees = nearbyNeutralTrees; 
        }
        else{
            nearbyTrees = nearbyEnemyTrees;
        }
        
        if(nearbyTrees.length > 0){
            int treeChannel1 = rc.readBroadcast(RobotPlayer.TREE_TARGET_CHANNEL_1*3 + archonNum);
            int treeChannel2 = rc.readBroadcast(RobotPlayer.TREE_TARGET_CHANNEL_2*3 + archonNum);
            int treeId = nearbyTrees[0].getID();
            if( treeChannel1 != treeId && treeChannel2 != treeId){
                MapLocation treeLoc = nearbyTrees[0].getLocation();
                if(treeChannel2 == -1){
                    rc.broadcast(RobotPlayer.TREE_TARGET_CHANNEL_2*3 + archonNum, treeId);
                    rc.broadcastFloat(RobotPlayer.ENEMY_TREE_2_X_CHANNEL*3 + archonNum, treeLoc.x);
                    rc.broadcastFloat(RobotPlayer.ENEMY_TREE_2_Y_CHANNEL*3 + archonNum, treeLoc.y);
                }
                else{
                    rc.broadcast(RobotPlayer.TREE_TARGET_CHANNEL_1*3 + archonNum, treeId);
                    rc.broadcastFloat(RobotPlayer.ENEMY_TREE_1_X_CHANNEL*3 + archonNum, treeLoc.x);
                    rc.broadcastFloat(RobotPlayer.ENEMY_TREE_1_Y_CHANNEL*3 + archonNum, treeLoc.y);
                }
                return true;
            }
        }
        return false;
    }
    
    /**
     * Updates the tree channels if the given locations are within sensing range.
     * @param archonNum
     * @throws GameActionException
     */
    static void clearTreeLocs(int archonNum) throws GameActionException{
        MapLocation treeLoc1 = new MapLocation(rc.readBroadcastFloat(ENEMY_TREE_1_X_CHANNEL*3 + archonNum),
                rc.readBroadcastFloat(ENEMY_TREE_1_Y_CHANNEL*3 + archonNum));
        if(rc.canSenseLocation(treeLoc1)){
            if(!rc.isLocationOccupiedByTree(treeLoc1)){
                rc.broadcast(TREE_TARGET_CHANNEL_1 * 3 + archonNum, -1);
            }
            else{
                if(rc.senseTreeAtLocation(treeLoc1).getID()!= rc.readBroadcast(TREE_TARGET_CHANNEL_1 * 3 + archonNum)){
                    rc.broadcast(TREE_TARGET_CHANNEL_1 * 3 + archonNum, rc.senseTreeAtLocation(treeLoc1).getID());
                }
            }
        }
        MapLocation treeLoc2 = new MapLocation(rc.readBroadcastFloat(ENEMY_TREE_2_X_CHANNEL*3 + archonNum),
                rc.readBroadcastFloat(ENEMY_TREE_2_Y_CHANNEL*3 + archonNum));
        if(rc.canSenseLocation(treeLoc2)){
            if(!rc.isLocationOccupiedByTree(treeLoc2)){
                rc.broadcast(TREE_TARGET_CHANNEL_2 * 3 + archonNum, -1);
            }
            else{
                if(rc.senseTreeAtLocation(treeLoc1).getID()!= rc.readBroadcast(TREE_TARGET_CHANNEL_2 * 3 + archonNum)){
                    rc.broadcast(TREE_TARGET_CHANNEL_2 * 3 + archonNum, rc.senseTreeAtLocation(treeLoc2).getID());
                }
            }
        }
    }
    
    /**
     * Adds enemy robot locations to the channel
     * @param archonNum
     * @return
     * @throws GameActionException
     */
    static boolean updateEnemyRobotLocs(int archonNum) throws GameActionException{
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1,rc.getTeam().opponent());
        if(nearbyRobots.length > 0){
            int robotChannel1 = rc.readBroadcast(RobotPlayer.ENEMY_ROBOT_CHANNEL_1*3 + archonNum);
            int robotChannel2 = rc.readBroadcast(RobotPlayer.ENEMY_ROBOT_CHANNEL_2*3 + archonNum);
            int robotId = nearbyRobots[0].getID();
            if( robotChannel1 != robotId && robotChannel2 != robotId){
                MapLocation robotLoc = nearbyRobots[0].getLocation();
                if(robotChannel2 == -1){
                    rc.broadcast(RobotPlayer.TREE_TARGET_CHANNEL_2*3 + archonNum, robotId);
                    rc.broadcastFloat(RobotPlayer.ENEMY_ROBOT_2_X_CHANNEL*3 + archonNum, robotLoc.x);
                    rc.broadcastFloat(RobotPlayer.ENEMY_ROBOT_2_Y_CHANNEL*3 + archonNum, robotLoc.y);
                }
                else{
                    rc.broadcast(RobotPlayer.TREE_TARGET_CHANNEL_1*3 + archonNum, robotId);
                    rc.broadcastFloat(RobotPlayer.ENEMY_ROBOT_1_X_CHANNEL*3 + archonNum, robotLoc.x);
                    rc.broadcastFloat(RobotPlayer.ENEMY_ROBOT_1_Y_CHANNEL*3 + archonNum, robotLoc.y);
                }
            }
        }
        return true;
    }
    
    /**
     * Updates the robot channels if the given locations are within sensing range.
     * @param archonNum
     * @throws GameActionException
     */
    static void clearEnemyLocs(int archonNum) throws GameActionException{
        MapLocation robotLoc1 = new MapLocation(rc.readBroadcastFloat(ENEMY_ROBOT_1_X_CHANNEL*3 + archonNum),
                rc.readBroadcastFloat(ENEMY_ROBOT_1_Y_CHANNEL*3 + archonNum));
        if(rc.canSenseLocation(robotLoc1)){
            if(!rc.isLocationOccupiedByRobot(robotLoc1)){
                rc.broadcast(ENEMY_ROBOT_CHANNEL_1 * 3 + archonNum, -1);
            }
            else{
                if(rc.senseRobotAtLocation(robotLoc1).getID()!= rc.readBroadcast(ENEMY_ROBOT_CHANNEL_1  * 3 + archonNum)){
                    rc.broadcast(ENEMY_ROBOT_CHANNEL_1 * 3 + archonNum, rc.senseRobotAtLocation(robotLoc1).getID());
                }
            }
        }
        MapLocation robotLoc2 = new MapLocation(rc.readBroadcastFloat(ENEMY_ROBOT_2_X_CHANNEL*3 + archonNum),
                rc.readBroadcastFloat(ENEMY_ROBOT_2_Y_CHANNEL*3 + archonNum));
        if(rc.canSenseLocation(robotLoc2)){
            if(!rc.isLocationOccupiedByRobot(robotLoc2)){
                rc.broadcast(ENEMY_ROBOT_CHANNEL_2 * 3 + archonNum, -1);
            }
            else{
                if(rc.senseRobotAtLocation(robotLoc1).getID()!= rc.readBroadcast(ENEMY_ROBOT_CHANNEL_2  * 3 + archonNum)){
                    rc.broadcast(ENEMY_ROBOT_CHANNEL_2 * 3 + archonNum, rc.senseRobotAtLocation(robotLoc2).getID());
                }
            }
        }
    }
    
    /**
     * Determines whether or not the robot is nearly dead based on a set threshold
     * @return true if it is lower than the threshold
     */
    static boolean isDying() {
        RobotType robotType = rc.getType();
        float robotHp = rc.getHealth();
        switch (robotType) {
        case GARDENER:
            return robotHp < DYING_GARDENER_HP_THRESHOLD; 
        case SOLDIER:
            return robotHp < DYING_SOLDIER_HP_THRESHOLD;
        case LUMBERJACK:
            return robotHp < DYING_LUMBERJACK_HP_THRESHOLD;
        case SCOUT:
            return robotHp < DYING_SCOUT_HP_THRESHOLD;
        case TANK:
            return robotHp < DYING_TANK_HP_THRESHOLD;
        default:
            return false;
        }
    }

    /**
     * Returns a random Direction
     * @return a random Direction
     */
    static Direction randomDirection() {
        return new Direction((float)Math.random() * 2 * (float)Math.PI);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
     *
     * @param dir The intended direction of movement
     * @param degreeOffset Spacing between checked directions (degrees)
     * @param checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {
        if(rc.hasMoved()){
            return false;
        }
        // First, try intended direction
        if (!rc.hasMoved() && rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }
        // Now try a bunch of similar angles
        int currentCheck = 1;
        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            if(!rc.hasMoved() && rc.canMove(dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset*currentCheck));
                return true;
            }
            // Try the offset on the right side
            if(!rc.hasMoved() && rc.canMove(dir.rotateRightDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateRightDegrees(degreeOffset*currentCheck));
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }
        // A move never happened, so return false.
        return false;
    }
    
    /**
     * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        return tryMove(dir,60,6);
    }
    
    /**
     * Attempts to move randomly in the general direction of dir at most degreeOffset away, trying to move
     * numChecks times
     * @param dir direction to attempt moving towards
     * @param degreeOffset 
     * @param numChecks
     * @return true if it successfully moves
     * @throws GameActionException 
     */
    static boolean tryMoveInGeneralDirection(Direction dir, float degreeOffset, int numChecks) throws GameActionException{
        if(rc.hasMoved()){
            return false;
        }
        int attempts = 0;
        while(attempts < numChecks){
            float multiplier = (float) (2*Math.random()) - 1;
            Direction randomDir = dir.rotateLeftDegrees(multiplier * degreeOffset);
            if(!rc.hasMoved() && rc.canMove(randomDir)){
                rc.move(randomDir);
                return true;
            }
            attempts +=1;
        }
        return false;
    }
    
    /**
     * Attempts to move randomly in the general direction of dir
     * @param dir
     * @return
     * @throws GameActionException
     */
    static boolean tryMoveInGeneralDirection(Direction dir) throws GameActionException{
        return tryMoveInGeneralDirection(dir, 110,11);
    }
    
    /**
     * Attempts to move towards a certain point while avoiding small obstacles at an angle of at most 60 degrees.
     * @param loc location it is trying to move towards
     * @return true if the robot has successfully moved towards the point
     * @throws GameActionException
     */
    static boolean moveTowards(MapLocation loc, RobotController rc) throws GameActionException{
        return tryMove(rc.getLocation().directionTo(loc), 45, 4);
    }
    
    /**
     * Attempts to move towards a certain direction while avoiding small obstacles at an angle of at most 60 degrees.
     * @param dir
     * @return true if the robot has successfully moved towards the point
     * @throws GameActionException
     */
    static boolean moveTowards(Direction dir, RobotController rc) throws GameActionException{
        return tryMove(dir, 45, 4);
    }
    
    // Can be improved to take into account bullet speed instead of just trajectory?
    /**
     * A slightly more complicated example function, this returns true if the given bullet is on a collision
     * course with the current robot. Doesn't take into account objects between the bullet and this robot.
     *
     * @param bullet The bullet in question
     * @return True if the line of the bullet's path intersects with this robot's current position.
     */
    static boolean willCollideWithMe(BulletInfo bullet) {
        MapLocation myLocation = rc.getLocation();

        // Get relevant bullet information
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(myLocation);
        float distToRobot = bulletLocation.distanceTo(myLocation);
        float theta = propagationDirection.radiansBetween(directionToRobot);

        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > Math.PI/2) {
            return false;
        }

        // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
        // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
        // This corresponds to the smallest radius circle centered at our location that would intersect with the
        // line that is the path of the bullet.
        float perpendicularDist = (float)Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

        return (perpendicularDist <= rc.getType().bodyRadius);
    }
    
    /**
     * Attempts to dodge incoming bullets that it is in the line of fire from
     * @throws GameActionException
     */
    static boolean dodge() throws GameActionException {
        if(rc.hasMoved()){
            return false;
        }
        BulletInfo[] bullets = rc.senseNearbyBullets();
        for (BulletInfo bi : bullets) {
            if (willCollideWithMe(bi)) {
                trySidestep(bi);
            }
        }
        return true;
    }
    
    /**
     * Attempts to dodge incoming bullets that it is in the line of fire from within a certain distance
     * @throws GameActionException
     */
    static boolean dodge(float dist) throws GameActionException {
        if(rc.hasMoved()){
            return false;
        }
        BulletInfo[] bullets = rc.senseNearbyBullets(dist);
        for (BulletInfo bi : bullets) {
            if (willCollideWithMe(bi)) {
                trySidestep(bi);
            }
        }
        return true;
        
    }
    
    /**
     * Attempts to dodge a bullet by moving perpendicularly away from it.
     * @param bullet
     * @return
     * @throws GameActionException
     */
    static boolean trySidestep(BulletInfo bullet) throws GameActionException{
        Direction towards = bullet.getDir();
        //MapLocation leftGoal = rc.getLocation().add(towards.rotateLeftDegrees(90), rc.getType().bodyRadius);
        //MapLocation rightGoal = rc.getLocation().add(towards.rotateRightDegrees(90), rc.getType().bodyRadius);
        return(tryMove(towards.rotateRightDegrees(90)) || tryMove(towards.rotateLeftDegrees(90)));
    }
    
    /**
     * Get the broadcasted enemy archon ids.
     * @param rc
     * @return array of enemy archon ids or -1 for each id not in broadcast
     * @throws GameActionException
     */
    static int[] getEnemyArchonIds() throws GameActionException {
        int[] ids = new int[MAX_ARCHONS];
        for (int i = 1; i<= MAX_ARCHONS; i++){
            ids[i-1] = rc.readBroadcast(ENEMY_ARCHON_ID_CHANNEL*3 + i);
        }
        return ids;
    }
    
    /**
     * Read broadcast channels to get the location of an enemy archon
     * @param rc
     * @return the location of an enemy archon or null if none have been sense
     * @throws GameActionException
     */
    static MapLocation enemyArchonLocation() throws GameActionException{
        int[] ids = getEnemyArchonIds();
        for (int i = 1; i<= MAX_ARCHONS; i++){
            int enemyArchonId = ids[i-1];
            if(enemyArchonId != -1){
                float archonX = rc.readBroadcastFloat(ENEMY_ARCHON_X_CHANNEL*3+i);
                float archonY = rc.readBroadcastFloat(ENEMY_ARCHON_Y_CHANNEL*3+i);
                return new MapLocation(archonX,archonY);
            }
        }
        return null;
    }
    
    /**
     * Reads a tree location from the channels or returns own location if it not on the channels
     * @param archonNum
     * @return
     * @throws GameActionException
     */
    static MapLocation readTreeLocation(int archonNum) throws GameActionException{
        if(rc.readBroadcast(TREE_TARGET_CHANNEL_1*3 + archonNum) != -1){
            return new MapLocation(rc.readBroadcastFloat(ENEMY_TREE_1_X_CHANNEL * 3 + archonNum), rc.readBroadcastFloat(ENEMY_TREE_1_Y_CHANNEL*3 + archonNum));
        }
        else if (rc.readBroadcast(TREE_TARGET_CHANNEL_2*3 + archonNum) != -1){
            return new MapLocation(rc.readBroadcastFloat(ENEMY_TREE_2_X_CHANNEL * 3 + archonNum), rc.readBroadcastFloat(ENEMY_TREE_2_Y_CHANNEL*3 + archonNum));
        }
        return rc.getLocation();
    }
    
    /**
     * Reads an enemy location from the channels or returns own location if it not on the channels
     * @param archonNum
     * @return
     * @throws GameActionException
     */
    static MapLocation readRobotLocation(int archonNum) throws GameActionException{
        if(rc.readBroadcast(RobotPlayer.ENEMY_ROBOT_CHANNEL_1*3 + archonNum) != -1){
            return new MapLocation(rc.readBroadcastFloat(ENEMY_ROBOT_1_X_CHANNEL * 3 + archonNum), rc.readBroadcastFloat(ENEMY_ROBOT_1_Y_CHANNEL*3 + archonNum));
        }
        else if (rc.readBroadcast(ENEMY_ROBOT_CHANNEL_2*3 + archonNum) != -1){
            return new MapLocation(rc.readBroadcastFloat(ENEMY_ROBOT_2_X_CHANNEL * 3 + archonNum), rc.readBroadcastFloat(ENEMY_ROBOT_2_Y_CHANNEL*3 + archonNum));
        }
        return rc.getLocation();
    }
    
    /**
     * Checks whether or not a team mate is within the shot line given the team mate, own location, and shooting
     * direction
     * @param nearbyTeamMate
     * @param ownLoc
     * @param shootingDir
     * @return
     */
    static boolean willHit(RobotInfo nearbyTeamMate, MapLocation ownLoc, Direction shootingDir){
        MapLocation teamMateLoc = nearbyTeamMate.getLocation();
        float theta = shootingDir.radiansBetween(ownLoc.directionTo(nearbyTeamMate.getLocation()));
        if(theta < Math.PI/2 && theta > -Math.PI/2){
            float hypotheneus = ownLoc.distanceTo(teamMateLoc);
            if(hypotheneus * Math.sin(theta) <= nearbyTeamMate.getRadius())
                return true;
        }
        return false;
    }
    
    /**
     * Checks whether or not a robot can shoot at a certain target robot at the center while avoiding friendly fire
     * Accounts for whether or not the robot is actually allowed to shoot
     * @param rc
     * @param enemyRobotId id of the center target
     * @param numShots number of shots to be fired (1, 3, or 5 only)
     * @return true if it is possible to shoot without hitting any robots closer than the target
     * @throws GameActionException
     */
    static boolean canShootRobot(RobotController rc, int enemyRobotId, int numShots) throws GameActionException{
        if(!(numShots==1 || numShots == 3 || numShots == 5))
            return false;
        if(rc.hasAttacked() || !rc.canSenseRobot(enemyRobotId))
            return false;
        if(!rc.canFireSingleShot())
            return false;
        RobotInfo enemyInfo = rc.senseRobot(enemyRobotId);
        MapLocation enemyLoc = enemyInfo.getLocation();
        MapLocation ownLoc = rc.getLocation();
        Direction enemyDir = ownLoc.directionTo(enemyLoc);
        RobotInfo[] nearbyTeamMates = rc.senseNearbyRobots(ownLoc.distanceTo(enemyLoc), rc.getTeam());
        
        for (RobotInfo nearbyTeamMate : nearbyTeamMates){
            if(willHit(nearbyTeamMate, ownLoc, enemyDir))
                return false;
        }
        
        if(numShots == 3 && rc.canFireTriadShot()){
            float spread = GameConstants.TRIAD_SPREAD_DEGREES;
            for(int i = 0; i<1; i++){
                Direction shootDirLeft = enemyDir.rotateLeftDegrees(spread);
                Direction shootDirRight = enemyDir.rotateRightDegrees(spread);
                for (RobotInfo nearbyTeamMate : nearbyTeamMates){
                    if(willHit(nearbyTeamMate, ownLoc, shootDirLeft))
                        return false;
                    if(willHit(nearbyTeamMate, ownLoc, shootDirRight))
                        return false;
                }
            }
        }
        else{
            return false;
        }
        
        if(numShots == 5 && rc.canFirePentadShot()){
            float spread = GameConstants.PENTAD_SPREAD_DEGREES;
            for(int i = 0; i<2; i++){
                Direction shootDirLeft = enemyDir.rotateLeftDegrees(spread);
                Direction shootDirRight = enemyDir.rotateRightDegrees(spread);
                for (RobotInfo nearbyTeamMate : nearbyTeamMates){
                    if(willHit(nearbyTeamMate, ownLoc, shootDirLeft))
                        return false;
                    if(willHit(nearbyTeamMate, ownLoc, shootDirRight))
                        return false;
                }
            }
        }
        else{
            return false;
        }
        return true;
    }
    
    /**
     * Tries to attack an enemy archon based on the enemy archon ids broadcasted
     * @return a boolean of whether an attack was performed
     * @throws GameActionException
     */
    static boolean tryAttackEnemyArchon(RobotController rc) throws GameActionException {
        if(rc.hasAttacked()){
            return false;
        }
        MapLocation ownLoc = rc.getLocation();
        for (int i=1; i<=3; i+=1) {
            int enemyArchonId = rc.readBroadcast(ENEMY_ARCHON_ID_CHANNEL*MAX_ARCHONS+i);
            if (enemyArchonId != -1 && rc.canSenseRobot(enemyArchonId)) {
                RobotInfo enemyArchon = rc.senseRobot(enemyArchonId);
                if(rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length > 6 && canShootRobot(rc, enemyArchonId, 5)){
                    rc.firePentadShot(ownLoc.directionTo(enemyArchon.getLocation()));
                    return true;
                }
                else if(rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length > 3 && canShootRobot(rc, enemyArchonId, 3)){
                    rc.fireTriadShot(ownLoc.directionTo(enemyArchon.getLocation()));
                    return true;
                }
                else if (canShootRobot(rc,enemyArchonId, 1)) {
                    rc.fireSingleShot(ownLoc.directionTo(enemyArchon.location));
                    return true;
                }
            }
        }
        return false;
    }
}
