package server.game;

import java.util.*;

public class TurnManager {
	private final Queue<String> teamOrder = new ArrayDeque<>();
	private String currentTeamId;

	public TurnManager(List<String> teamIds) {
		teamOrder.addAll(teamIds);
		currentTeamId = teamOrder.peek();
	}

	public String getCurrentTeamId() {
		return currentTeamId;
	}

	public void nextTurn() {
		teamOrder.add(teamOrder.poll());
		currentTeamId = teamOrder.peek();
	}

	public boolean isMyTurn(String teamId) {
		return teamId.equals(currentTeamId);
	}
}
