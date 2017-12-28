package Server;

import Keywords.BlockStatus;
import Keywords.Direction;

import java.io.*;
import java.net.Socket;
import java.util.*;

/**
 * Created by User on 22.12.2017.
 */
public class Game {
    private BlockStatus[][] map;
    private Map<Pair, ArrayList<Player>> playersMap;
    int currentPlayer;
    boolean hasWinner;
    private ArrayList<Player> players;
    private Iterator<Player> it;
    private int mapSize;
    private HashSet<String> rules;
    private String turn;
    private String block;
    int rulescount;

    public ArrayList<Player> getPlayers() {
        return players;
    }

    public Game(int mapSize) {
        this.mapSize = mapSize;
        currentPlayer = 0;
        hasWinner = false;
        turn = "";
        rulescount = 0;
        map = generateMap(mapSize);
        players = new ArrayList<Player>(0);
    }

    public void playersRdy() {
        playersMap = scatterPlayers(map);
    }

    private Map<Pair, ArrayList<Player>> scatterPlayers(BlockStatus[][] map) {
        Map<Pair, ArrayList<Player>> r = new TreeMap<Pair, ArrayList<Player>>();
        int c = players.size();
        for (Player p:players){
            for (int i = 0; i < mapSize; i++) {
                for (int j = 0; j < mapSize; j++) {
                    if (map[i][j] == BlockStatus.empty&&!r.containsKey(new Pair(i,j))) {
                        ArrayList<Player> b = new ArrayList<Player>();
                        b.add(p);
                        r.put(new Pair(i, j), b);
                        c--;
                    }
                }
            }
        }
        return r;
    }

    public void regenerateMap(){
        map=generateMap(mapSize);
    }
    private BlockStatus[][] generateMap(int mapSize) {
        BlockStatus[][] r = new BlockStatus[mapSize][mapSize];
        float chanceunb = 1 / 16;
        float chancebr = 1 / 8;
        for (int i = 0; i < mapSize; i++) {
            for (int j = 0; j < mapSize; j++) {
                double a = Math.random();
                if (a > 1 - chanceunb) {
                    chanceunb /= 2;
                    r[i][j] = BlockStatus.unbreakable;
                    continue;
                }
                float b = chanceunb;
                chanceunb = 1 / 16;
                if (a > 1 - b - chancebr) {
                    chancebr /= 2;
                    r[i][j] = BlockStatus.breakable;
                    continue;
                }
                chancebr = 1 / 8;
                r[i][j] = BlockStatus.empty;
            }
        }
        return r;
    }

    public void removeBags() {
        BlockStatus[][] save = new BlockStatus[mapSize][mapSize];
        for (int i = 0; i < mapSize; i++) {
            for (int j = 0; j < mapSize; j++) {
                save[i][j] = null;
            }
        }
        boolean b = false;
        int i = 0, j = 0;
        for (i = 0; i < mapSize; i++) {
            for (j = 0; j < mapSize; j++) {
                if (map[i][j] == BlockStatus.empty) {
                    b = true;
                    save[i][j]=BlockStatus.empty;
                    break;
                }
            }
            if (b) break;
        }
        if (save[i][j]!=null){
            boolean c=true;
            for (;i<mapSize;i++){
                while (j<mapSize&&j>=0&&map[i][j]!=BlockStatus.unbreakable){

                    if(c){
                        j++;
                    }
                    else j--;
                }
            }
        }
        else regenerateMap();
    }

    public void setBlock(String block) {
        this.block = block;
    }

    public void addToTurn(String s) {
        turn += s;
    }

    public void sendTurn() throws IOException {
        for (Player p : players) {
            String t = "( turn ( " + currentPlayer + " " + turn + ") ) ";
            t += block;
            t += hasWinner ? " lose" : "";
            p.send(t);
        }
    }

    public void selectRules(HashSet<String> r) {
        HashSet<String> r1 = new HashSet<String>();
        for (String s : rules) {
            if (r.contains(s)) {
                r1.add(s);
            }
        }
        rules = r1;
    }

    public void notifyRules() throws IOException {
        String s = "( rules ";
        for (String s2 : rules) {
            s += s2 + " ";
        }
        s += ")";
        for (Player p : players) {
            p.send(s);
            p.send("( userscount " + players.size() + " )");
        }
    }

