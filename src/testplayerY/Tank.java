package testplayerY;
import battlecode.common.*;
import testplayerY.RobotPlayer;

public strictfp class Tank {
    RobotController rc;

    public Tank(RobotController rc){
        this.rc = rc;
    }
    
    /*
     * TODO:
     * Soldier code, except more movement attempts against trees (how does body attack work?)
     */
    
    /**
     * TODO:
     * 
     * A tank will identify which archon group it belongs to via checking the three sets of archon locations and 
     * identifying which it is closest to.<br>
     * 
     * Basic idea is to attempt dodges/ move towards enemy archon if it has been scouted. Continually fire
     * maximum number of shots at closest detectable enemy/ archon if it is detectable unless friendly fire occurs (then reduce
     * shot size)<br>
     * 
     * (Tentatively using same code as soldier)<br>
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
    
    void runTank() throws GameActionException {
        System.out.println("I'm a tank!");
        Team ownTeam = rc.getTeam();
        Team enemy = ownTeam.opponent();
        int archonNum = RobotPlayer.getNearestArchon();
        Direction headedTo = RobotPlayer.getArchonDirection(archonNum).opposite();
        int currentTargetId = -1;

        while (true) {
            try {
                RobotPlayer.dodge();
                RobotPlayer.updateEnemyRobotLocs(archonNum);
                RobotPlayer.updateTreeLocs(archonNum);
                MapLocation enemyArchonLocation = RobotPlayer.enemyArchonLocation();
                if (enemyArchonLocation != null) {
                    RobotPlayer.moveTowards(enemyArchonLocation, rc);
                    if (!rc.hasMoved()) {
                        if (rc.canFireSingleShot()) {
                            rc.fireSingleShot(rc.getLocation().directionTo(enemyArchonLocation));
                        }
                    }
                }
                MapLocation enemyLoc = RobotPlayer.readRobotLocation(archonNum);
                if(!enemyLoc.equals(rc.getLocation()))
                    headedTo = rc.getLocation().directionTo(enemyLoc);
                headedTo = tryMoveInGeneralDirection(headedTo,110,11);
                RobotPlayer.tryAttackEnemyArchon(rc);
                MapLocation ownLocation = rc.getLocation();
                // See if there are any nearby enemy robots
                RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(-1, enemy);
                
                if(currentTargetId != -1 && rc.canSenseRobot(currentTargetId)){
                    Direction attackDirection = ownLocation.directionTo(rc.senseRobot(currentTargetId).getLocation());
                    if(RobotPlayer.canShootRobot(rc, currentTargetId, 5))
                        rc.firePentadShot(attackDirection);
                    else if(RobotPlayer.canShootRobot(rc, currentTargetId, 3))
                        rc.fireTriadShot(attackDirection);
                    else if(RobotPlayer.canShootRobot(rc, currentTargetId, 1))
                        rc.fireSingleShot(attackDirection);
                }
                else{
                    currentTargetId = -1;
                    int enemyArchons = 0;
                    MapLocation nearestArchonLoc = null; // sorry 005
                    for (RobotInfo robot : nearbyEnemies) {
                        if (robot.type == RobotType.GARDENER) {
                            enemyArchons++;
                            if (enemyArchons == 1) {
                                currentTargetId = robot.getID();
                                nearestArchonLoc = robot.getLocation();
                            }
                            for(int i =1; i<=3; i++){
                                int possibleArchonId = rc.readBroadcast(RobotPlayer.ENEMY_ARCHON_ID_CHANNEL*3 +i);
                                if(possibleArchonId == -1){
                                    rc.broadcast(RobotPlayer.ENEMY_ARCHON_ID_CHANNEL*3 + i, robot.getID());
                                    rc.broadcastFloat(RobotPlayer.ENEMY_ARCHON_X_CHANNEL*3 + i, robot.getLocation().x);
                                    rc.broadcastFloat(RobotPlayer.ENEMY_ARCHON_Y_CHANNEL*3 + i, robot.getLocation().y);
                                    break;
                                }
                                if(possibleArchonId == robot.getID()){
                                    rc.broadcastFloat(RobotPlayer.ENEMY_ARCHON_X_CHANNEL*3 + i, robot.getLocation().x);
                                    rc.broadcastFloat(RobotPlayer.ENEMY_ARCHON_Y_CHANNEL*3 + i, robot.getLocation().y);
                                    break;
                                }
                            }
                        }
                    }
                    if (nearbyEnemies.length > 0){
                        currentTargetId = nearbyEnemies[0].getID();
                        headedTo = ownLocation.directionTo(nearbyEnemies[0].getLocation());
                    }
                    
                    if(rc.canSenseRobot(currentTargetId)){
                        Direction attackDirection = ownLocation.directionTo(rc.senseRobot(currentTargetId).getLocation());
                        if(RobotPlayer.canShootRobot(rc, currentTargetId, 5))
                            rc.firePentadShot(attackDirection);
                        else if(RobotPlayer.canShootRobot(rc, currentTargetId, 3))
                            rc.fireTriadShot(attackDirection);
                        else if(RobotPlayer.canShootRobot(rc, currentTargetId, 1))
                            rc.fireSingleShot(attackDirection);
                    }
                    else if (nearbyEnemies.length > 0){
                        currentTargetId = nearbyEnemies[0].getID();
                        Direction attackDirection = ownLocation.directionTo(rc.senseRobot(currentTargetId).getLocation());
                        if(RobotPlayer.canShootRobot(rc, currentTargetId, 5))
                            rc.firePentadShot(attackDirection);
                        else if(RobotPlayer.canShootRobot(rc, currentTargetId, 3))
                            rc.fireTriadShot(attackDirection);
                        else if(RobotPlayer.canShootRobot(rc, currentTargetId, 1))
                            rc.fireSingleShot(attackDirection);
                    }
                    // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                    Clock.yield();
                }  
            } catch (Exception e) {
                System.out.println("Tank Exception");
                e.printStackTrace();
            }
        }
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
