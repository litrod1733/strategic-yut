package client.net;

import com.google.gson.Gson;
import common.dto.Message;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class Connection implements Closeable {
	private final Socket socket;
	private final PrintWriter out;
	private final BufferedReader in;
	private final Gson gson = new Gson();

	public Connection(String host, int port) throws IOException {
		this.socket = new Socket(host, port);
		this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
		this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}

	public void send(Message msg) {
		out.println(gson.toJson(msg));
	}

	public void listen(Consumer<Message> onMessage) {
		new Thread(() -> {
			String line;
			try {
				while ((line = in.readLine()) != null) {
					onMessage.accept(gson.fromJson(line, Message.class));
				}
			} catch (IOException ignored) {}
		}, "client-listener").start();
	}

	@Override
	public void close() throws IOException {
		socket.close();
	}
}
