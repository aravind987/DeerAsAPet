import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
class Run
{
    private static int WIDTH, HEIGHT, SLEEP = 1;
    private static JPanel display;
    private static Game session;
    public static void main(String[] args)
    {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        WIDTH = (int)d.getWidth();
        HEIGHT = (int)d.getHeight();
        try{session = new Game(JOptionPane.showInputDialog(null, "What's your name !", "Dialog", JOptionPane.QUESTION_MESSAGE), new BufferedReader(new FileReader(new File("gamedata.txt"))));}
        catch(Exception e)
        {
            System.err.println("Game initialization failed : " + e.getMessage());
            System.exit(1);
        }
        JFrame f = new JFrame("Adventure Story !");
        f.setBounds(0, 0, WIDTH, HEIGHT);
        display = new JPanel()
        {
            public void paint(Graphics g)
            {
                session.next();
                session.draw(g, this.getSize());
            }
        };
        display.setSize(WIDTH, HEIGHT);
        display.setDoubleBuffered(true);
        f.add(display);
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        f.setVisible(true);
        Thread control = new Thread(new Runnable()
        {
            public void run()
            {
                while(true)
                {
                    try{Thread.sleep(SLEEP);}
                    catch(InterruptedException e){}
                    display.repaint();
                }
            }
        });
        control.start();
    }
}
class Game
{
    private String playerName;
    private ArrayList<GameEvent> history;
    private ArrayList<Object> gamedata;
    private State state;
    public Game(String playerName, BufferedReader dataStream) throws Exception
    {
        this.playerName = playerName;
        this.history = new ArrayList<GameEvent>();
        this.gamedata = new ArrayList<Object>();
        this.state = new State();
        String line = dataStream.readLine();
        boolean next_option = false;
        while(line != null)
        {
            if(Character.isDigit(line.charAt(0)))
            {
                if(!next_option)
                {
                    gamedata.add(new Choice());
                    next_option = true;
                }
                ((Choice)gamedata.get(gamedata.size()-1)).addOption(line.substring(1).trim());
            }
            else
            {
                gamedata.add(new GameEvent(line.trim()));
                next_option = false;
            }
            line = dataStream.readLine();
        }
        dataStream.close();
    }
    public void next()
    {
        /*for(int i = 0; i < this.gameplay.size(); i++)
        {
            System.out.println(this.gameplay.get(i));
        }
        System.out.println("EOG");*/

        // take turn / do nothing
    }
    public void draw(Graphics g, Dimension d)
    {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, (int)d.getWidth(), (int)d.getHeight());
        // draw media
    }
}
class State
{
    private static int MAX_BAR_VALUE = 100;
    private int water_bar_value, food_bar_value, energy_bar_value;
    public State()
    {
        this.water_bar_value = 0;
        this.food_bar_value = 0;
        this.energy_bar_value = 0;
    }
}
class GameEvent
{
    public String eventDescripton;
    public GameEvent(String eventDescripton)
    {
        this.eventDescripton = eventDescripton;
    }
}
class Choice
{
    private ArrayList<GameEvent> options;
    public Choice(){this.options = new ArrayList<GameEvent>();}
    public Choice(String[] options)
    {
        this.options = new ArrayList<GameEvent>();
        for(int i = 0; i < options.length; i++){this.options.add(new GameEvent(options[i]));}
    }
    public void addOption(String option){this.options.add(new GameEvent(option));}
    public GameEvent select(int option){return this.options.get(option);}
    public String[] getOptions()
    {
        String[] options = new String[this.options.size()];
        for(int i = 0; i < this.options.size(); i++){options[i] = this.options.get(i).eventDescripton;}
        return options;
    }
}