package server.game;

import server.util.RandomProvider;

import java.util.*;
import static server.game.Models.*;

public class Rules {
	private final Board board;
	private final RandomProvider rnd;

	public Rules(Board board, RandomProvider rnd) {
		this.board = board;
		this.rnd = rnd;
	}

	public record MoveResult(boolean captured, String victimId, int newPos) {}

	public MoveResult movePiece(List<Team> teams, Team myTeam, Piece piece, int steps, Tokens tokens) {
		if (piece.finished()) throw new IllegalStateException("already finished");

		int next = board.clamp(piece.pos + steps);
		Piece victim = findOpponentAt(teams, myTeam.id, next);

		piece.pos = next;

		if (victim != null) {
			victim.pos = 0;	// 잡힌 말은 시작점으로
			int randomFronts = rnd.nextInt(5);
			int rewardSteps = Throwing.mapFrontCount(randomFronts).steps;
			tokens.add(rewardSteps);	// 즉시 토큰 +1
			return new MoveResult(true, victim.id, piece.pos);
		}
		return new MoveResult(false, null, piece.pos);
	}

	private Piece findOpponentAt(List<Team> teams, String myTeamId, int position) {
		for (Team t : teams) {
			if (t.id.equals(myTeamId)) continue;
			for (Piece p : t.pieces) {
				if (!p.finished() && p.pos == position) return p;
			}
		}
		return null;
	}

	// 업기(같은 팀의 말이 같은 칸일 때)
	public void tryStack(Team team) {
		Map<Integer, List<Piece>> map = new HashMap<>();
		for (Piece p : team.pieces) {
			if (!p.finished()) map.computeIfAbsent(p.pos, k -> new ArrayList<>()).add(p);
		}
		for (var e : map.entrySet()) {
			List<Piece> list = e.getValue();
			if (list.size() >= 2) {
				Piece base = list.get(0);
				for (int i = 1; i < list.size(); i++) {
					base.stacked += list.get(i).stacked;
					list.get(i).stacked = 0;
				}
			}
		}
	}
}
