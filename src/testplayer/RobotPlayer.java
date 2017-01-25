package testplayer;
import battlecode.common.*;

/*
 * TODO:
 * 
 * Archon:
 * done
 * 
 * Gardener:
 * done
 * Add purchasing of victory points for pasive gardeners
 * 
 * Lumberjack:
 * done
 * 
 * Soldier:
 * done
 * Maybe try avoiding friendly fire?
 * 
 * Scout:
 * Add better condition for switching to hiding mode
 * 
 * Tank:
 * Change from soldier code, check how body attack works
 * 
 */

public strictfp class RobotPlayer {
    static RobotController rc;
    
    //Offset when converting floats to integers and vice versa
    static int CONVERSION_OFFSET = 100000;
    
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
    
    static final int TARGET_IS_TREE = 1;
    static final int TARGET_IS_ROBOT = 2;
    
    static final double DYING_GARDENER_HP_THRESHOLD = 0.19 * RobotType.GARDENER.maxHealth;
    static final double DYING_SOLDIER_HP_THRESHOLD = 0.19 * RobotType.SOLDIER.maxHealth;
    static final double DYING_LUMBERJACK_HP_THRESHOLD = 0.19 * RobotType.LUMBERJACK.maxHealth;
    static final double DYING_TANK_HP_THRESHOLD = 0.19 * RobotType.TANK.maxHealth;
    static final double DYING_SCOUT_HP_THRESHOLD = 0.19 * RobotType.SCOUT.maxHealth;
    static final int MAX_ARCHONS = 3;

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
                Archon.runArchonPhase1();
                break;
            case GARDENER:
                Gardener.runGardenerPhase1();
                break;
            case SOLDIER:
                Soldier.runSoldierPhase1();
                break;
            case LUMBERJACK:
                Lumberjack.runLumberjack();
                break;
            case TANK:
                Tank.runTank();
            case SCOUT:
                Scout.runScout();
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
            float archonX = ((float) rc.readBroadcast(ARCHON_LOCATION_X_CHANNEL*3+i)) / CONVERSION_OFFSET;
            float archonY = ((float) rc.readBroadcast(ARCHON_LOCATION_Y_CHANNEL*3+i)) / CONVERSION_OFFSET;
            MapLocation archonLoc = new MapLocation(archonX, archonY);
            float dist = loc.distanceTo(archonLoc);
            if(dist < min_distance){
                min_distance = dist;
                archonNum = i;
            }
        }
    	return archonNum;
    }
    
    static MapLocation getArchonLoc(int archonNum) throws GameActionException{
        float archonX = ((float) rc.readBroadcast(ARCHON_LOCATION_X_CHANNEL*3+archonNum)) / CONVERSION_OFFSET;
        float archonY = ((float) rc.readBroadcast(ARCHON_LOCATION_Y_CHANNEL*3+archonNum)) / CONVERSION_OFFSET;
        return new MapLocation(archonX, archonY);
    }
    
    static Direction getArchonDirection(int archonNum) throws GameActionException{
        float headedToRadians = ((float) rc.readBroadcast(ARCHON_DIRECTION_RADIANS_CHANNEL*3 + archonNum))/CONVERSION_OFFSET;
        return new Direction(headedToRadians);
    }
    
    /**
     * Find the nearest robot from a list of robots to a certain location
     * @param robotList list of robot's info
     * @param location 
     * @return information about the nearest robot or null if list is empty
     */
    static RobotInfo findNearestRobot(RobotInfo[] robotList, MapLocation location) {
    	float smallestDistance = Float.MAX_VALUE;
    	RobotInfo nearestRobot = null;
    	for (RobotInfo robotInfo : robotList) {
    		float distance = robotInfo.location.distanceTo(location);
			if (distance < smallestDistance) {
				smallestDistance = distance;
				nearestRobot = robotInfo;
			}
    	}
    	return nearestRobot;
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
     * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        return tryMove(dir,20,3);
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

        // First, try intended direction
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }
        // Now try a bunch of similar angles
        int currentCheck = 1;
        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            if(rc.canMove(dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset*currentCheck));
                return true;
            }
            // Try the offset on the right side
            if(rc.canMove(dir.rotateRightDegrees(degreeOffset*currentCheck))) {
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
     * Attempts to move randomly in the general direction of dir at most degreeOffset away, trying to move
     * numChecks times
     * @param dir direction to attempt moving towards
     * @param degreeOffset 
     * @param numChecks
     * @return true if it successfully moves
     * @throws GameActionException 
     */
    static boolean tryMoveInGeneralDirection(Direction dir, float degreeOffset, int numChecks) throws GameActionException{
        int attempts = 0;
        while(attempts < numChecks){
            float multiplier = (float) (2*Math.random()) - 1;
            Direction randomDir = dir.rotateLeftDegrees(multiplier * degreeOffset);
            if(rc.canMove(randomDir)){
                rc.move(randomDir);
                return true;
            }
            attempts +=1;
        }
        return false;
    }
    
    /**
     * Attempts to move towards a certain point while avoiding small obstacles at an angle of at most 60 degrees.
     * @param loc location it is trying to move towards
     * @return true if the robot has successfully moved towards the point
     * @throws GameActionException
     */
    static boolean moveTowards(MapLocation loc) throws GameActionException{
        return tryMove(rc.getLocation().directionTo(loc), 45, 4);
    }
    
    /**
     * Attempts to move towards a certain direction while avoiding small obstacles at an angle of at most 60 degrees.
     * @param dir
     * @return true if the robot has successfully moved towards the point
     * @throws GameActionException
     */
    static boolean moveTowards(Direction dir) throws GameActionException{
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
    static void dodge() throws GameActionException {
        BulletInfo[] bullets = rc.senseNearbyBullets();
        for (BulletInfo bi : bullets) {
            if (willCollideWithMe(bi)) {
                trySidestep(bi);
            }
        }
    }
    
    /**
     * Attempts to dodge incoming bullets that it is in the line of fire from within a certain distance
     * @throws GameActionException
     */
    static void dodge(float dist) throws GameActionException {
        BulletInfo[] bullets = rc.senseNearbyBullets(dist);
        for (BulletInfo bi : bullets) {
            if (willCollideWithMe(bi)) {
                trySidestep(bi);
            }
        }
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
     * Tries to attack an enemy archon based on the enemy archon ids broadcasted
     * @return a boolean of whether an attack was tried
     * @throws GameActionException
     */
    static boolean tryAttackEnemyArchon() throws GameActionException {
    	for (int i=1; i<=MAX_ARCHONS; i++) {
    		int enemyArchonId = rc.readBroadcast(ENEMY_ARCHON_ID_CHANNEL*MAX_ARCHONS+i);
    		if (enemyArchonId != 0 && rc.canSenseRobot(enemyArchonId)) {
        		try {
        			RobotInfo enemyArchon = rc.senseRobot(enemyArchonId);
        			if(rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length > 6){
        			    if(rc.canFirePentadShot()){
        			        rc.firePentadShot(rc.getLocation().directionTo(enemyArchon.location));
        			    }
        			}
        			else if(rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length > 3){
                        if(rc.canFireTriadShot()){
                            rc.fireTriadShot(rc.getLocation().directionTo(enemyArchon.location));
                        }
                    }
        			else if (rc.canFireSingleShot()) {
        				rc.fireSingleShot(rc.getLocation().directionTo(enemyArchon.location));
        			}
        			return true;
        		} catch (GameActionException e) {
        			// do nothing
        		}
    		} else {
    			// no broadcasted locations for >=i yet; don't do anything
    			break;
    		}
    	}
    	return false;
    }
}
