package server.net;

import com.google.gson.Gson;
import common.dto.Message;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

public class ServerSocketManager {
	private final int port;
	private final ExecutorService pool = Executors.newCachedThreadPool();
	private final Gson gson = new Gson();

	public ServerSocketManager(int port) {
		this.port = port;
	}

	public void start() throws IOException {
		try (ServerSocket server = new ServerSocket(port)) {
			System.out.println("[Server] listening on: " + port);
			while (true) {
				Socket client = server.accept();
				pool.submit(() -> handleClient(client));
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
