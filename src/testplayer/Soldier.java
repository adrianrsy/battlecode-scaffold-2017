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
     * For Phase 1<br>
     * A gardener will identify which archon group it belongs to via checking the three sets of archon locations and 
     * identifying which it is closest to.<br>
     * For the first 60 turns, it will be an active gardener and move in the direction of its leader archon every other turn 
     * by reading the movement direction of said archon while trying to build other robots behind it for other purposes, 
     * e.g. lumberjacks for terrain clearing, scouts for harassing/scouting, soldiers for basic defense.<br>
     * While active, it will check for the phase number each turn and perform these tasks/switch to a different task.
     * After 60 turns, it will stop being an active gardener by signaling to the broadcast that it is no longer active, 
     * i.e. decrement the active gardener counter. Upon being inactive, it will spend 5 turns moving away from the archon 
     * then planting trees around itself in a hexagonal fashion.<br>
     * When inactive, it will water the adjacent tree with the lowest hp.<br>
     *<br>
     *
     * Channels:<br>
     * <ul>
     * <li>1 - 3 -> Phase number
     * <li>4 - 6 -> Archon movement direction
     * <li>7 - 9 -> Archon location x
     * <li>10-12 -> Archon location y
     * <li>13-15 -> Living Gardener channel
     * </ul>
     *<br>
     *
     * Movement:<br>
     * Moves once every two turns in the same direction as the archon it belongs to
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
