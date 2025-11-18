package server.net;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import common.dto.Message;
import server.game.Models;
import server.game.TurnManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Router {
	private final Gson gson = new Gson();
	private final TurnManager turn;
	private final ConnectionHub hub;
	private final java.util.concurrent.Executor gameExec =
		java.util.concurrent.Executors.newSingleThreadExecutor();

	public Router(TurnManager turn, ConnectionHub hub) {
		this.turn = turn;
		this.hub = hub;
	}

	private Message buildState() {
		return new Message("STATE", Map.of(
			"phase", turn.getPhase().name(),
			"turn", turn.getCurrentTeamId(),
			"tokens", turn.getTokens(),
			"board", serializeBoard()
		));
	}

	public void handle(Message msg, ClientHandler ch) {
		gameExec.execute(() -> handleSync(msg, ch));
	}

	private void handleSync(Message msg, ClientHandler ch) {
		switch (msg.type) {
			case "JOIN" -> onJoin(msg.payload, ch);
			case "CHAT_ALL" -> onChatAll(msg.payload, ch);
			case "CHAT_T" -> onChatTeam(msg.payload, ch);
			case "CHOOSE" -> onChoose(msg.payload, ch);
			case "MOVE" -> onMove(msg.payload, ch);
			case "PING" -> onPing(ch);
			case "REQ_STATE" -> ch.send(buildState());

			default -> ch.send(new Message("ERROR", Map.of("message", "unknown type: " + msg.type)));
		}
	}

	private void onPing(ClientHandler ch) {
		ch.touch();
		ch.send(new Message("PONG", "ok"));
	}

	private boolean ensureTurn(ClientHandler ch) {
		if (!turn.getCurrentTeamId().equals(ch.getTeamId())) {
			ch.send(new Message("ERROR", "not your turn"));
			return false;
		}
		return true;
	}

	private void onJoin(Object payload, ClientHandler ch) {
		Map<String, Object> p = asMap(payload);

		String nickname = (String) p.getOrDefault("nickname", "anon");
		String teamId = (String) p.getOrDefault("teamId", "A");

		if (turn.getPlayer(nickname) != null) {
			ch.send(new Message("ERROR", "Nickname already in use."));
			return;
		}

		long teamCount = turn.getPlayers().stream().filter(pl -> pl.teamId.equals(teamId)).count();

		if (teamCount >= 2) {
			ch.send(new Message("ERROR", "Team " + teamId + " is full."));
			return;
		}

		if (turn.getPlayers().size() >= 4) {
			ch.send(new Message("ERROR", "Game is full (4 players max)."));
			return;
		}

		ch.setNickname(nickname);
		ch.setTeamId(teamId);

		Models.Player player = new Models.Player(nickname, teamId);
		turn.addPlayer(player);

		boolean hasLeader = turn.getPlayers().stream().anyMatch(pl -> teamId.equals(teamId) && pl.isLeader);

		if (!hasLeader) {
			player.isLeader = true;
			hub.broadcastTeam(teamId, new Message("SYS", nickname + " is the team leader."));
		}

		ch.send(new Message("JOIN_OK", Map.of("teamId", teamId, "nickname", nickname, "isLeader", player.isLeader)));

		hub.broadcast(new Message("SYS", nickname + " joined (" + teamId + ")"));

		hub.broadcast(new Message("PLAYER_COUNT", turn.getPlayers().size()));

		if (turn.getPlayers().size() == 4) {
			turn.startGame();
			hub.broadcast(new Message("SYS", "All players joined! Game started (team " + turn.getCurrentTeamId() + " first)."));
			hub.broadcast(buildState());
		} else {
			ch.send(buildState());
		}
	}

	private void onChatAll(Object payload, ClientHandler ch) {
		String text = stringify(payload);
		hub.broadcast(new Message("CHAT_ALL", Map.of("from", ch.getNickname(), "text", text)));
	}

	private void onChatTeam(Object payload, ClientHandler ch) {
		String text = stringify(payload);
		hub.broadcastTeam(ch.getTeamId(), new Message("CHAT_T", Map.of("from", ch.getNickname(), "text", text)));
	}

	private void onChoose(Object payload, ClientHandler ch) {
		if (!turn.isGameStarted()) {
			ch.send(new Message("ERROR", "game not started yet"));
			return;
		}
		if (turn.isGameOver()) {
			ch.send(new Message("ERROR", "game already ended"));
			return;
		}

		if (!ensureTurn(ch)) return;

		var map = asMap(payload);
		Object raw = map.get("front");

		boolean front;

		if (raw instanceof Boolean b) {
			front = b;
		} else if (raw instanceof Number n) {
			front = n.intValue() != 0;
		} else if (raw instanceof String s) {
			front = s.equalsIgnoreCase("true") || s.equals("1");
		} else {
			ch.send(new Message("ERROR", "front must be boolean or 0/1"));
			return;
		}

		Models.Player me = turn.getPlayer(ch.getNickname());
		if (me == null) {
			ch.send(new Message("ERROR", "player not joined"));
			return;
		}

		me.chosenFront = front;

		hub.broadcastTeam(me.teamId, new Message("SYS", me.id + " chose " + (front ? "앞면" : "뒷면")));

		if (!allPlayersChosen()) {
			return;
		}

		int fronts = computeFrontCount();
		clearChoices();

		turn.applyChoiceFronts(fronts);
		publishTurnState();
	}

	private void onMove(Object payload, ClientHandler ch) {
		if (!turn.isGameStarted()) {
			ch.send(new Message("ERROR", "game not started yet"));
			return;
		}

		if (turn.isGameOver()) {
			ch.send(new Message("ERROR", "game already ended"));
			return;
		}

		Models.Player me = turn.getPlayer(ch.getNickname());
		if (me == null || !me.isLeader) {
			ch.send(new Message("ERROR", "only team leader can move"));
			return;
		}

		if (!ensureTurn(ch)) return;

		var p = asMap(payload);
		String pieceId = (String) p.get("pieceId");
		int steps = ((Number) p.get("steps")).intValue();

		if (!((String)pieceId).startsWith(turn.getCurrentTeamId())) {
			ch.send(new Message("ERROR", "cannot move opponent piece"));
			return;
		}

		if (turn.getPhase() != TurnManager.Phase.ALLOCATE) {
			ch.send(new Message("ERROR", "not in ALLOCATE phase"));
			return;
		}

		var res = turn.allocateMove(pieceId, steps);

		hub.broadcast(new Message("MOVED", Map.of("pieceId", pieceId, "steps", steps, "captured", res.captured(), "victimId", res.victimId(), "newPos", res.newPos())));
		publishTurnState();

		if (turn.isGameOver()) {
			hub.broadcast(new Message("GAME_END", Map.of("winner", turn.getWinnerTeamId())));
		}
	}

	private boolean allPlayersChosen() {
		return turn.getPlayers().stream().allMatch(p -> p.chosenFront != null);
	}

	private int computeFrontCount() {
		return (int) turn.getPlayers().stream().filter(p -> Boolean.TRUE.equals(p.chosenFront)).count();
	}

	private void clearChoices() {
		turn.getPlayers().forEach(p -> p.chosenFront = null);
	}

	private void publishTurnState() {
		hub.broadcast(new Message("TOKENS_UPDATED", turn.getTokens()));
		hub.broadcast(new Message("PHASE", turn.getPhase().name()));
		hub.broadcast(new Message("TURN", turn.getCurrentTeamId()));
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> asMap(Object payload) {
		JsonElement tree = gson.toJsonTree(payload);
		return gson.fromJson(tree, Map.class);
	}

	private Map<String, Object> serializeBoard() {
		Map<String, Object> b = new HashMap<>();
		List<Map<String, Object>> teamJson = new ArrayList<>();
		for (var t : turnTeams()) {
			Map<String, Object> tj = new HashMap<>();
			tj.put("id", t.id);
			List<Map<String, Object>> pieces = new ArrayList<>();
			for (var p : t.pieces) {
				Map<String, Object> pj = new HashMap<>();
				pj.put("id", p.id);
				pj.put("pos", p.pos);
				pj.put("stacked", p.stacked);
				pj.put("finished", p.finished());
				pieces.add(pj);
			}
			tj.put("pieces", pieces);
			teamJson.add(tj);
		}
		b.put("teams", teamJson);
		return b;
	}

	private int extractInt(Object payload, String key) {
		var map = asMap(payload);
		Object v = map.get(key);
		if (v == null) throw new IllegalArgumentException("missing key: " + key);
		return ((Number) v).intValue();
	}
	private String stringify(Object payload) {
		if (payload == null) return "";
		if (payload instanceof String s) return s;
		return gson.toJson(payload);
	}

	@SuppressWarnings("unchecked")
	private List<Models.Team> turnTeams() {
		return (List<Models.Team>) (List<?>) turn.getTeams();
	}
}
