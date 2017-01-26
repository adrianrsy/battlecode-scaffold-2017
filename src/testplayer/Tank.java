package testplayer;
import battlecode.common.*;
import testplayer.RobotPlayer;

public strictfp class Tank {
    RobotController rc;

    public Tank(RobotController rc){
        this.rc = rc;
    }
    
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
        Team ownTeam = rc.getTeam();
        Team enemy = ownTeam.opponent();
        int archonNum = RobotPlayer.getNearestArchon();
        Direction headedTo = RobotPlayer.getArchonDirection(archonNum).opposite();
        int currentTargetId = -1;

        while (true) {
            try {
                RobotPlayer.dodge(rc);
                RobotPlayer.moveTowards(RobotPlayer.enemyArchonLocation(rc), rc);
                RobotPlayer.moveTowards(headedTo, rc);
                
                RobotPlayer.tryAttackEnemyArchon(rc);
                MapLocation ownLocation = rc.getLocation();
                
                // See if there are any nearby enemy robots
                RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(-1, enemy);
                
                if(currentTargetId > 0 && rc.canSenseRobot(currentTargetId)){
                    Direction attackDirection = ownLocation.directionTo(rc.senseRobot(currentTargetId).getLocation());
                    if (nearbyEnemies.length >= 6 && rc.canFirePentadShot()) {                     
                        rc.firePentadShot(attackDirection);
                    } else if (nearbyEnemies.length >= 3 && rc.canFireTriadShot()) {
                        rc.fireTriadShot(attackDirection);
                    } else if (nearbyEnemies.length >= 1 && rc.canFireSingleShot()) {
                        rc.fireSingleShot(attackDirection);
                    }
                }
                else{
                    currentTargetId = -1;
                    int enemyArchons = 0;
                    MapLocation nearestArchonLoc = null; // sorry 005
                    for (RobotInfo robot : nearbyEnemies) {
                        if (robot.type == RobotType.ARCHON) {
                            enemyArchons++;
                            if (enemyArchons == 1) {
                                nearestArchonLoc = robot.getLocation();
                            }
                            for(int i =1; i<=3; i++){
                                int possibleArchonId = rc.readBroadcast(RobotPlayer.ENEMY_ARCHON_ID_CHANNEL*3 +i);
                                if(possibleArchonId == -1){
                                    rc.broadcast(RobotPlayer.ENEMY_ARCHON_ID_CHANNEL*3 + i, robot.getID());
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
                    if (nearbyEnemies.length > 0){
                        currentTargetId = nearbyEnemies[0].getID();
                        headedTo = ownLocation.directionTo(nearbyEnemies[0].getLocation());
                    }
                    
                    if (nearbyEnemies.length >= 6 && rc.canFirePentadShot()) {
                        // shoot at the nearest enemy archon if it is within sensing radius
                        // if not, shoot at the nearest enemy
                        if (enemyArchons > 0) {
                            rc.firePentadShot(ownLocation.directionTo(nearestArchonLoc));
                        } else {                        
                            rc.firePentadShot(ownLocation.directionTo(nearbyEnemies[0].getLocation()));
                        }
                    } else if (nearbyEnemies.length >= 3 && rc.canFireTriadShot()) {
                        if (enemyArchons > 0) {
                            rc.fireTriadShot(ownLocation.directionTo(nearestArchonLoc));
                        } else {
                            rc.fireTriadShot(ownLocation.directionTo(nearbyEnemies[0].getLocation()));
                        }
                    } else if (nearbyEnemies.length >= 1 && rc.canFireSingleShot()) {
                        if (enemyArchons > 0) {
                            rc.fireSingleShot(ownLocation.directionTo(nearestArchonLoc));
                        } else {
                            rc.fireSingleShot(ownLocation.directionTo(nearbyEnemies[0].getLocation()));
                        }
                    }

                    // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                    Clock.yield();
                }  
            } catch (Exception e) {
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }
        }
    }

}
