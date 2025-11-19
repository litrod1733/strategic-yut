package server.game;

import server.net.ClientHandler;

import java.util.*;
import static server.game.Models.*;

public class TurnManager {
	private final List<Models.Team> teams;
	private final Rules rules;
	private final Tokens tokens = new Tokens();
	private final Queue<String> teamOrder = new ArrayDeque<>();
	private String currentTeamId;
	private boolean gameStarted = false;
	private String winnerTeamId = null;

	private final Map<String, Models.Player> players = new HashMap<>();

	public enum Phase { CHOOSE, ALLOCATE }
	private Phase phase = Phase.CHOOSE;

	public TurnManager(List<Models.Team> teams, Rules rules, List<String> teamIds) {
		this.teams = teams;
		this.rules = rules;
		teamOrder.addAll(teamIds);
		currentTeamId = teamOrder.peek();
	}

	public void addPlayer(Models.Player p) {
		players.put(p.id, p);
	}

	public Models.Player getPlayer(String nickname) {
		return players.get(nickname);
	}

	public Collection<Models.Player> getPlayers() {
		return players.values();
	}

	public String getCurrentTeamId() {
		return currentTeamId;
	}

	public void startGame() {
		if (gameStarted) return;
		gameStarted = true;
		phase = Phase.CHOOSE;
	}

	public boolean isGameStarted() {
		return gameStarted;
	}

	public void startTurn() {
		phase = Phase.CHOOSE;
	}

	public void applyChoiceFronts(int fronts) {
		var outcome = Throwing.mapFrontCount(fronts);
		tokens.addOutcome(outcome);
		System.out.println("[Turn] " + currentTeamId + " chose fronts=" + fronts + " -> outcome=" + outcome);

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
//		System.out.println("[Turn] " + currentTeamId + "moved piece " + pieceId +
//			" -> pos=" + result.newPos() +
//			", captured=" + result.captured() +
//			", victim=" + result.victimId());

		rules.tryStack(my);

		if (winnerTeamId == null && isTeamFinished(my)) {
			winnerTeamId = my.id;
		}

		tokens.popFront();

//		System.out.printf("[Turn] Team %s moved piece %s -> pos=%d | captured=%s | victim=%s | tokens=%s | phase%s%n",
//			currentTeamId,
//			piece.id,
//			result.newPos(),
//			result.captured(),
//			result.victimId(),
//			tokens.asList(),
//			phase
//		);

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

	public List<Models.Team> getTeams() {
		return teams;
	}

	public boolean isGameOver() {
		return winnerTeamId != null;
	}

	public String getWinnerTeamId() {
		return winnerTeamId;
	}

	private boolean isTeamFinished(Team team) {
		for (Piece p : team.pieces) {
			if (!p.finished()) {
				return false;
			}
		}
		return true;
	}
}
