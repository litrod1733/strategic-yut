package client;

import client.net.Connection;
import common.dto.Message;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientMain {
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 7777;


        try (Connection conn = new Connection(host, port)) {
            System.out.println("[Client] connected to " + host + ": " + port);
            conn.listen(msg -> {
                switch (msg.type) {
                    case "PONG" -> { return; }
                    case "STATE" -> {
                        renderState(msg.payload);
                        renderFromListener("[STATE updated]");
                    }
                    case "TOKENS_UPDATED" -> renderFromListener("[HUD] tokens=" + msg.payload);
                    case "PHASE" -> renderFromListener("[HUD] phase=" + msg.payload);
                    case "TURN" -> renderFromListener("[HUD] turn=" + msg.payload);
                    case "MOVED" -> {
                        Map<?, ?> m = (Map<?, ?>) msg.payload;
                        String s = String.format("[MOVE] %s steps=%s -> pos%s %s%n", m.get("pieceId"), m.get("steps"), m.get("newPos"),
                          Boolean.TRUE.equals(m.get("captured")) ? "(captured " + m.get("victimId") + ")" : "");
                        renderFromListener(s);
                    }
                    case "YUT_RESULT" -> {
                        Map<?, ?> m = (Map<?, ?>) msg.payload;
                        String name = String.valueOf(m.get("name"));
                        boolean extra = Boolean.TRUE.equals(m.get("extra"));

                        if (extra) {
                            renderFromListener("['" + name + "' 입니다. 한번 더 윷의 모양을 결정해 주세요.]");
                        } else {
                            renderFromListener("['" + name + "' 입니다.]");
                        }
                    }
                    case "GAME_END" -> {
                        Map<?, ?> m = (Map<?, ?>) msg.payload;
                        Object winner = m.get("winner");

                        renderFromListener("""
                          ===== GAME END =====
                          Winner team: %s
                          ====================
                          """.formatted(winner));
                    }
                    default -> renderFromListener("[Client] recv: " + msg.type + " / " + msg.payload);
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
                String line = sc.nextLine();

                String sanitized = line.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
                if (sanitized.isEmpty()) continue;

                line = sanitized.trim();
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
                    System.out.println("usage: /choose <0 or 1>    // 0=back, 1=front");
                    return;
                }
                int fronts = Integer.parseInt(sp[1]);
                if (fronts != 0 && fronts != 1) {
                    System.out.println("fronts must be 0 or 1 (0=back, 1=front)");
                    return;
                }
                boolean front = (fronts == 1);
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
          /choose <0|1>         - choose my yut face (0=back, 1=front)
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

        System.out.println("===== STATE =====");
        System.out.printf("turn=%s, phase=%s, tokens=%s%n", turn, phase, tokens);

        final int SIZE = 5;
        char[][] g = new char[SIZE][SIZE];

        for (int r = 0; r < SIZE; r++) {
            Arrays.fill(g[r], ' ');
        }

        int[][] PATH = {
          {0,0}, {0,1}, {0,2}, {0,3}, {0,4},
          {1,0}, {1,1}, {1,3}, {1,4},
          {2,0}, {2,2}, {2,4},
          {3,0}, {3,1}, {3,3}, {3,4},
          {4,0}, {4,1}, {4,2}, {4,3}, {4,4}
        };

        for (int[] cell : PATH) {
            int r = cell[0], c = cell[1];
            g[r][c] = 'o';
        }

        for (Map<String, Object> t : teams) {
            String tid = (String) t.get("id");
            char mark = "A".equals(tid) ? 'A' : 'B';
            List<Map<String, Object>> ps = (List<Map<String, Object>>)  t.get("pieces");

            for (Map<String, Object> p : ps) {
                int pos = ((Number) p.get("pos")).intValue();
                if (pos < 0) continue;
                if (pos >= PATH.length) pos = PATH.length - 1;

                int r = PATH[pos][0];
                int c = PATH[pos][1];
                g[r][c] = mark;
            }
        }

        System.out.print("   ");
        for (int col = 0; col < SIZE; col++) {
            System.out.print(" " + col + " ");
        }
        System.out.println();

        for (int r = 0; r < SIZE; r++) {
            System.out.print((r + 1) + "  ");
            for (int c = 0; c < SIZE; c++) {
                System.out.print(g[r][c] + "  ");
            }
            System.out.println();
        }

        System.out.println("===============");
    }

    private static final Object OUT_LOCK = new Object();

    private static void renderFromListener(String line) {
        synchronized (OUT_LOCK) {
            System.out.print("\r");
            System.out.println(line);
            System.out.print("> ");
            System.out.flush();
        }
    }
}
