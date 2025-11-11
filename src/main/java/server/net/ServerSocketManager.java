package server.net;

import com.google.gson.Gson;
import common.dto.Message;
import server.game.*;
import server.util.JavaRandomProvider;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.*;

public class ServerSocketManager {
	private final int port;
	private final ExecutorService pool = Executors.newCachedThreadPool();
	private final Gson gson = new Gson();

	public ServerSocketManager(int port) {
		this.port = port;
	}

	public void start() throws IOException {
		var A = new Models.Team("A");
		A.pieces.add(new Models.Piece("A1", "A"));
		A.pieces.add(new Models.Piece("A2", "A"));
		var B = new Models.Team("B");
		B.pieces.add(new Models.Piece("B1", "B"));
		B.pieces.add(new Models.Piece("B2", "B"));
		var board = new Models.Board();
		var rules = new Rules(board, new JavaRandomProvider());
		var turn = new TurnManager(List.of(A, B), rules, List.of("A", "B"));
		turn.startTurn();

		ConnectionHub hub = new ConnectionHub();
		Router router = new Router(turn, hub);

		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleAtFixedRate(
			() -> hub.pruneStale(30_000L),
			5,
			5,
			TimeUnit.SECONDS
		);

		try (ServerSocket server = new ServerSocket(port)) {
			System.out.println("[Server] listening on: " + port);
			while (true) {
				Socket client = server.accept();
				System.out.println("[Server] connected: " + client.getRemoteSocketAddress());
				pool.submit(new ClientHandler(client, router, hub));
			}
		}
	}

	private void handleClient(Socket socket) {
		String remote = socket.getRemoteSocketAddress().toString();
		System.out.println("[Server] connected: " + remote);
		try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {
			String line;
			while ((line = in.readLine()) != null) {
				Message msg = gson.fromJson(line, Message.class);
				System.out.println("[Server] recv: " + msg.type);

				out.println(gson.toJson(msg));
			}
		} catch (Exception e) {
			System.err.println("[Server] error: " + e.getMessage());
		} finally {
			try {
				socket.close();
			} catch (IOException ignore) {}
			System.out.println("[Server] disconnected: " + remote);
		}
	}
}
