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
     * If the archon is detected in sensing distance, this will be broadcasted to a channel (to be read by shooting units
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
        int currentTargetId = 0;
        int archonController = RobotPlayer.getNearestArchon();
        int currentPhase;
        boolean hasSentDyingBroadcast = false;

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                currentPhase = rc.readBroadcast(archonController);
                switch (currentPhase){
                case 1:
                    break;
                case 2:
                    break;
                case 3:
                    break;
                case 4:
                    break;
                case 5:
                    break;
                default:
                    break;
                }
                MapLocation ownLocation = rc.getLocation();

                // See if there are any nearby enemy robots
                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

                // If there are some...
                if (robots.length > 0) {
                    // And we have enough bullets, and haven't attacked yet this turn...
                    if (rc.canFireSingleShot()) {
                        boolean hasShot = false;
//                      find current target in nearby robots
                        for (RobotInfo robot : robots) {
                            if (currentTargetId == robot.ID) {
                                // ...Then fire a bullet in the direction of the target
                                rc.fireSingleShot(rc.getLocation().directionTo(robot.location));
                                hasShot = true;
                                break;
                            }
                        }
                        if (!hasShot) {
                            RobotInfo nearestRobot = RobotPlayer.findNearestRobot(robots, ownLocation);
                            currentTargetId = nearestRobot.ID;
                            rc.fireSingleShot(ownLocation.directionTo(nearestRobot.location));
                        }
                    }
                }
                
//              send a broadcast to the archon controlling it if it's dying so that it gets replaced
                if (!hasSentDyingBroadcast && RobotPlayer.isDying()) {
                    int channel = archonController + RobotPlayer.MAX_ARCHONS;
                    int previousCount = rc.readBroadcast(channel);
                    rc.broadcast(channel, previousCount+1);
                    hasSentDyingBroadcast = true;
                }

                // Move randomly
//                TODO: decide where to move. away from bullet/s?
                RobotPlayer.tryMove(RobotPlayer.randomDirection());

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }
        }
    }

}