    class Pair implements Comparable {
        int x;
        int y;

        public Pair(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int compareTo(Object o) {
            if (o.getClass() == Pair.class) {
                Pair p = (Pair) o;
                if (p.x > x && p.y > y) return 1;
                if (p.x > x && p.y < y) return 1;
                if (p.x < x && p.y > y) return -1;
                if (p.x < x && p.y < y) return -1;
                return 0;
            }
            return -1;
        }
    }

    class Player extends Thread {
        Game g;
        private int id;
        private boolean alive;
        private int posX;
        private int posY;
        BufferedReader input;
        BufferedWriter output;
        Socket socket;
        BlockStatus[][] playerMap;

        public int getPlayerId() {
            return id;
        }

        public Player(int id, Socket socket, Game game) {
            this.id = id;
            this.socket = socket;
            alive = true;
            g = game;
            posY = mapSize;
            posX = mapSize;
            playerMap = new BlockStatus[mapSize * 2 - 1][mapSize * 2 - 1];
            Arrays.fill(playerMap, null);
            try {
                input = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            } catch (IOException e) {
                System.out.println("Player died: " + e);
            }
            this.start();
        }

        public void send(String s) throws IOException {
            output.write(s + "\n");
            output.flush();
        }

        public void move(Direction d) {
            int x = posX;
            int y = posY;
            switch (d) {
                case up:
                    if (posY < mapSize) {
                        posY++;
                    }
                    break;
                case down:
                    if (posY < mapSize) {
                        posY--;
                    }
                    break;
                case right:
                    if (posY < mapSize) {
                        posX++;
                    }
                    break;
                case left:
                    if (posY < mapSize) {
                        posX--;
                    }
                    break;
                case none:
                    break;
            }
            movePlayer(this, x, y);
        }

        public boolean blow(Direction d) {
            if (rules.contains("ultimateblow")) {
                return ultimateBlow(this, d);
            } else {
                if (Game.this.blow(this, d)) hasWinner = true;
            }
            return Game.this.blow(this, d);
        }

        public BlockStatus look(Direction d) {
            BlockStatus b = null;
            switch (d) {
                case up:
                    if (posY < mapSize)
                        b = map[posX][posY + 1];
                    else b = BlockStatus.unbreakable;
                    playerMap[posX][posY + 1] = b;
                    break;
                case down:
                    if (posY > 0)
                        b = map[posX][posY - 1];
                    else b = BlockStatus.unbreakable;
                    playerMap[posX][posY - 1] = b;
                    break;
                case right:
                    if (posX < mapSize)
                        b = map[posX + 1][posY];
                    else b = BlockStatus.unbreakable;
                    playerMap[posX + 1][posY] = b;
                    break;
                case left:
                    if (posX > 0)
                        b = map[posX - 1][posY];
                    else b = BlockStatus.unbreakable;
                    playerMap[posX - 1][posY] = b;
                    break;
                case none:
                    break;
            }

            return b;
        }

        public void die() {
            alive = false;
        }

