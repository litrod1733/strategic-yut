package server;

import server.net.ServerSocketManager;


public class ServerMain {
    public static void main(String[] args) throws Exception {
        int port = (args.length > 0) ? Integer.parseInt(args[0]) : 7777;
        System.out.println("[Server] Strategic Yut Online starting on: " + port);

        new ServerSocketManager(port).start();
    }
}
