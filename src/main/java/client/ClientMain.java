package client;
public class ClientMain {
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        String port = args.length > 1 ? args[1] : "8080";
        System.out.println("[Client] Connecting to " + host + ":" + port);
    }
}
