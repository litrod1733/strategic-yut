package server.game;

import javax.print.attribute.standard.MediaSize;

public final class Throwing {

	public enum Outcome {
		BACKDO(-1), GAE(2), GEOL(3), YUT(4), MO(5);
		public final int steps;
		Outcome(int s) {
			this.steps = s;
		}
	}

	public static Outcome mapFrontCount(int fronts) {
		return switch (fronts) {
			case 0 -> Outcome.MO;
			case 1 -> Outcome.BACKDO;
			case 2 -> Outcome.GAE;
			case 3 -> Outcome.GEOL;
			case 4 -> Outcome.YUT;
			default -> throw new IllegalArgumentException("fronts must be 0..4");
		};
	}

	public static boolean isExtraThrow(Outcome o) {
		return (o == Outcome.YUT || o == Outcome.MO);
	}
}
