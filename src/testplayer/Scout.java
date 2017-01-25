package testplayer;
import battlecode.common.*;
import testplayer.RobotPlayer;

public strictfp class Scout {
    static RobotController rc;
    
    //Active turn limit
    static int PHASE_1_ACTIVE_TURN_LIMIT = 60;
    
    //Turns it will move away until inactive
    static int MOVE_AWAY_TURNS = 5;
    
    /**
     * A scout will identify which archon group it belongs to via checking the three sets of archon locations and 
     * identifying which it is closest to.<br>
     * Scouts will at first always try to dodge nearby bullets, then move in the general opposite direction of their archon leader
     * until an enemy archon is detected.
     * Once detected, broadcast enemy archon location to channel, then go to hide mode.
     * In hide mode, a scout will look for an enemy tree, stay and hide there, then shoot at sensed enemies.
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
    
    static void runScout() throws GameActionException {
        System.out.println("I'm a scout!");
        Team ownTeam = rc.getTeam();
        Team enemy = ownTeam.opponent();
        int archonNum = RobotPlayer.getNearestArchon();
        Direction headedTo = RobotPlayer.getArchonDirection(archonNum).opposite();
        boolean hidingMode = false;

        while (!hidingMode) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
            	// first try to dodge any bullets
            	RobotPlayer.dodge();
            	RobotPlayer.moveTowards(headedTo);

                MapLocation ownLocation = rc.getLocation();

                // See if there are any nearby enemy robots
                RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(-1, enemy);
                int enemyArchons = 0;
                for (RobotInfo robot : nearbyEnemies) {
                	if (robot.type == RobotType.ARCHON) {
                		enemyArchons++;
                		// FIXME
                		rc.broadcast(RobotPlayer.ENEMY_ARCHON_CHANNEL*3+enemyArchons, robot.ID);
                	}
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }
        }
        
        while (hidingMode) {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
            	MapLocation ownLocation = rc.getLocation();
            	TreeInfo treeAtLocation = rc.senseTreeAtLocation(ownLocation);
            	if (treeAtLocation != null && treeAtLocation.team == enemy) {
            		// try to move to the center of the tree
            		RobotPlayer.tryMove(ownLocation.directionTo(treeAtLocation.location));
            		// See if there are any nearby enemy robots and shoot at the nearest one
                    RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(-1, enemy);
                    if (nearbyEnemies.length > 0 && rc.canFireSingleShot()) {
                    	rc.fireSingleShot(ownLocation.directionTo(nearbyEnemies[0].location));
                    }
            	} else {
                    TreeInfo[] nearbyEnemyTrees = rc.senseNearbyTrees(-1, enemy);
                    if (nearbyEnemyTrees.length > 0) {
                    	// try to move to the nearest enemy tree
                    	RobotPlayer.tryMove(ownLocation.directionTo(nearbyEnemyTrees[0].location));
                    } else {
                    	// if can't find enemy trees, try to move back from where it came from to look
                    	RobotPlayer.tryMove(headedTo.opposite());
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
