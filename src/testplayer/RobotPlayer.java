package testplayer;
import battlecode.common.*;
import testplayer.Archon;

public strictfp class RobotPlayer {
    static RobotController rc;
    
    //Except for the channel that contains the round number
    static int ROUND_NUMBER_CHANNEL = 1000;
    
    //For channel numbers, get channel number, multiply by 3, then add archon number (from 1 to 3)
    static final int PHASE_NUMBER_CHANNEL = 0;
    static final int ARCHON_DIRECTION_RADIANS_CHANNEL = 1;
    static final int ARCHON_LOCATION_X_CHANNEL = 2;
    static final int ARCHON_LOCATION_Y_CHANNEL = 3;
    static final int LIVING_GARDENERS_CHANNEL = 4;
    static final int IMMEDIATE_TARGET_X_CHANNEL = 5;
    static final int IMMEDIATE_TARGET_Y_CHANNEL = 6;
    
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
    @SuppressWarnings("unused")
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
                break;
            case SCOUT:
                break;
        }
	}
    
    static int getNearestArchon() throws GameActionException {
        MapLocation loc = rc.getLocation();
        int archonNum = 1;
        float min_distance = Float.MAX_VALUE;
        for(int i = 1; i <= rc.getInitialArchonLocations(rc.getTeam()).length; i++){
            float archonX = ((float) rc.readBroadcast(ARCHON_LOCATION_X_CHANNEL*3+i)) / Archon.CONVERSION_OFFSET;
            float archonY = ((float) rc.readBroadcast(ARCHON_LOCATION_Y_CHANNEL*3+i)) / Archon.CONVERSION_OFFSET;
            MapLocation archonLoc = new MapLocation(archonX, archonY);
            float dist = loc.distanceTo(archonLoc);
            if(dist < min_distance){
                min_distance = dist;
                archonNum = i;
            }
        }
    	return archonNum;
    }
    
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

    
    static boolean isDying(RobotType robotType, float robotHp) {
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
}
