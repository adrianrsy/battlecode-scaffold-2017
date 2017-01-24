package testplayer;
import battlecode.common.*;
import testplayer.RobotPlayer;

public strictfp class Lumberjack {
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
    
    static void runLumberjack() throws GameActionException {
        System.out.println("I'm a lumberjack!");
        Team ownTeam = rc.getTeam();
        Team enemy = ownTeam.opponent();
        int currentTargetId = 0;
        int archonController = RobotPlayer.getNearestArchon();
        int currentPhase;
        boolean hasSentDyingBroadcast = false;

        while (true) {
            try {
                currentPhase = rc.readBroadcast(archonController);
                switch (currentPhase){
                case 1:
//                  TODO:
//                  get direction from archon movement
//                  check for trees in the way and chop them
//                  head that direction
                    TreeInfo[] trees = rc.senseNearbyTrees(1); //GameConstants.INTERACTION_DIST_FROM_EDGE;
                    boolean hasShaken = false;
                    boolean hasChopped = false;
                    for (TreeInfo tree : trees) {
                        int treeId = tree.ID;
                        if (!hasShaken && rc.canShake(treeId)) {
                            rc.shake(treeId);
                            hasShaken = true;
                        }
//                      FIXME: only chop when it is in the way?
                        if (!hasChopped && tree.team != ownTeam && rc.canChop(treeId)) {
                            rc.chop(treeId);
                            hasChopped = true;
                        }
                        if (hasChopped && hasShaken) {
                            break;
                        }
                    }
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

                // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
                RobotInfo[] robots = rc.senseNearbyRobots(RobotType.LUMBERJACK.bodyRadius+GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);

                if(robots.length > 0 && !rc.hasAttacked()) {
                    // Use strike() to hit all nearby robots!
                    rc.strike();
                } else {
                    // No close robots, so search for robots within sight radius
                    robots = rc.senseNearbyRobots(-1,enemy);

                    // If there is a robot, move towards it
                    if(robots.length > 0) {
                        MapLocation myLocation = rc.getLocation();
                        MapLocation enemyLocation = robots[0].getLocation();
                        Direction toEnemy = myLocation.directionTo(enemyLocation);

                        RobotPlayer.tryMove(toEnemy);
                    } else {
                        // Move Randomly
                        RobotPlayer.tryMove(RobotPlayer.randomDirection());
                    }
                }
                
//              send a broadcast to the archon controlling it if it's dying so that it gets replaced
                if (!hasSentDyingBroadcast && RobotPlayer.isDying()) {
                    int channel = archonController + RobotPlayer.MAX_ARCHONS;
                    int previousCount = rc.readBroadcast(channel);
                    rc.broadcast(channel, previousCount+1);
                    hasSentDyingBroadcast = true;
                }


                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Lumberjack Exception");
                e.printStackTrace();
            }
        }
    }

}
