package client;

import client.net.Connection;
import common.dto.Message;

import java.util.Objects;
import java.util.Scanner;

public class ClientMain {
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 7777;

        try (Connection conn = new Connection(host, port)) {
            System.out.println("[Client] connected to " + host + ": " + port);
            conn.listen(msg -> System.out.println("[Client] recv: " + msg.type + " / " + msg.payload));

            Scanner sc = new Scanner(System.in);
            while (true) {
                System.out.print("> ");
                String line = sc.nextLine();
                if (line.equalsIgnoreCase("exit")) break;
                conn.send(new Message("CHAT_ALL", line));
            }
        }
        System.out.println("[Client] bye");
    }
}
