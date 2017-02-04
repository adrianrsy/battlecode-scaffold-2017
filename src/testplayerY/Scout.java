package testplayerY;
import battlecode.common.*;
import testplayerY.RobotPlayer;

public strictfp class Scout {
    RobotController rc;
    
    public Scout(RobotController rc){
        this.rc = rc;
    }
    
    /*
     * TODO:
     * Fix motion, move away from archon until unable to move then switch and move the opposite direction
     */
    
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
    
    void runScout() throws GameActionException {
        System.out.println("I'm a scout!");
        Team ownTeam = rc.getTeam();
        Team enemy = ownTeam.opponent();
        int archonNum = RobotPlayer.getNearestArchon();
        Direction headedTo = RobotPlayer.getArchonDirection(archonNum).opposite();
        boolean hidingMode = false;
        // this is local to each scout, so if the scout dies before being able to replace the archon,
        // it will never get replaced which should be fine for now
        int enemyArchonsToReplace = 0;

        while (!hidingMode) {
            try {
            	// first try to dodge any bullets
            	RobotPlayer.dodge();
            	
            	RobotPlayer.updateEnemyRobotLocs(archonNum);
            	RobotPlayer.updateTreeLocs(archonNum);
            	RobotPlayer.clearEnemyLocs(archonNum);
            	RobotPlayer.clearTreeLocs(archonNum);
            	MapLocation enemyArchonLocation = RobotPlayer.enemyArchonLocation();
                if (enemyArchonLocation != null) {
                    //RobotPlayer.moveTowards(enemyArchonLocation, rc);
                }
                headedTo = tryMoveTowardsOpen(headedTo, RobotPlayer.getArchonLoc(archonNum).directionTo(rc.getLocation()), 6);
                TreeInfo[] treesNearby = rc.senseNearbyTrees();
                for (TreeInfo tree : treesNearby) {
                    if (tree.team != ownTeam && tree.team != enemy && rc.canShake(tree.ID)) {
                        rc.shake(tree.ID);
                    }
                }
                int[] enemyArchonIds = RobotPlayer.getEnemyArchonIds();
                // check which broadcasted enemy archon ids belong to dead archons
                for(int i=0; i<enemyArchonIds.length; i++){
                    if (enemyArchonIds[i] != -1) {
                        float archonX = rc.readBroadcastFloat(RobotPlayer.ENEMY_ARCHON_X_CHANNEL*3+i+1);
                        float archonY = rc.readBroadcastFloat(RobotPlayer.ENEMY_ARCHON_Y_CHANNEL*3+i+1);
                        // if the location can be sensed but the robot can't be, then the robot is likely dead
                        if (rc.canSenseLocation(new MapLocation(archonX,archonY)) && !rc.canSenseRobot(enemyArchonIds[i])) {
                            rc.broadcast(RobotPlayer.ENEMY_ARCHON_ID_CHANNEL*3 + i+1, -1);
                            enemyArchonIds[i] = -1;
                            //enemyArchonsToReplace++;
                        }
                    }
                }
                
                RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(-1, enemy);
                for (RobotInfo robot : nearbyEnemies) {
                    if (rc.canFireSingleShot()) {
//                        rc.fireSingleShot(rc.getLocation().directionTo(robot.location));
                    }
                	if (robot.type == RobotType.GARDENER || enemyArchonsToReplace > 0) {
                	    for (int i=0; i<enemyArchonIds.length; i++) {
                	        int possibleArchonId = enemyArchonIds[i];
                		    if(possibleArchonId == -1){
                		        int newId = robot.getID();
                		        rc.broadcast(RobotPlayer.ENEMY_ARCHON_ID_CHANNEL*3+i+1, newId);
                		        rc.broadcastFloat(RobotPlayer.ENEMY_ARCHON_X_CHANNEL*3 + i+1, robot.getLocation().x);
                                rc.broadcastFloat(RobotPlayer.ENEMY_ARCHON_Y_CHANNEL*3 + i+1, robot.getLocation().y);
                                if (robot.type != RobotType.ARCHON) {
                                    enemyArchonsToReplace--;
                                }
                                enemyArchonIds[i] = newId;
                                break;
                		    } else if(possibleArchonId == robot.getID()){
                		        rc.broadcastFloat(RobotPlayer.ENEMY_ARCHON_X_CHANNEL*3 + i+1, robot.getLocation().x);
                                rc.broadcastFloat(RobotPlayer.ENEMY_ARCHON_Y_CHANNEL*3 + i+1, robot.getLocation().y);
                                break;
                		    }
                		}
                	}
                }
                if(RobotPlayer.isDying()){
                    hidingMode = true;
                    int numScouts = rc.readBroadcast(RobotPlayer.LIVING_SCOUT_CHANNEL * 3 + archonNum);
                    rc.broadcast(RobotPlayer.LIVING_SCOUT_CHANNEL*3 + archonNum, numScouts - 1);

                }
                if(!rc.hasMoved()){
                    headedTo = RobotPlayer.randomDirection();
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
    
    /**
     * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
     *
     * @param dir The intended direction of movement
     * @param degreeOffset Spacing between checked directions (degrees)
     * @param checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
     * @return true if a move was performed
     * @throws GameActionException
     */
    Direction tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {
        if(rc.hasMoved()){
            return dir;
        }
        // First, try intended direction
        if (!rc.hasMoved() && rc.canMove(dir)) {
            rc.move(dir);
            return dir;
        }
        // Now try a bunch of similar angles
        int currentCheck = 1;
        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            if(!rc.hasMoved() && rc.canMove(dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset*currentCheck));
                return dir.rotateLeftDegrees(degreeOffset*currentCheck);
            }
            // Try the offset on the right side
            if(!rc.hasMoved() && rc.canMove(dir.rotateRightDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateRightDegrees(degreeOffset*currentCheck));
                return dir.rotateRightDegrees(degreeOffset*currentCheck);
            }
            // No move performed, try slightly further
            currentCheck++;
        }
        // A move never happened, so return false.
        return dir;
    }
    
   /**
    * Tries to move towards a final goal, but takes into account the direction it is currently heading
    * (To avoid obstacles without turning back)
    * @param dir The direction it is currently headed towards
    * @param secondaryDir The final goal direction
    * @param checks number of 
    * @return
    * @throws GameActionException
    */
    Direction tryMoveTowardsOpen(Direction dir, Direction secondaryDir, int checks) throws GameActionException {
        if(rc.hasMoved()){
            return dir;
        }
        float difRadiansPerCheck = (secondaryDir.radians - dir.radians)/ checks;
        // First, try intended direction
        if (!rc.hasMoved() && rc.canMove(dir)) {
            rc.move(dir);
            return dir;
        }
        int currentCheck = 1;
        Direction targetDir = new Direction(dir.radians);
        while(currentCheck <= checks){
            if (difRadiansPerCheck > 0) {
                targetDir = targetDir.rotateRightRads(difRadiansPerCheck);
            } else {
                targetDir = targetDir.rotateLeftRads(-difRadiansPerCheck);
            }
            //Direction targetDir = new Direction(dir.radians + difRadiansPerCheck*currentCheck);
            if(!rc.hasMoved() && rc.canMove(targetDir)) {
                rc.move(targetDir);
                return targetDir;
            }
            currentCheck++;
        }
        if(!rc.hasMoved()){
            System.out.println("Cannot move thus trying a somewhat random move");
            return tryMove(dir,150,15);
        }
        
        return dir;
    }

}
