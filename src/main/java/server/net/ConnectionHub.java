package server.net;

import common.dto.Message;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ConnectionHub {
	private final Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());

	public void register(ClientHandler ch) {
		clients.add(ch);
	}
	public void remove(ClientHandler ch) {
		clients.remove(ch);
	}

	public void broadcast(Message msg) {
		synchronized (clients) {
			for (ClientHandler ch : clients) ch.send(msg);
		}
	}

	public void broadcastTeam(String teamId, Message msg) {
		synchronized (clients) {
			for (ClientHandler ch : clients) {
				if (teamId != null && teamId.equals(ch.getTeamId())) ch.send(msg);
			}
		}
	}
}
