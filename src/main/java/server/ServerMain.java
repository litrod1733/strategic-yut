package server;

import server.game.TurnManager;

import java.util.List;

public class ServerMain {
    public static void main(String[] args) {
        String port = args.length > 0 ? args[0] : "8080";
        System.out.println("[Server] Strategic Yut Online starting on :" + port);

    }
}