        public void run() {
            System.out.println(id);
            try {
                while (true) {
                    String command = input.readLine();
                    System.out.println(command);
                    Interpreter.interpret(command, g, this);
                }
            } catch (IOException e) {
                System.out.println("Player died: " + e);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private void movePlayer(Player player, int x, int y) {
        playersMap.get(new Pair(x, y)).remove(player);
        playersMap.get(new Pair(player.posX, player.posY)).add(player);
    }

    private boolean blow(Player p, Direction d) {
        boolean killed = false;
        switch (d) {
            case up:
                if (p.posY < mapSize) {
                    if (map[p.posX][p.posY + 1] == BlockStatus.breakable) {
                        map[p.posX][p.posY + 1] = BlockStatus.broken;
                    }
                    if (!playersMap.get(new Pair(p.posX, p.posY + 1)).isEmpty()) {
                        killed = true;
                        for (Player player : playersMap.get(new Pair(p.posX, p.posY + 1))) {
                            player.die();
                        }
                    }
                }
                break;
            case down:
                if (p.posY < mapSize) {
                    if (map[p.posX][p.posY - 1] == BlockStatus.breakable) {
                        map[p.posX][p.posY - 1] = BlockStatus.broken;
                    }
                    if (!playersMap.get(new Pair(p.posX, p.posY - 1)).isEmpty()) {
                        killed = true;
                        for (Player player : playersMap.get(new Pair(p.posX, p.posY - 1))) {
                            player.die();
                        }
                    }
                }
                break;
            case right:
                if (p.posY < mapSize) {
                    if (map[p.posX + 1][p.posY] == BlockStatus.breakable) {
                        map[p.posX + 1][p.posY] = BlockStatus.broken;
                    }
                    if (!playersMap.get(new Pair(p.posX + 1, p.posY)).isEmpty()) {
                        killed = true;
                        for (Player player : playersMap.get(new Pair(p.posX + 1, p.posY))) {
                            player.die();
                        }
                    }
                }
                break;
            case left:
                if (p.posY < mapSize) {
                    if (map[p.posX - 1][p.posY] == BlockStatus.breakable) {
                        map[p.posX - 1][p.posY] = BlockStatus.broken;
                    }
                    if (!playersMap.get(new Pair(p.posX - 1, p.posY)).isEmpty()) {
                        killed = true;
                        for (Player player : playersMap.get(new Pair(p.posX - 1, p.posY))) {
                            player.die();
                        }
                    }
                }
                break;
            case none:
                break;
        }
        return killed;
    }

    private boolean ultimateBlow(Player player, Direction d) {
        boolean killed = false;
        int i;
        switch (d) {
            case up:
                i = player.posY;
                while (i < mapSize && playersMap.get(new Pair(player.posX, i)).isEmpty() && map[player.posX][i] != BlockStatus.unbreakable && map[player.posX][i] != BlockStatus.breakable) {
                    i++;
                }
                if (!playersMap.get(new Pair(player.posX, i)).isEmpty()) {
                    killed = true;
                    if (!playersMap.get(new Pair(player.posX, i)).isEmpty()) {
                        killed = true;
                        for (Player p : playersMap.get(new Pair(player.posX, i))) {
                            p.die();
                        }
                    }
                }
                if (map[player.posX][i] == BlockStatus.breakable) {
                    map[player.posX][i] = BlockStatus.broken;
                }
                break;
            case down:
                i = player.posY;
                while (i >= 0 && playersMap.get(new Pair(player.posX, i)).isEmpty() && map[player.posX][i] != BlockStatus.unbreakable && map[player.posX][i] != BlockStatus.breakable) {
                    i--;
                }
                if (!playersMap.get(new Pair(player.posX, i)).isEmpty()) {
                    killed = true;
                    if (!playersMap.get(new Pair(player.posX, i)).isEmpty()) {
                        killed = true;
                        for (Player p : playersMap.get(new Pair(player.posX, i))) {
                            p.die();
                        }
                    }
                }
                if (map[player.posX][i] == BlockStatus.breakable) {
                    map[player.posX][i] = BlockStatus.broken;
                }
                break;
            case right:
                i = player.posX;
                while (i < mapSize && playersMap.get(new Pair(i, player.posY)).isEmpty() && map[i][player.posY] != BlockStatus.unbreakable && map[i][player.posY] != BlockStatus.breakable) {
                    i++;
                }
                if (!playersMap.get(new Pair(i, player.posY)).isEmpty()) {
                    killed = true;
                    if (!playersMap.get(new Pair(i, player.posY)).isEmpty()) {
                        killed = true;
                        for (Player p : playersMap.get(new Pair(i, player.posY))) {
                            p.die();
                        }
                    }
                }
                if (map[i][player.posY] == BlockStatus.breakable) {
                    map[i][player.posY] = BlockStatus.broken;
                }
                break;
            case left:
                i = player.posX;
                while (i >= 0 && playersMap.get(new Pair(i, player.posY)).isEmpty() && map[i][player.posY] != BlockStatus.unbreakable && map[i][player.posY] != BlockStatus.breakable) {
                    i--;
                }
                if (!playersMap.get(new Pair(i, player.posY)).isEmpty()) {
                    killed = true;
                    if (!playersMap.get(new Pair(i, player.posY)).isEmpty()) {
                        killed = true;
                        for (Player p : playersMap.get(new Pair(i, player.posY))) {
                            p.die();
                        }
                    }
                }
                if (map[i][player.posY] == BlockStatus.breakable) {
                    map[i][player.posY] = BlockStatus.broken;
                }
                break;
            case none:
                break;
        }
        return killed;
    }
}
