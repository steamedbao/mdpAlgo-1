package algo;

import connection.CommMgr;
import connection.SocketMgr;
import entities.GridMap;
import entities.Robot;

import java.util.ArrayList;
import java.util.List;

import static constant.EntitiesConstants.*;

/**
 * Algorithm for exploration phase (full exploration)
 */
public class ImageExpAlgorithmRunner implements AlgorithmRunner {

    private int sleepDuration;
    private static final int START_X = 0;
    private static final int START_Y = 17;
    private static final int CALIBRATION_LIMIT = 4;

    public ImageExpAlgorithmRunner(int speed) {
        sleepDuration = 1000 / speed;
    }

    @Override
    public void run(GridMap grid, Robot robot, boolean realRun) {
        grid.reset();
        robot.reset();
        if (realRun) {
            grid.reset();
            String msg = SocketMgr.getInstance().receiveMessage(false);
            while (!msg.equals("startIMGexp")) {
                msg = SocketMgr.getInstance().receiveMessage(false);
                // SocketMgr.getInstance().sendMessage("A","M");
            }

        }
        // SELECT EITHER ONE OF THE METHODS TO RUN ALGORITHMS.
        runExplorationAlgorithmThorough(grid, robot, realRun);
        // runExplorationLeftWall(grid, robot, realRun);

        // CALIBRATION AFTER EXPLORATION
        calibrateAndTurn(robot, realRun);

        // GENERATE MAP DESCRIPTOR, SEND TO ANDROID
        String part1 = grid.generateDescriptorPartOne();
        String part2 = grid.generateDescriptorPartTwo();
        SocketMgr.getInstance().sendMessage(TARGET_ANDROID, CommMgr.generateFinalDescriptor(part1, part2));
    }

