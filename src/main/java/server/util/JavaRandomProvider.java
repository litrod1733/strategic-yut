package server.util;

import java.util.Random;

public class JavaRandomProvider implements RandomProvider {
	private final Random rnd = new Random();

	@Override
	public int nextInt(int bound) {
		return rnd.nextInt(bound);
	}
}
