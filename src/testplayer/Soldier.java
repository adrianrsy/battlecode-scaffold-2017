package testplayer;
import battlecode.common.*;
import testplayer.RobotPlayer;

public strictfp class Soldier {
    static RobotController rc;
    
    //Active turn limit
    static int PHASE_1_ACTIVE_TURN_LIMIT = 60;
    
    //Turns it will move away until inactive
    static int MOVE_AWAY_TURNS = 5;
    
    /**
     * A soldier will identify which archon group it belongs to via checking the three sets of archon locations and 
     * identifying which it is closest to.<br>
     * Soldiers will always try to dodge nearby bullets, then move in the general opposite direction of their archon leader 
     * then try to shoot at the enemy archon if within sensing distance, or the closest enemy within sensing distance. 
     * If multiple enemies are within sensing distance, fire a triple shot (if >3), or a quintuple shot (if >6).<br>
     * If the enemy archon is detected in sensing distance, this will be broadcasted to a channel (to be read by shooting units
     * of all three archons, and they will try to sense it and shoot at it.)
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
    
    static void runSoldierPhase1() throws GameActionException {
        System.out.println("I'm a soldier!");
        Team ownTeam = rc.getTeam();
        Team enemy = ownTeam.opponent();
        int archonNum = RobotPlayer.getNearestArchon();
        Direction headedTo = RobotPlayer.getArchonDirection(archonNum).opposite();

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
            	// first try to dodge any bullets
            	RobotPlayer.dodge();
            	RobotPlayer.moveTowards(headedTo);
            	
            	RobotPlayer.tryAttackEnemyArchon();
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
                		// FIXME: how to broadcast about enemy archon?
                		rc.broadcast(RobotPlayer.ENEMY_ARCHON_CHANNEL*3+enemyArchons, robot.ID);
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
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }
        }
    }

}
