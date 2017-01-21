package testplayer;
import battlecode.common.*;
import testplayer.RobotPlayer;

//Goal during phase 1 is to move towards a corner and esstablish a base there.





public strictfp class Archon {
    static RobotController rc;
    
    //For channel numbers, get channel number, multiply by 3, then add archon number (from 1 to 3)
    static int PHASE_NUMBER_CHANNEL = 0;
    static int ARCHON_DIRECTION_RADIANS_CHANNEL = 1;
    static int ARCHON_LOCATION_X_CHANNEL = 2;
    static int ARCHON_LOCATION_Y_CHANNEL = 3;
    static int LIVING_GARDENERS_CHANNEL = 4;
    static int IMMEDIATE_TARGET_CHANNEL = 5;
    
    //Except for the channel that contains the round number
    static int ROUND_NUMBER_CHANNEL = 1000;
    
    //Direction variables
    static Direction NORTH = Direction.getNorth();
    static Direction SOUTH = Direction.getSouth();
    static Direction EAST = Direction.getEast();
    static Direction WEST = Direction.getWest();
    static Direction NORTH_EAST = new Direction((float) ((NORTH.radians + EAST.radians)/2.0));
    static Direction NORTH_WEST = new Direction((float) ((NORTH.radians + WEST.radians)/2.0));
    static Direction SOUTH_EAST = new Direction((float) ((SOUTH.radians + EAST.radians)/2.0));
    static Direction SOUTH_WEST = new Direction((float) ((SOUTH.radians + WEST.radians)/2.0));
    
    
    static void runArchon() throws GameActionException {
        System.out.println("I'm an archon!");
        int archonNum;
        int phaseNum = 1;
        
        //If first channel has not yet been assigned, this must be the first archon
        if(rc.readBroadcast(PHASE_NUMBER_CHANNEL*3 + 1) != 1){
            rc.broadcast(PHASE_NUMBER_CHANNEL*3 + 1, phaseNum);
            archonNum = 1;
        }
        //If second channel has not yet been assigned, this must be the second archon
        else if (rc.readBroadcast(PHASE_NUMBER_CHANNEL*3 + 2) != 1){
            rc.broadcast(PHASE_NUMBER_CHANNEL*3 + 2, 1);
            archonNum =2;
        }
        //Otherwise, it is the third archon
        else{
            rc.broadcast(PHASE_NUMBER_CHANNEL*3 + 3, 1);
            archonNum = 3;
        }
        
        int numRoundsRemaining;
        if(archonNum == 1){
            numRoundsRemaining = rc.getRoundNum();
            rc.broadcast(ROUND_NUMBER_CHANNEL, numRoundsRemaining);
        }
        else{
            numRoundsRemaining = rc.readBroadcast(ROUND_NUMBER_CHANNEL);
        }

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                int currentPhaseNum = rc.readBroadcast(PHASE_NUMBER_CHANNEL);
                switch(currentPhaseNum){
                    case 1:
                        runArchonPhase1(archonNum);
                }
                numRoundsRemaining -=1;
//                if(numRoundsRemaining == 0){
//                    float numBullets = rc.getTeamBullets();
//                    int numPoints = (int) ((numBullets)/GameConstants.BULLET_EXCHANGE_RATE);
//                    rc.donate(numPoints);
//                }
            } catch (Exception e) {
                System.out.println("Archon Exception");
                e.printStackTrace();
            }
        }
    }
    
    /**
     * For Phase 1<br>
     * In order, each turn an archon will attempt to move towards a corner, signal its movement direction and location, <br>
     * signal a target if there is an obstacle (neutral trees/enemy robots) preventing it from moving, <br>
     * build gardeners and increment the number of gardeners behind it if possible until a limit is reached, <br> 
     * then sense broadcasting enemies and attempt to predict and broadcast enemy archon location.<br>
     *<br>
     *
     * Channels:<br>
     * <ul>
     * <li>1 - 3 -> Phase number
     * <li>4 - 6 -> Archon movement direction
     * <li>7 - 9 -> Archon location x
     * <li>10-12 -> Archon location y
     * <li>13-15 -> Living Gardener channel
     * <li>16-18 -> Immediate Target channel
     * <li>1000  -> round number
     * </ul>
     *<br>
     *
     * Movement:<br>
     * <ul>
     * <li>If there is only one archon, it will attempt to move southwest.
     * <li>If there are two archons, the first will move southwest, the second will move southeast.
     * <li>If there are three archons, first will move southwest, second will move southeast, third will head northwest.
     * </ul>
     *<br>
     *
     * Signal movement direction:<br>
     * Each archon will signal the direction it moved in radians.<br>
     * Spawned units will follow this direction and will be spawned approximately opposite this direction<br>
     */
    
    static void runArchonPhase1(int archonNum) throws GameActionException{
        Direction headedTo;
        switch(archonNum){
        case 1:
            headedTo = SOUTH_WEST;
            break;
        case 2:
            headedTo = SOUTH_EAST;
            break;
        case 3:
            headedTo = NORTH_WEST;
            break;
        default:
            System.out.println("Invalid archon number");
            headedTo = SOUTH_WEST;
            break;
        }
        

    }
    
    /**
     * Attempts to move towards a certain point while avoiding small obstacles at an angle of at most 60 degrees.
     * @param loc location it is trying to move towards
     * @return true if the robot has successfully moved towards the point
     * @throws GameActionException
     */
    static boolean moveTowards(MapLocation loc) throws GameActionException{
        return RobotPlayer.tryMove(rc.getLocation().directionTo(loc), 60, 4);
    }
    
    /**
     * Attempts to move towards a certain direction while avoiding small obstacles at an angle of at most 60 degrees.
     * @param dir
     * @return true if the robot has successfully moved towards the point
     * @throws GameActionException
     */
    static boolean moveTowards(Direction dir) throws GameActionException{
        return RobotPlayer.tryMove(dir, 60, 4);
    }

}
