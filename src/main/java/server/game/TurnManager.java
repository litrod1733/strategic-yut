package server.game;

import java.util.*;
import static server.game.Models.*;

public class TurnManager {
	private final List<Models.Team> teams;
	private final Rules rules;
	private final Tokens tokens = new Tokens();
	private final Queue<String> teamOrder = new ArrayDeque<>();
	private String currentTeamId;

	public enum Phase { CHOOSE, ALLOCATE }
	private Phase phase = Phase.CHOOSE;

	public TurnManager(List<Models.Team> teams, Rules rules, List<String> teamIds) {
		this.teams = teams;
		this.rules = rules;
		teamOrder.addAll(teamIds);
		currentTeamId = teamOrder.peek();
	}

	public String getCurrentTeamId() {
		return currentTeamId;
	}

	public void startTurn() {
		phase = Phase.CHOOSE;
	}

	public void applyChoiceFronts(int fronts) {
		var outcome = Throwing.mapFrontCount(fronts);
		tokens.addOutcome(outcome);

		if (Throwing.isExtraThrow(outcome)) {
			phase = Phase.CHOOSE;
			return;
		}

		phase = Phase.ALLOCATE;
	}

	public Rules.MoveResult allocateMove(String pieceId, int steps) {
		if (phase != Phase.ALLOCATE)
			throw new IllegalStateException("Not in ALLOCATE phase");

		var my = findTeam(currentTeamId);
		var piece = findPiece(my, pieceId);

		var result = rules.movePiece(teams, my, piece, steps, tokens);
		rules.tryStack(my);
		tokens.popFront();

		if (!tokens.isEmpty()) {
			return result;
		}

		endTurn();
		return result;
	}

	public void nextTurn() {
		teamOrder.add(teamOrder.poll());
		currentTeamId = teamOrder.peek();
	}

	private void endTurn() {
		nextTurn();
		phase = Phase.CHOOSE;
	}

	public boolean isMyTurn(String teamId) {
		return teamId.equals(currentTeamId);
	}

	private Models.Team findTeam(String id) {
		for (var t : teams) {
			if (t.id.equals(id)) {
				return t;
			}
		}
			throw new NoSuchElementException("team not found: " + id);
	}

	private Models.Piece findPiece(Models.Team team, String pid) {
		for (var p : team.pieces) {
			if (p.id.equals(pid)) {
				return p;
			}
		}
		throw new NoSuchElementException("piece not found: " + pid);
	}

	public Phase getPhase() {
		return phase;
	}

	public List<Integer> getTokens() {
		return tokens.asList();
	}
}
