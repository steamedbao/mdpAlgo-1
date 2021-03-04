package algo;

import static constant.EntitiesConstants.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import entities.GridMap;
import entities.Robot;
import entities.Sensor;
import simInterface.*;

public class MdpApp {
	public static void main(String args[]) {

		GridMap test = new GridMap();

		test.presetObstacles();
		test.printMapObstacle();

		GridMap grid = new GridMap();
		// setObstaclesMap(grid);
		Sensor sensor4 = new Sensor(6, 2, 0, RIGHT, 2);
		Sensor sensor5 = new Sensor(2, 0, 0, LEFT, 5);
		Sensor sensor3 = new Sensor(2, 2, 0, MIDDLE, 8);
		Sensor sensor1 = new Sensor(2, 0, 0, MIDDLE, 8);
		Sensor sensor2 = new Sensor(2, 1, 0, MIDDLE, 8);
		Sensor sensor6 = new Sensor(2, 0, 2, LEFT, 5);
		List<Sensor> sensors = new ArrayList<>();
		sensors.add(sensor1);
		sensors.add(sensor2);
		sensors.add(sensor3);
		sensors.add(sensor4);
		sensors.add(sensor5);
		sensors.add(sensor6);
		Robot robot = new Robot(grid, sensors);
		Simulator simulator = new Simulator(grid, robot);

		new ConnectButtonListener(simulator);
		new PrintHexButtonListener(simulator, grid, robot);
		new LoadMapButtonListener(simulator, robot, grid);
		new ExplorationButtonListener(simulator, robot, grid);
		new ReplayButtonListener(simulator, robot, grid);
		new ImageExpButtonListener(simulator, robot, grid);
		new FastestPathButtonListener(simulator, robot, grid);
		new TimeLimitedButtonListener(simulator, robot, grid);
		new CoverageLimitedButtonListener(simulator, robot, grid);
		new RealRunButtonListener(simulator, robot, grid);
		new RealRunCheckBoxListener(simulator);

		String testStr = "0700000000000001C00002000400080010202040408001000200040000380000000020004200";

		BigInteger testInt = new BigInteger(testStr, 16);
		String testBin = testInt.toString(2);

		int len = testStr.length() * 4;

		// left pad the string result with 0s if converting to BigInteger removes them.
		if (testBin.length() < len) {
			int diff = len - testBin.length();
			String pad = "";
			for (int i = 0; i < diff; ++i) {
				pad = pad.concat("0");
			}
			testBin = pad.concat(testBin);
		}

		System.out.println("testbin " + testBin);

		ArrayList<String> stringList = new ArrayList<String>();
		int count = 0;
		String tentativeStr = "";

		for (int i = 0; i < testBin.length(); i++) {
			if (count % 15 != 0 | count == 0) {
				tentativeStr = tentativeStr + testBin.charAt(i);
				count++;
			} else {
				stringList.add(tentativeStr + '\n');
				tentativeStr = "" + testBin.charAt(i);
				count = 1;
			}
		}

		System.out.println(stringList);
		System.out.println(stringList.size());

		ArrayList<String> stringList2 = new ArrayList<String>();
		for (int i = stringList.size() - 1; i >= 0; i--) {
			stringList2.add(stringList.get(i));
		}
		System.out.println(stringList2);
		String endStr = stringList2.toString();
		endStr = endStr.replace(", ", "").replace("[", "").replace("]", "");
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < endStr.length(); i++) {
			int j = 0;
			if (i < endStr.length() && i > 0) {
				j = i - 1;
			}
			if (i > 0 && endStr.charAt(j) != '\n') {
				result.append(" ");
			}
			result.append(endStr.charAt(i));
		}

		System.out.println(result);
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter("test1.txt"));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {

			out.write(result.toString());
		} catch (IOException e) {
			System.out.println("Exception ");

		} finally {
			try {
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		simulator.setVisible(true);
		System.out.println("Simulator started.");
	}

}
