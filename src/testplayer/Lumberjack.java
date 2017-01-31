package testplayer;
import battlecode.common.*;
import testplayer.RobotPlayer;

public strictfp class Lumberjack {
    RobotController rc;
    
    public Lumberjack(RobotController rc){
        this.rc = rc;
    }
    
    /*
     * TODO:
     * Sense and update trees each turn on the turn marker
     * Switch between attacking (staying in place) and non-attacking (looking for a target from the channels) modes
     */
    
    
    //Active turn limit
    static int PHASE_1_ACTIVE_TURN_LIMIT = 60;
    
    //Turns it will move away until inactive
    static int MOVE_AWAY_TURNS = 5;
    
    /**
     * A lumberjack will identify which archon group it belongs to via checking the three sets of archon locations and 
     * identifying which it is closest to.<br>
     * For the first 60 turns, it will be an active lumberjack and move in the direction of its leader archon every turn by 
     * reading the movement direction of said archon while chopping all neutral or enemy trees. <br>
     * While active, it will try to attack the target location specified by the archon as broadcasted in the channel <br>
     * After 60 turns, it will stop being an active lumberjack and move in the general opposite direction of the archon
     * while attacking enemies in strike range or going after and chopping trees.<br>
     * <br>
     *
     * Channels:<br>
     * <ul>
     * <li>4 - 6 -> Archon movement direction
     * <li>7 - 9 -> Archon location x
     * <li>10-12 -> Archon location y
     * <li>16-18 -> Immediate Target x
     * <li>19-21 -> Immediate Target y
     * <li>22-24 -> Target type (1 for tree, 2 for robot)
     * <li>25-27 -> Target id 
     * </ul>
     *<br>
     *
     * Movement:<br>
     * Moves every turn.
     *<br>
     */
    
    void runLumberjack() throws GameActionException {
        System.out.println("I'm a lumberjack!");
        Team ownTeam = rc.getTeam();
        Team enemy = ownTeam.opponent();
        boolean hasTarget = false;
        int currentTargetId = -1;
        int archonNum = RobotPlayer.getNearestArchon();
        //boolean hasSentDyingBroadcast = false;
        int turnCount = 0;

        while (turnCount < PHASE_1_ACTIVE_TURN_LIMIT) {
            System.out.println("I am a lumberjack on turn " + turnCount);
            try {
                if(!hasTarget){
                    
                    int targetType = rc.readBroadcast(RobotPlayer.TARGET_TYPE*3 + archonNum);
                    int targetTreeId = rc.readBroadcast(RobotPlayer.TARGET_ID*3 + archonNum);
                    //If the target specified by the archon is a tree and can be sensed by the lumberjack, 
                    //it should move toward that general direction
                    if(targetType == 1 && rc.canSenseTree(targetTreeId)){
                        RobotPlayer.moveTowards(rc.senseTree(targetTreeId).getLocation(), rc);
                    }
                    //List of trees that the lumberjack can chop/shake
                    TreeInfo[] trees = rc.senseNearbyTrees();
                    if(currentTargetId < 0){
                        boolean hasShaken = false;
                        boolean hasChopped = false;
                        for (TreeInfo tree : trees) {
                            int treeId = tree.getID();
                            if (!hasShaken && rc.canShake(treeId)) {
                                rc.shake(treeId);
                                hasShaken = true;
                            }
                            if (!hasChopped && !tree.getTeam().equals(ownTeam) && rc.canChop(treeId)) {
                                //If the tree will survive until the next turn, keep it as a target
                                if(tree.getHealth() > GameConstants.LUMBERJACK_CHOP_DAMAGE)
                                    currentTargetId = tree.getID();
                                rc.chop(treeId);
                                hasTarget = true;
                                hasChopped = true;
                                
                            }
                            if (hasChopped && hasShaken) {
                                break;
                            }
                        }
                    }
                    else if (rc.canSenseTree(currentTargetId)){
                        //Current target id is only positive when there is an actual target that was chopped previously
                        TreeInfo targetTree = rc.senseTree(currentTargetId);
                        RobotPlayer.moveTowards(targetTree.getLocation(), rc);
                        if (rc.canShake(currentTargetId)) {
                            rc.shake(currentTargetId);
                        }
                        if (rc.canChop(currentTargetId)) {
                            //If the tree will survive until the next turn, keep it as a target
                            if(targetTree.getHealth() < GameConstants.LUMBERJACK_CHOP_DAMAGE)
                                //If the tree dies, we reset the target to none.
                                currentTargetId = -1;
                            rc.chop(currentTargetId);
                        }
                    }
                }
                else{
                    TreeInfo[] nearbyNeutralTrees = rc.senseNearbyTrees(rc.getType().bodyRadius + GameConstants.INTERACTION_DIST_FROM_EDGE, Team.NEUTRAL);
                    TreeInfo[] nearbyEnemyTrees = rc.senseNearbyTrees(rc.getType().bodyRadius + GameConstants.INTERACTION_DIST_FROM_EDGE, rc.getTeam().opponent());
                    if(nearbyEnemyTrees.length > 0 || nearbyNeutralTrees.length > 0){
                        if(nearbyEnemyTrees.length > 0){
                            if(rc.canChop(nearbyEnemyTrees[0].getID()))
                                rc.chop(nearbyEnemyTrees[0].getID());
                            if(rc.canShake(nearbyEnemyTrees[0].getID()))
                                rc.shake(nearbyEnemyTrees[0].getID());
                        }
                        else if(nearbyNeutralTrees.length > 0){
                            if(rc.canChop(nearbyNeutralTrees[0].getID()))
                                rc.chop(nearbyNeutralTrees[0].getID());
                            if(rc.canShake(nearbyNeutralTrees[0].getID()))
                                rc.shake(nearbyNeutralTrees[0].getID());
                        }      
                    }
                    else{
                        hasTarget = false;
                    }
                }
                
                turnCount +=1;
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Lumberjack Exception");
                e.printStackTrace();
            }
        }
        while(turnCount >=PHASE_1_ACTIVE_TURN_LIMIT){
            hasTarget = false;
            try {
                System.out.println("I am a lumberjack on turn " + turnCount);
                RobotPlayer.dodge(4); //based on max bullet speed
                if(!hasTarget){
                    RobotPlayer.updateTreeLocs(archonNum);
                    RobotPlayer.updateEnemyRobotLocs(archonNum);
                    TreeInfo[] nearbyTrees = rc.senseNearbyTrees();
                    if(nearbyTrees.length == 0){
                        RobotPlayer.tryMoveInGeneralDirection(rc.getLocation().directionTo(RobotPlayer.readTreeLocation(archonNum)));
                        RobotPlayer.tryMoveInGeneralDirection(rc.getLocation().directionTo(RobotPlayer.readRobotLocation(archonNum)));
                    }
                    for(TreeInfo nearbyTree: nearbyTrees){
                        if(!nearbyTree.getTeam().equals(ownTeam)){
                            if(!rc.hasMoved() && rc.canMove(rc.getLocation().directionTo(nearbyTree.getLocation()))){
                                rc.move(rc.getLocation().directionTo(nearbyTree.getLocation()));
                                break;
                            }
                            if(rc.canChop(nearbyTree.getID())){
                                rc.chop(nearbyTree.getID());
                                hasTarget = true;
                            }
                        }
                    }
                    RobotPlayer.clearTreeLocs(archonNum);
                    RobotPlayer.clearEnemyLocs(archonNum);
                    
                }
                else{
                    boolean hasChopped = false;
                    TreeInfo[] nearbyTrees = rc.senseNearbyTrees(rc.getType().bodyRadius + GameConstants.INTERACTION_DIST_FROM_EDGE);
                    for(TreeInfo nearbyTree : nearbyTrees){
                        if(!nearbyTree.getTeam().equals(ownTeam)){
                            if(rc.canShake(nearbyTree.getID())){
                                rc.shake(nearbyTree.getID());
                            }
                            if(rc.canChop(nearbyTree.getID())){
                                rc.chop(nearbyTree.getID());
                                hasChopped = true;
                            }
                        }
                    }
                    hasTarget = hasChopped;
                }
                RobotInfo[] nearbyEnemyRobots = rc.senseNearbyRobots(rc.getType().bodyRadius + GameConstants.INTERACTION_DIST_FROM_EDGE, enemy);
                RobotInfo[] nearbyAllyRobots = rc.senseNearbyRobots(rc.getType().bodyRadius + GameConstants.INTERACTION_DIST_FROM_EDGE, ownTeam);
                if(nearbyEnemyRobots.length > nearbyAllyRobots.length && rc.canStrike()){
                    rc.strike();
                }
                Clock.yield();
            } catch (Exception e) {
                System.out.println("Lumberjack Exception");
                e.printStackTrace();
            }   
        }
    }
}



////send a broadcast to the archon controlling it if it's dying so that it gets replaced
//if (!hasSentDyingBroadcast && RobotPlayer.isDying()) {
//  int channel = archonNum + RobotPlayer.MAX_ARCHONS;
//  int previousCount = rc.readBroadcast(channel);
//  rc.broadcast(channel, previousCount+1);
//  hasSentDyingBroadcast = true;
//}
