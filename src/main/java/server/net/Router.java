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
		ch.setNickname((String) p.getOrDefault("nickname", "anon"));
		ch.setTeamId((String) p.getOrDefault("teamId", "A"));
		ch.send(new Message("JOIN_OK", Map.of("teamId", ch.getTeamId(), "nickname", ch.getNickname())));
		hub.broadcast(new Message("SYS", ch.getNickname() + " joined (" + ch.getTeamId() + ")"));
		ch.send(buildState());
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
		if (!ensureTurn(ch)) return;

		int fronts = extractInt(payload, "fronts");

		if(turn.getPhase() != TurnManager.Phase.CHOOSE) {
			turn.applyChoiceFronts(fronts);
			ch.send(new Message("ERROR", "not in CHOOSE phase"));
			return;
		}

		turn.applyChoiceFronts(fronts);

		hub.broadcast(new Message("CHOOSE_RESULT", Map.of("fronts", fronts)));

		publishTurnState();
	}

	private void onMove(Object payload, ClientHandler ch) {
		if (!ensureTurn(ch)) return;

		var p = asMap(payload);
		String pieceId = (String) p.get("pieceId");
		int steps = ((Number) p.get("steps")).intValue();

		if (turn.getPhase() != TurnManager.Phase.ALLOCATE) {
			ch.send(new Message("ERROR", "not in ALLOCATE phase"));
			return;
		}

		var res = turn.allocateMove(pieceId, steps);

		hub.broadcast(new Message("MOVED", Map.of("pieceId", pieceId, "steps", steps, "captured", res.captured(), "victimId", res.victimId(), "newPos", res.newPos())));
		publishTurnState();
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
