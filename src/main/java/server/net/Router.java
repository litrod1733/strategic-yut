package server.net;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import common.dto.Message;
import server.game.TurnManager;

import java.util.List;
import java.util.Map;

public class Router {
	private final Gson gson = new Gson();
	private final TurnManager turn;
	private final ConnectionHub hub;

	public Router(TurnManager turn, ConnectionHub hub) {
		this.turn = turn;
		this.hub = hub;
	}

	public void handle(Message msg, ClientHandler ch) {
		switch (msg.type) {
			case "JOIN" -> onJoin(msg.payload, ch);
			case "CHAT_ALL" -> onChatAll(msg.payload, ch);
			case "CHAT_T" -> onChatTeam(msg.payload, ch);

			case "CHOOSE" -> onChoose(msg.payload, ch);
			case "MOVE" -> onMove(msg.payload, ch);

			default -> ch.send(new Message("ERROR", Map.of("message", "unknown type: " + msg.type)));
		}
	}

	private void onJoin(Object payload, ClientHandler ch) {
		var p = asMap(payload);
		ch.setNickname((String) p.getOrDefault("nickname", "anon"));
		ch.setTeamId((String) p.getOrDefault("teamId", "A"));
		ch.send((new Message("JOIN_OK", Map.of("teamId", ch.getTeamId(), "nickname", ch.getNickname()))));
		hub.broadcast(new Message("SYS", ch.getNickname() + " joined (" + ch.getTeamId() + ")"));
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
		int fronts = extractInt(payload, "fronts");

		if(turn.getPhase() == TurnManager.Phase.CHOOSE) {
			turn.applyChoiceFronts(fronts);

			hub.broadcast(new Message("TOKENS_UPDATED", turn.getTokens()));
			if (turn.getPhase() == TurnManager.Phase.ALLOCATE) {
				hub.broadcast(new Message("PHASE", "ALLOCATE"));
			} else {
				hub.broadcast(new Message("PHASE", "CHOOSE"));
			}
		} else {
			ch.send(new Message("ERROR", "not in CHOOSE phase"));
		}
	}

	private void onMove(Object payload, ClientHandler ch) {
		var p = asMap(payload);
		String pieceId = (String) p.get("pieceId");
		int steps = ((Number) p.get("steps")).intValue();

		if (turn.getPhase() != TurnManager.Phase.ALLOCATE) {
			ch.send(new Message("ERROR", "not in ALLOCATE phase"));
			return;
		}

		var res = turn.allocateMove(pieceId, steps);

		hub.broadcast(new Message("MOVED", Map.of("pieceId", pieceId, "steps", steps, "captured", res.captured(), "victimId", res.victimId(), "newPos", res.newPos())));
		hub.broadcast(new Message("TOKENS_UPDATED", turn.getTokens()));
		hub.broadcast(new Message("PHASE", turn.getPhase().name()));
		hub.broadcast(new Message("TURN", turn.getCurrentTeamId()));
	}

	private Map<?,?> asMap(Object payload) {
		JsonElement tree = gson.toJsonTree(payload);
		return gson.fromJson(tree, Map.class);
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
}
