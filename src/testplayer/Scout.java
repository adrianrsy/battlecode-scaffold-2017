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
            try {
            	// first try to dodge any bullets
            	RobotPlayer.dodge();
            	RobotPlayer.moveTowards(headedTo);
                RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(-1, enemy);
                for (RobotInfo robot : nearbyEnemies) {
                	if (robot.type == RobotType.ARCHON) {
                		for(int i =0; i<3; i++){
                		    int possibleArchonId = rc.readBroadcast(RobotPlayer.ENEMY_ARCHON_ID_CHANNEL*3+i);
                		    if(possibleArchonId == -1){
                		        rc.broadcast(RobotPlayer.ENEMY_ARCHON_ID_CHANNEL*3+i, robot.getID());
                		        rc.broadcast(RobotPlayer.ENEMY_ARCHON_X_CHANNEL*3 + i, (int) (robot.getLocation().x * RobotPlayer.CONVERSION_OFFSET));
                                rc.broadcast(RobotPlayer.ENEMY_ARCHON_Y_CHANNEL*3 + i, (int) (robot.getLocation().y * RobotPlayer.CONVERSION_OFFSET));
                                break;
                		    }
                		    if(possibleArchonId == robot.getID()){
                		        rc.broadcast(RobotPlayer.ENEMY_ARCHON_X_CHANNEL*3 + i, (int) (robot.getLocation().x * RobotPlayer.CONVERSION_OFFSET));
                                rc.broadcast(RobotPlayer.ENEMY_ARCHON_Y_CHANNEL*3 + i, (int) (robot.getLocation().y * RobotPlayer.CONVERSION_OFFSET));
                                break;
                		    }
                		}
                	}
                }
                if(RobotPlayer.isDying()){
                    hidingMode = true;
                }
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Scout Exception");
                e.printStackTrace();
            }
        }
        
        while (hidingMode) {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                RobotPlayer.dodge(4);
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
                System.out.println("Scout Exception");
                e.printStackTrace();
            }
        }
    }

}
