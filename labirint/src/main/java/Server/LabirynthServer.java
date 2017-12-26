package Server;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by User on 26.12.2017.
 */
public class LabirynthServer {
    public static volatile List<Game> gamesArchive = new ArrayList<Game>(){};

    /**
     * Runs the application. Pairs up clients that connect.
     */
    public static void main(String[] args) throws Exception {
        ServerSocket listener = new ServerSocket(8901);
        System.out.println("Tic Tac Toe Server is Running");
        try {
            while (true) {
                Game game = new Game(5);
                gamesArchive.add(game);
                ArrayList<Game.Player> players=game.getPlayers();
                while (players.size()<2){
                    players.add(game.new Player(players.size(),listener.accept()));
                }
                for(Game.Player p : players){
                    p.start();
                }
            }
        } finally {
            listener.close();
        }
    }
}
