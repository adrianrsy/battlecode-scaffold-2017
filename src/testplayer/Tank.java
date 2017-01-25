package testplayer;
import battlecode.common.*;
import testplayer.RobotPlayer;

public strictfp class Tank {
    static RobotController rc;
    
    //Active turn limit
    static int PHASE_1_ACTIVE_TURN_LIMIT = 60;
    
    //Turns it will move away until inactive
    static int MOVE_AWAY_TURNS = 5;
    
    /**
     * TODO:
     * 
     * A tank will identify which archon group it belongs to via checking the three sets of archon locations and 
     * identifying which it is closest to.<br>
     * 
     * Basic idea is to attempt dodges/ move towards enemy archon if it has been scouted. Continually fire
     * maximum number of shots at closest detectable enemy/ archon if it is detectable unless friendly fire occurs (then reduce
     * shot size)
     *
     * Channels:<br>
     * <ul>
     * <li>4 - 6 -> Archon movement direction
     * </ul>
     *<br>
     *
     * Movement:<br>
     * Moves every turn.
     *<br>
     */
    
    static void runTank() throws GameActionException {
        System.out.println("I'm a tank!");
        Team ownTeam = rc.getTeam();
        Team enemy = ownTeam.opponent();
        int archonNum = RobotPlayer.getNearestArchon();
        int archonTargetId = 0;
        
        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
            	// first try to dodge any bullets
            	RobotPlayer.dodge();
            	if (archonTargetId == 0) {
            		int enemyArchonId = rc.readBroadcast(RobotPlayer.ENEMY_ARCHON_CHANNEL*RobotPlayer.MAX_ARCHONS+archonNum);
            		if (enemyArchonId != 0) {
            			// if it is not 0 (the default value) anymore, then enemy archon has been found
            			archonTargetId = enemyArchonId;
            			// TODO: get location of archon target
            		}
            	}
            	
            	// TODO: headedTo should be the location of archonTargetId
            	// RobotPlayer.moveTowards(headedTo);
            	
            	// RobotPlayer.tryAttackEnemyArchon();
                MapLocation ownLocation = rc.getLocation();

                // See if there are any nearby enemy robots
                RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(-1, enemy);
                int enemyArchons = 0;
                MapLocation nearestArchonLoc = null; // sorry 005
                for (RobotInfo robot : nearbyEnemies) {
                	if (robot.type == RobotType.ARCHON) {
                		enemyArchons++;
                		if (enemyArchons == 1) {
                			nearestArchonLoc = robot.location;
                		}
                	}
                }
                
                if (nearbyEnemies.length >= 6 && rc.canFirePentadShot()) {
                	// shoot at the nearest enemy archon if it is within sensing radius
                	// if not, shoot at the nearest enemy
                	if (enemyArchons > 0) {
                		rc.firePentadShot(ownLocation.directionTo(nearestArchonLoc));
                	} else {                		
                		rc.firePentadShot(ownLocation.directionTo(nearbyEnemies[0].location));
                	}
                } else if (nearbyEnemies.length >= 3 && rc.canFireTriadShot()) {
                	if (enemyArchons > 0) {
                		rc.fireTriadShot(ownLocation.directionTo(nearestArchonLoc));
                	} else {
                		rc.fireTriadShot(ownLocation.directionTo(nearbyEnemies[0].location));
                	}
                } else if (nearbyEnemies.length >= 1 && rc.canFireSingleShot()) {
                	if (enemyArchons > 0) {
                		rc.fireSingleShot(ownLocation.directionTo(nearestArchonLoc));
                	} else {
                		rc.fireSingleShot(ownLocation.directionTo(nearbyEnemies[0].location));
                	}
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Tank Exception");
                e.printStackTrace();
            }
        }
    }

}
