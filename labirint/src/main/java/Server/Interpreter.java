package Server;

import Keywords.Direction;

import java.util.HashSet;
import java.util.Stack;

/**
 * Created by User on 26.12.2017.
 */
public class Interpreter {
    public static void interpret(String src, Game g, Game.Player p) {
        String[] tokens = src.split(" ");
        Stack<String> machine = new Stack<String>();
        boolean is=true;
        for (int i = tokens.length - 1; i > 0; i--) {
            if (tokens[i].matches("[0-9]+") || tokens[i].equals(")")) {
                machine.push(tokens[i]);
            }
            if ( tokens[i].equals("up") || tokens[i].equals("down") || tokens[i].equals("left") || tokens[i].equals("right") || tokens[i].equals("none"))
                if (tokens[i].equals("move")) {
                    String dir = machine.pop();
                    Direction d = Direction.none;
                    if (dir.equals("up")) {
                        d = Direction.up;
                    }
                    if (dir.equals("down")) {
                        d = Direction.down;
                    }
                    if (dir.equals("left")) {
                        d = Direction.left;
                    }
                    if (dir.equals("right")) {
                        d = Direction.right;
                    }
                    p.move(d);
                    g.addToTurn("( move " + dir + " )");
                    String check = machine.pop();
                    if (!check.equals(")") || !tokens[i - 1].equals("(")) {
                        throw new RuntimeException("Неправильно расставлены скобки!");
                    }
                }
            if (tokens[i].equals("look")) {
                String dir = machine.pop();
                Direction d = Direction.none;
                if (dir.equals("up")) {
                    d = Direction.up;
                }
                if (dir.equals("down")) {
                    d = Direction.down;
                }
                if (dir.equals("left")) {
                    d = Direction.left;
                }
                if (dir.equals("right")) {
                    d = Direction.right;
                }
                String s = String.valueOf(g.getPlayers().get(g.currentPlayer).look(d));
                if (d != Direction.none) {
                    p.send("( block " + s + " )");
                    g.setBlock("( block " + s + " )");
                }
                g.addToTurn("( look " + dir + " )");
                String check = machine.pop();
                if (!check.equals(")") && !tokens[i - 1].equals("(")) {
                    throw new RuntimeException("Неправильно расставлены скобки!");
                }
            }
            if (tokens[i].equals("blow")) {
                String dir = machine.pop();
                Direction d = Direction.none;
                if (dir.equals("up")) {
                    d = Direction.up;
                }
                if (dir.equals("down")) {
                    d = Direction.down;
                }
                if (dir.equals("left")) {
                    d = Direction.left;
                }
                if (dir.equals("right")) {
                    d = Direction.right;
                }
                if(p.blow(d)){
                    p.send(" win");
                }
                g.addToTurn("( blow " + dir + " )");
                String check = machine.pop();
                if (!check.equals(")") && !tokens[i - 1].equals("(")) {
                    throw new RuntimeException("Неправильно расставлены скобки!");
                }
            }
            if(tokens[i].equals("greeting")){
                is=false;
                p.send("( greeting "+p.getPlayerId()+" )");
                String check = machine.pop();
                if (!check.equals(")") && !tokens[i - 1].equals("(")) {
                    throw new RuntimeException("Неправильно расставлены скобки!");
                }
            }
            if(tokens[i].equals("options")){
                is=false;
                String s=machine.pop();
                HashSet<String> rules=new HashSet<String>();
                while (!s.equals(")")){
                    rules.add(s);
                }
                machine.push(s);
                g.selectRules(rules);
                g.rulescount++;
                if(g.rulescount==g.getPlayers().size()){
                    g.notifyRules();
                }
                String check = machine.pop();
                if (!check.equals(")") && !tokens[i - 1].equals("(")) {
                    throw new RuntimeException("Неправильно расставлены скобки!");
                }
            }
        }
        g.currentPlayer++;
        if(g.currentPlayer==g.getPlayers().size()){
            g.currentPlayer=0;
        }
        g.sendTurn();
    }
}
