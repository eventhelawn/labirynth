package Server;

import Keywords.BlockStatus;
import Keywords.Direction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
        hasWinner=false;
        turn="";
        rulescount=0;
        map = generateMap(mapSize);
        players=new ArrayList<Player>();
    }

    public void playersRdy(){
        playersMap=scatterPlayers(map);
    }
    private Map<Pair, ArrayList<Player>> scatterPlayers(BlockStatus[][] map) {
        Map<Pair, ArrayList<Player>> r = new TreeMap<Pair, ArrayList<Player>>();
        int c = players.size();
        float chance = 1 / 4;
        while (c > 0) {
            for (int i = 0; i < mapSize; i++) {
                for (int j = 0; j < mapSize; j++) {
                    if (map[i][j] == BlockStatus.empty){
                        double a=Math.random();
                        if(a>1-chance){
                            if(r.containsKey(new Pair(i,j)))r.get(new Pair(i,j)).add(players.get(--c));
                            else {
                                ArrayList<Player> b=new ArrayList<Player>();
                                b.add(players.get(--c));
                                r.put(new Pair(i,j),b);
                            }
                        }
                    }
                }
            }
        }
        return r;
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
                    r[i][j] = BlockStatus.unbrekable;
                    continue;
                }
                float b = chanceunb;
                chanceunb = 1 / 16;
                if (a > 1 - b - chancebr) {
                    chancebr /= 2;
                    r[i][j] = BlockStatus.brekable;
                    continue;
                }
                chancebr = 1 / 8;
                r[i][j] = BlockStatus.empty;
            }
        }
        return r;
    }

    public void setBlock(String block) {
        this.block = block;
    }

    public void addToTurn(String s) {
        turn+=s;
    }
    public void sendTurn(){
        for(Player p:players){
            String t="( turn ( "+currentPlayer+" "+turn+") ) ";
            t+=block;
            t+=hasWinner ? " lose":"";
            p.send(t);
        }
    }

    public void selectRules(HashSet<String> r) {
        HashSet<String> r1=new HashSet<String>();
        for(String s:rules){
            if(r.contains(s)){
                r1.add(s);
            }
        }
        rules=r1;
    }
    public void notifyRules(){
        String s="( rules ";
        for(String s2:rules){
            s+=s2+" ";
        }
        s+=")";
        for(Player p:players){
            p.send(s);
            p.send("( userscount "+players.size()+" )");
        }
    }
    class Pair {
        int x;
        int y;

        public Pair(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    class Player extends Thread{
        Game g;
        private int id;
        private boolean alive;
        private int posX;
        private int posY;
        BufferedReader input;
        PrintWriter output;
        Socket socket;
        BlockStatus[][] playerMap;

        public int getPlayerId() {
            return id;
        }

        public Player(int id, Socket socket,Game game) {
            this.id = id;
            this.socket = socket;
            alive = true;
            g=game;
            posY = mapSize;
            posX = mapSize;
            playerMap = new BlockStatus[mapSize * 2 - 1][mapSize * 2 - 1];
            Arrays.fill(playerMap, null);
            try {
                input = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                System.out.println("Player died: " + e);
            }
        }

        public void send(String s){
            output.println(s);
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
                if(Game.this.blow(this, d))hasWinner=true;
            }return Game.this.blow(this, d);
        }

        public BlockStatus look(Direction d) {
            BlockStatus b = null;
            switch (d) {
                case up:
                    if (posY < mapSize)
                        b = map[posX][posY + 1];
                    else b = BlockStatus.unbrekable;
                    playerMap[posX][posY + 1] = b;
                    break;
                case down:
                    if (posY > 0)
                        b = map[posX][posY - 1];
                    else b = BlockStatus.unbrekable;
                    playerMap[posX][posY - 1] = b;
                    break;
                case right:
                    if (posX < mapSize)
                        b = map[posX + 1][posY];
                    else b = BlockStatus.unbrekable;
                    playerMap[posX + 1][posY] = b;
                    break;
                case left:
                    if (posX > 0)
                        b = map[posX - 1][posY];
                    else b = BlockStatus.unbrekable;
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
            try {
                while (true) {
                    String command = input.readLine();
                    Interpreter.interpret(command,g,this);
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
                    if (map[p.posX][p.posY + 1] == BlockStatus.brekable) {
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
                    if (map[p.posX][p.posY - 1] == BlockStatus.brekable) {
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
                    if (map[p.posX + 1][p.posY] == BlockStatus.brekable) {
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
                    if (map[p.posX - 1][p.posY] == BlockStatus.brekable) {
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
                while (i < mapSize && playersMap.get(new Pair(player.posX, i)).isEmpty() && map[player.posX][i] != BlockStatus.unbrekable && map[player.posX][i] != BlockStatus.brekable) {
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
                if (map[player.posX][i] == BlockStatus.brekable) {
                    map[player.posX][i] = BlockStatus.broken;
                }
                break;
            case down:
                i = player.posY;
                while (i >= 0 && playersMap.get(new Pair(player.posX, i)).isEmpty() && map[player.posX][i] != BlockStatus.unbrekable && map[player.posX][i] != BlockStatus.brekable) {
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
                if (map[player.posX][i] == BlockStatus.brekable) {
                    map[player.posX][i] = BlockStatus.broken;
                }
                break;
            case right:
                i = player.posX;
                while (i < mapSize && playersMap.get(new Pair(i, player.posY)).isEmpty() && map[i][player.posY] != BlockStatus.unbrekable && map[i][player.posY] != BlockStatus.brekable) {
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
                if (map[i][player.posY] == BlockStatus.brekable) {
                    map[i][player.posY] = BlockStatus.broken;
                }
                break;
            case left:
                i = player.posX;
                while (i >= 0 && playersMap.get(new Pair(i, player.posY)).isEmpty() && map[i][player.posY] != BlockStatus.unbrekable && map[i][player.posY] != BlockStatus.brekable) {
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
                if (map[i][player.posY] == BlockStatus.brekable) {
                    map[i][player.posY] = BlockStatus.broken;
                }
                break;
            case none:
                break;
        }
        return killed;
    }
}
