package support;

import server.util.RandomProvider;

public class FixedRandomProvider implements RandomProvider {
	private final int value;

	public FixedRandomProvider(int value) {
		this.value = value;
	}

	@Override
	public int nextInt(int bound) {
		return value % bound;
	}
}
