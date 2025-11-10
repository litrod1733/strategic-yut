package server.net;

import com.google.gson.Gson;
import common.dto.Message;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
	private final Socket socket;
	private final Router router;
	private final ConnectionHub hub;
	private final Gson gson = new Gson();

	private PrintWriter out;
	private BufferedReader in;

	private String teamId;
	private String nickname;

	public ClientHandler(Socket socket, Router router, ConnectionHub hub) {
		this.socket = socket;
		this.router = router;
		this.hub = hub;
	}

	@Override
	public void run() {
		try (socket) {
			this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
			this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			hub.register(this);

			System.out.println("[Server] connected: " + socket.getRemoteSocketAddress());

			String line;
			while ((line = in.readLine()) != null) {
				Message msg = gson.fromJson(line, Message.class);
				router.handle(msg, this);
			}
		} catch (IOException e) {
			System.err.println("[Server] client error: " + e.getMessage());
		} finally {
			hub.remove(this);
			System.out.println("[Server] disconnected: " + socket.getRemoteSocketAddress());
		}
	}

	public void send(Message msg) {
		if (out != null) out.println(gson.toJson(msg));
	}

	public String getTeamId() {
		return teamId;
	}
	public String getNickname() {
		return nickname;
	}
	public void setTeamId(String t) {
		this.teamId = t;
	}
	public void setNickname(String n) {
		this.nickname = n;
	}
}
