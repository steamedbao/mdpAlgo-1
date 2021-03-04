package algo;

import entities.*;
import connection.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static constant.EntitiesConstants.*;

/**
 * Fastest path algorithm using A* search + customized score functions
 */
public class FastestPathAlgorithmRunner implements AlgorithmRunner {

    private int sleepDuration;
    private int mWayPointX = -1;
    private int mWayPointY = -1;

    private static final int START_X = 0;
    private static final int START_Y = 17;
    private static final int GOAL_X = 12;
    private static final int GOAL_Y = 0;

    public FastestPathAlgorithmRunner(int speed) {
        sleepDuration = 1000 / speed;
    }

    public FastestPathAlgorithmRunner(int speed, int x, int y) {
        sleepDuration = 1000 / speed;
        mWayPointX = x;
        mWayPointY = y;
    }

    @Override
    public void run(GridMap grid, Robot robot, boolean realRun) {
        robot.reset();

        // receive waypoint
        int wayPointX = mWayPointX - 1, wayPointY = mWayPointY;
        System.out.println(wayPointX + wayPointY);

        // if (realRun && wayPointX == -1 && wayPointY == -1) {
        // // receive from Android
        // System.out.println("Waiting for waypoint");
        // //SocketMgr.getInstance().clearInputBuffer();
        // String msg = SocketMgr.getInstance().receiveMessage();
        // List<Integer> waypoints;
        // while ((waypoints = MessageMgr.parseMessage(msg)) == null) {
        // msg = SocketMgr.getInstance().receiveMessage();
        // }
        // // the coordinates in fastest path search is different from real grid
        // coordinate
        // wayPointX = waypoints.get(0)-1;
        // wayPointY = waypoints.get(1)-1;
        // } else if (!realRun) {
        if (!realRun) {
            // ignore waypoint for simulation
            wayPointX = START_X;
            wayPointY = START_Y;
        }

        // run from start to waypoint and from waypoint to goal
        System.out.println("Fastest path algorithm started with waypoint " + wayPointX + "," + wayPointY);
        Robot fakeRobot = new Robot(new GridMap(), new ArrayList<>());
        // y need to plus 1 need trial and error if unlucky

        List<String> path1 = AlgorithmRunner.runAstar(START_X, START_Y, wayPointX, wayPointY, grid, fakeRobot);
        List<String> path2 = AlgorithmRunner.runAstar(wayPointX, wayPointY, GOAL_X, GOAL_Y, grid, fakeRobot);

        if (path1 != null && path2 != null) {
            path1.addAll(path2);
            String compressedPath = AlgorithmRunner.compressPath(path1);
            System.out.println("Algorithm finished, executing actions");
            System.out.println(path1.toString());
            System.out.println(compressedPath.toString());

            if (realRun) {
                //// INITIAL CALIBRATION
                // if (realRun) {
                // SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "C");
                // SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "R");
                // SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "C");
                // SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "R");
                // }
                // SEND ENTIRE PATH AT ONCE
                // String compressedPath = AlgorithmRunner.compressPath(path1);
                // System.out.println(compressedPath.toString());

                // AR,PC,fastestpath
                SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, compressedPath);
                // SIMULATE AT THE SAME TIME
                for (String action : path1) {
                    if (action.equals("M")) {
                        robot.move();
                    } else if (action.equals("L")) {
                        robot.turn(LEFT);
                    } else if (action.equals("R")) {
                        robot.turn(RIGHT);
                    } else if (action.equals("U")) {
                        robot.turn(LEFT);
                        robot.turn(LEFT);
                    }
                    takeStep();
                }
            } else {
                for (String action : path1) {
                    if (action.equals("M")) {
                        robot.move();
                    } else if (action.equals("L")) {
                        robot.turn(LEFT);
                    } else if (action.equals("R")) {
                        robot.turn(RIGHT);
                    } else if (action.equals("U")) {
                        robot.turn(LEFT);
                        robot.turn(LEFT);
                    }
                    takeStep();
                }
            }
        } else {
            System.out.println("Fastest path not found!");
        }
    }

    /**
     * Pause the simulation for sleepDuration
     */
    private void takeStep() {
        try {
            Thread.sleep(sleepDuration);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
