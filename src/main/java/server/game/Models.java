package server.game;

import java.util.*;

public class Models {

	public static final int GOAL = 20;

	// 플레이어의 말
	public static class Piece {
		public final String id;
		public final String teamId;
		public int pos = 0;
		public int stacked = 1;

		public Piece(String id, String teamId) {
			this.id = id;
			this.teamId = teamId;
		}

		public boolean finished() {
			return pos >= GOAL;
		}
	}

	// 팀 (2명)
	public static class Team {
		public final String id;
		public final List<Piece> pieces = new ArrayList<>();

		public Team(String id) {
			this.id = id;
		}

		public List<Piece> onBoard() {
			return pieces.stream().filter(p -> !p.finished()).toList();
		}
	}

	// 보드 (20칸)
	public static class Board {
		public int clamp(int pos) {
			return Math.max(0, Math.min(GOAL, pos));
		}
	}
}
