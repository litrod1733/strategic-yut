package client;

import client.net.Connection;
import common.dto.Message;

import java.util.*;

public class ClientMain {
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 7777;

        try (Connection conn = new Connection(host, port)) {
            System.out.println("[Client] connected to " + host + ": " + port);
            conn.listen(msg -> {
                switch (msg.type) {
                    case "STATE" -> renderState(msg.payload);
                    case "TOKENS_UPDATED" -> System.out.println("[HUD] tokens=" + msg.payload);
                    case "PHASE" -> System.out.println("[HUD] phase=" + msg.payload);
                    case "TURN" -> System.out.println("[HUD] turn" + msg.payload);
                    case "MOVED" -> {
                        Map<?, ?> m = (Map<?, ?>) msg.payload;
                        System.out.printf("[MOVE] %s steps=%s -> pos%s %s%n", m.get("pieceId"), m.get("steps"), m.get("newPos"),
                          Boolean.TRUE.equals(m.get("captured")) ? "(captured " + m.get("victimId") + ")" : "");
                    }
                    default -> System.out.println("[Client] recv: " + msg.type + " / " + msg.payload);
                }
            });

            new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(15000);
                        conn.send(new Message("PING", "keepalive"));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, "heartbeat-thread").start();

            printHelp();

            Scanner sc = new Scanner(System.in);
            while (true) {
                System.out.print("> ");
                if (!sc.hasNextLine()) break;
                String line = sc.nextLine().trim();
                if (line.isEmpty()) continue;
                if (line.equalsIgnoreCase("exit")) break;

                if (line.startsWith("/")) {
                    handleCommand(line, conn);
                    continue;
                }

                conn.send(new Message("CHAT_ALL", line));
            }
        }
        System.out.println("[Client] bye");
    }

    private static void handleCommand(String line, Connection conn) {
        try {
            if (line.startsWith("/help")) {
                printHelp();
                return;
            }

            if (line.startsWith("/join")) {
                String[] sp = line.split("\\s+", 3);
                if (sp.length < 3) {
                    System.out.println("usage: /join <teamId> <nickname>");
                    return;
                }
                conn.send(new Message("JOIN", Map.of("teamId", sp[1], "nickname", sp[2])));
                return;
            }

            if (line.startsWith("/say")) {
                String text = line.substring(5).trim();
                if (text.isEmpty()) {
                    System.out.println("usage: /say <text...>");
                    return;
                }
                conn.send(new Message("CHAT_ALL", text));
                return;
            }

            if (line.startsWith("/team")) {
                String text = line.substring(6).trim();
                if (text.isEmpty()) {
                    System.out.println("usage: /team <text...>");
                    return;
                }
                conn.send(new Message("CHAT_T", text));
                return;
            }

            if (line.startsWith("/choose")) {
                String[] sp = line.split("\\s+");
                if (sp.length != 2) {
                    System.out.println("usage: /choose <fronts:0~4>");
                    return;
                }
                int fronts = Integer.parseInt(sp[1]);
                if (fronts < 0 || fronts >4) {
                    System.out.println("fronts must be 0..4");
                    return;
                }
                conn.send(new Message("CHOOSE", Map.of("fronts", fronts)));
                return;
            }

            if (line.startsWith("/move")) {
                String[] sp = line.split("\\s+");
                if(sp.length != 3) {
                    System.out.println("usage: /move <pieceId> <steps>");
                    return;
                }
                String pieceId = sp[1];
                int steps = Integer.parseInt(sp[2]);
                conn.send(new Message("MOVE", Map.of("pieceId", pieceId, "steps", steps)));
                return;
            }

            if (line.startsWith("/state")) {
                conn.send(new Message("REQ_STATE", ""));
                return;
            }

            System.out.println("unknown command. type /help");
        } catch (NumberFormatException nfe) {
            System.out.println("number format error: " + nfe.getMessage());
        } catch (Exception e) {
            System.out.println("command error: " + e.getMessage());
        }
    }

    private static void printHelp() {
        System.out.println("""
          commands:
          /join <teamId> <nickname>     - set my & nickname (e.g., /join A jason)
          /say <text...>                - broadcast chat to all
          /team <text...>               - chat to my team only
          /choose <fronts:0..4>         - choose yut fronts (0=모, 4=윷)
          /move <pieceId> <steps>       - move (e.g., /move A1 2)
          /help                         - show this help
          exit                          - quit client
          """);
    }

    @SuppressWarnings("unchecked")
    private static void renderState(Object payload) {
        Map<String, Object> st = (Map<String, Object>) payload;
        String turn = (String) st.get("turn");
        String phase = (String) st.get("phase");
        List<Integer> tokens = (List<Integer>) st.get("tokens");
        Map<String, Object> board = (Map<String, Object>) st.get("board");
        List<Map<String, Object>> teams = (List<Map<String, Object>>) board.get("teams");

        System.out.println("== STATE ==");
        System.out.printf("turn=%s, phase=%s, tokens=%s%n", turn, phase, tokens);

        int TRACK = 20;
        char[] line = new char[TRACK + 1];
        Arrays.fill(line, '.');

        for (Map<String, Object> t : teams) {
            String tid = (String) t.get("id");
            List<Map<String, Object>> ps = (List<Map<String, Object>>) t.get("pieces");
            for (Map<String, Object> p : ps) {
                int pos = ((Number) p.get("pos")).intValue();
                if (0 <= pos && pos <= TRACK) {
                    char mark = "A".equals(tid) ? 'A' : 'B';
                    line[pos] = mark;
                }
            }
        }
        System.out.println(new String(line));
    }
}