    private void calibrateAndTurn(Robot robot, boolean realRun) {
        if (realRun) {
            while (robot.getOrientation() != SOUTH) {
                robot.turn(LEFT);
                SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "L");
            }
            // SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "C");
            // SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "R");
            // SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "C");
            // SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "R");
        }
    }

    private void runExplorationAlgorithmThorough(GridMap grid, Robot robot, boolean realRun) {
        boolean endZoneFlag = false;
        boolean startZoneFlag = false;

        // CALIBRATE & SENSE
        int calibrationCounter = 0;
        // if (realRun) {
        // calibrateAtStart();
        // }
        robot.sense(realRun);

        // INITIAL UPDATE OF MAP TO ANDROID

        if (realRun)
            SocketMgr.getInstance().sendMessage(TARGET_ANDROID,
                    CommMgr.generateMapDescriptorMsg(grid.generateDescriptorPartOne(), grid.generateDescriptorPartTwo(),
                            robot.getCenterPosX(), robot.getCenterPosY(), robot.getOrientation()));

        // MAIN LOOP (LEFT-WALL-FOLLOWER)
        while (!endZoneFlag || !startZoneFlag) {
            calibrationCounter = moveAndSense(grid, robot, realRun, calibrationCounter);
            takePhoto(robot, grid, realRun);

            System.out.println(endZoneFlag + " " + startZoneFlag + " " + robot.getPosX() + "," + robot.getPosY());
            if (GridMap.isInEndZone(robot.getPosX(), robot.getPosY())) {
                endZoneFlag = true;
            }
            if (endZoneFlag && GridMap.isInStartZone(robot.getPosX() + 2, robot.getPosY())) {
                startZoneFlag = true;
            }

            // IF EXPLORATION COMPLETED & HAVE NOT GO BACK TO START, FIND THE FASTEST PATH
            // BACK TO START POINT
            if (grid.checkExploredPercentage() == 100 && !startZoneFlag) {
                findPathAndMove(grid, robot, START_X, START_Y, realRun);

                if (endZoneFlag && GridMap.isInStartZone(robot.getPosX() + 2, robot.getPosY())) {
                    startZoneFlag = true;
                }
                // AT THIS STAGE, ROBOT SHOULD HAVE RETURNED BACK TO START POINT.
            }
        }

        //
        // BELOW IS THE 2ND EXPLORATION !!!!!!!!!!!!!
        //
        // INITIALISE NEW GridMap TO PREVENT CHECKING PREVIOUSLY EXPLORED CELLS.
        GridMap exploreChecker = new GridMap();
        for (int x = 0; x < MAP_COLS; x++) {
            for (int y = 0; y < MAP_ROWS; y++) {
                exploreChecker.setExplored(x, y, grid.getIsExplored(x, y));
                exploreChecker.setIsObstacle(x, y, grid.getIsObstacle(x, y));
            }
        }

        // SWEEPING THROUGH UNEXPLORED, BUT REACHABLE CELLS WITHIN ARENA.
        if (grid.checkExploredPercentage() < 100.0) { // CHECK FOR UNEXPLORED CELLS
            System.out.println("NOT FULLY EXPLORED, DOING A 2ND RUN!");

            for (int y = MAP_ROWS - 1; y >= 0; y--) {
                for (int x = 0; x <= MAP_COLS - 1; x++) {
                    // CHECK FOR UNEXPLORED CELLS && CHECK IF NEIGHBOURS ARE REACHABLE OR NOT
                    if (!grid.getIsExplored(x, y) && (findPathAndMove(grid, robot, x, y, realRun)
                            || findPathAndMove(grid, robot, x + 1, y, realRun)
                            || findPathAndMove(grid, robot, x - 1, y, realRun)
                            || findPathAndMove(grid, robot, x, y + 1, realRun)
                            || findPathAndMove(grid, robot, x, y - 1, realRun))) {
                        System.out.println("found path");
                        robot.sense(realRun);
                        calibrationCounter = moveAndSense(grid, robot, realRun, calibrationCounter);
                        takePhoto(robot, grid, realRun);
                    }
                    while (exploreChecker.getIsExplored(robot.getPosX(), robot.getPosY()) != grid
                            .getIsExplored(robot.getPosX(), robot.getPosY())) {
                        calibrationCounter = moveAndSense(grid, robot, realRun, calibrationCounter);
                        takePhoto(robot, grid, realRun);
                    }
                    if (grid.checkExploredPercentage() == 100) { // IF FULLEST EXPLORED, EXIT AND GO TO START
                        break;
                    }
                }
            }
        }
        /*
         * FASTEST PATH BACK TO START ONCE THE EXPLORATION IS COMPLETED.
         */
        if (!GridMap.isInStartZone(robot.getPosX() + 2, robot.getPosY() + 2)) {
            findPathAndMove(grid, robot, START_X, START_Y, realRun);
        }
        System.out.println("EXPLORATION COMPLETED!");
        System.out.println("PERCENTAGE OF AREA EXPLORED: " + grid.checkExploredPercentage() + "%!");
        robot.reset();
    }

    private boolean findPathAndMove(GridMap grid, Robot robot, int x, int y, boolean realRun) {
        Robot fakeRobot = new Robot(grid, new ArrayList<>());
        fakeRobot.setPosX(robot.getPosX());
        fakeRobot.setPosY(robot.getPosY());
        fakeRobot.setOrientation(robot.getOrientation());
        List<String> returnPath = AlgorithmRunner.runAstar(robot.getPosX(), robot.getPosY(), x, y, grid, fakeRobot);

        if (returnPath != null) {
            System.out.println("A* Algorithm finished, executing actions");
            System.out.println(returnPath.toString());
            if (realRun) {
                fakeRobot.setPosX(robot.getPosX());
                fakeRobot.setPosY(robot.getPosY());
                fakeRobot.setOrientation(robot.getOrientation());
                String compressed = AlgorithmRunner.compressPathForExploration(returnPath, fakeRobot);
                SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, compressed);
            }

            for (String action : returnPath) {
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
                // robot.sense(realRun);
                if (realRun)
                    SocketMgr.getInstance().sendMessage(TARGET_ANDROID,
                            CommMgr.generateMapDescriptorMsg(grid.generateDescriptorPartOne(),
                                    grid.generateDescriptorPartTwo(), robot.getCenterPosX(), robot.getCenterPosY(),
                                    robot.getOrientation()));
                if (!realRun)
                    stepTaken();
            }
            return true;
        } else {
            System.out.println("Fastest path not found!");
            return false;
        }
    }

    private int moveAndSense(GridMap grid, Robot robot, boolean realRun, int calibrationCounter) {
        // CALIBRATION
        if (realRun) {
            calibrationCounter++;
            // IF CAN CALIBRATE FRONT, TAKE THE OPPORTUNITY
            // OTHERWISE CALIBRATE LEFT
            if (calibrationCounter >= CALIBRATION_LIMIT) {
                if (robot.canCalibrateFront() && robot.canCalibrateLeft()) {
                    SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "CFL");
                    calibrationCounter = 0;
                } else if (robot.canCalibrateLeft()) {
                    SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "CL");
                    calibrationCounter = 0;
                }
            }
        } else {
            // SHOW CALIBRATION PROCESS ON SIMULATOR
            calibrationCounter++;
            if (calibrationCounter >= CALIBRATION_LIMIT) {
                if (robot.canCalibrateFront() && robot.canCalibrateLeft()) {
                    System.out.println("CFL");
                    calibrationCounter = 0;
                } else if (robot.canCalibrateLeft()) {
                    System.out.println("CL");
                    calibrationCounter = 0;
                }
            }
        }

        boolean turned = leftWallFollower(robot, grid, realRun);
        if (turned) {
            // SENSE AFTER CALIBRATION
            senseAndUpdateAndroid(robot, grid, realRun);
        }

        // MOVE FORWARD
        if (realRun)
            SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "1");
        robot.move();
        stepTaken();

        // // CALIBRATION
        //// if (realRun) {
        //// calibrationCounter++;
        //// // IF CAN CALIBRATE FRONT, TAKE THE OPPORTUNITY
        //// // OTHERWISE CALIBRATE LEFT
        //// if (robot.canCalibrateFront()) {
        //// SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "C");
        //// calibrationCounter = 0;
        //// } else if (calibrationCounter >= CALIBRATION_LIMIT &&
        // robot.canCalibrateLeft()) {
        //// SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "L");
        //// SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "C");
        //// SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "R");
        //// calibrationCounter = 0;
        //// }
        //// }
        // else {
        // //SHOW CALIBRATION PROCESS ON SIMULATOR
        // calibrationCounter++;
        // System.out.println("calibration c: "+ calibrationCounter);
        // if (calibrationCounter >= CALIBRATION_LIMIT && robot.canCalibrateLeft()){
        // robot.turn(LEFT);
        // robot.turn(RIGHT);
        // calibrationCounter = 0;
        // } else if (robot.canCalibrateFront()) {
        // calibrationCounter = 0;
        // }
        // }

        // SENSE AFTER CALIBRATIONx
        senseAndUpdateAndroid(robot, grid, realRun);
        return calibrationCounter;
    }

    private void takePhoto(Robot robot, GridMap grid, boolean realRun) {
        if (robot.canTakePhotoFront()) {
            System.out.println("Taking photo for front obstacle.");
            if (realRun)
                SocketMgr.getInstance().sendMessage(TARGET_RPI, "P");
            else
                stepTaken();
        }
        if (robot.canTakePhotoLeft()) {
            System.out.println("Taking photo for left obstacle.");
            if (realRun) {
                SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "L");
                SocketMgr.getInstance().sendMessage(TARGET_RPI, "P");
                SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "R");
            } else {
                robot.turn(LEFT);
                stepTaken();
                robot.turn(RIGHT);
            }
        }
    }

    /**
     * Checks if a turn is necessary and which direction to turn
     * 
     * @param robot
     * @param grid
     * @param realRun
     * @return whether a turn is performed
     */
    private boolean leftWallFollower(Robot robot, GridMap grid, boolean realRun) {
        if (robot.isObstacleAhead()) {
            if (robot.isObstacleRight() && robot.isObstacleLeft()) {
                System.out.println("OBSTACLE DETECTED! (ALL 3 SIDES) U-TURNING");
                if (realRun)
                    SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "U");
                robot.turn(RIGHT);
                robot.turn(RIGHT);
                if (!realRun)
                    stepTaken();
            } else if (robot.isObstacleLeft()) {
                System.out.println("OBSTACLE DETECTED! (FRONT + LEFT) TURNING RIGHT");
                if (realRun)
                    SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "R");
                robot.turn(RIGHT);
                if (!realRun)
                    stepTaken();
            } else {
                System.out.println("OBSTACLE DETECTED! (FRONT) TURNING LEFT");
                if (realRun)
                    SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "L");
                robot.turn(LEFT);
                if (!realRun)
                    stepTaken();
            }
            System.out.println("-----------------------------------------------");

            return true; // TURNED
        } else if (!robot.isObstacleLeft()) {
            System.out.println("NO OBSTACLES ON THE LEFT! TURNING LEFT");
            if (realRun)
                SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "L");
            robot.turn(LEFT);
            if (!realRun)
                stepTaken();
            System.out.println("-----------------------------------------------");

            return true; // TURNED
        }
        return false; // DIDN'T TURN
    }

    private void stepTaken() {
        /*
         * MAKE IT MOVE SLOWLY SO CAN SEE STEP BY STEP MOVEMENT
         */
        try {
            Thread.sleep(sleepDuration);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //
    // private void calibrateAtStart() {
    // SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "C");
    // SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "R");
    // SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "C");
    // SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "R");
    // }

    private void senseAndUpdateAndroid(Robot robot, GridMap grid, boolean realRun) {
        robot.sense(realRun);
        if (realRun) {
            SocketMgr.getInstance().sendMessage(TARGET_ANDROID,
                    CommMgr.generateMapDescriptorMsg(grid.generateDescriptorPartOne(), grid.generateDescriptorPartTwo(),
                            robot.getCenterPosX(), robot.getCenterPosY(), robot.getOrientation()));
        }
    }
}