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
            e.printStackTrace();
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
    public static final char END_SYMBOL = '|', STATE_CHANGE_SYMBOL = '{', STATE_CHANGE_SEPERATOR = ':';
    public static final String PRINT_DELIMITER = "  ", STATE_CHANGE_DELIMITER = ",";
    private String playerName;
    private GameEvent history;
    private GameEvent gamedata;
    private State state;
    public Game(String playerName, BufferedReader dataStream) throws Exception
    {
        this.playerName = playerName;
        this.history = new GameEvent();
        this.gamedata = new GameEvent(dataStream.readLine().trim());
        this.state = new State();
        getGameData(dataStream, this.gamedata);
        dataStream.close();
        printGameData(this.gamedata, ""); // comment on completion
    }
    private static void getGameData(BufferedReader br, GameEvent root) throws Exception
    {
        String line = null;
        boolean next_option = false;
        while((line = br.readLine()) != null)
        {
            if(Character.isDigit(line.charAt(0)))
            {
                if(!next_option)
                {
                    root.next = new Choice();
                    root = root.next;
                    next_option = true;
                }
                if(line.charAt(line.length() - 1) == END_SYMBOL){((Choice)root).addOption(line.substring(1, line.length() - 1).trim());}
                else
                {
                    ((Choice)root).addOption(line.substring(1).trim());
                    getGameData(br, ((Choice)root).getLastOption());
                }
            }
            else if(line.charAt(line.length() - 1) == END_SYMBOL)
            {
                line = line.substring(0, line.length() - 1).trim();
                if(!line.equals("")){root.next = new GameEvent(line);}
                return;
            }
            else
            {
                root.next = new GameEvent(line.trim());
                root = root.next;
                next_option = false;
            }
        }
    }
    public void next()
    {
        // take turn / do nothing
    }
    private static void printGameData(GameEvent event, String delim)
    {
        while(event != null)
        {
            if(event instanceof Choice)
            {
                String[] opts = ((Choice)event).getOptionDescriptions();
                for(int i = 0; i < opts.length; i++)
                {
                    System.out.println(delim + (i+1) + ") " + opts[i] + " -> " + State.getChangeDescription(((Choice)event).getStateChange(i-1)));
                    printGameData(((Choice)event).getOption(i-1).next, delim + PRINT_DELIMITER);
                }
                return;
            }
            else
            {
                System.out.println(delim + event.eventDescripton);
                event = event.next;
            }
        }
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
    private static final int MAX_BAR_VALUE = 100, STATE_SIZE = 3;
    private int water_bar_value, food_bar_value, energy_bar_value;
    public State()
    {
        this.water_bar_value = 0;
        this.food_bar_value = 0;
        this.energy_bar_value = 0;
    }
    public void applyChanges(int[] values)
    {
        this.water_bar_value += values[0];
        this.food_bar_value += values[1];
        this.energy_bar_value += values[2];
        this.water_bar_value = Math.min(MAX_BAR_VALUE, water_bar_value);
        this.food_bar_value = Math.min(MAX_BAR_VALUE, food_bar_value);
        this.energy_bar_value = Math.min(MAX_BAR_VALUE, energy_bar_value);
    }
    public static int[] parse(String s)
    {
        int[] values = new int[STATE_SIZE];
        String[] changes = s.split(Game.STATE_CHANGE_DELIMITER);
        for(String change : changes)
        {
            int value_pos = change.indexOf(Game.STATE_CHANGE_SEPERATOR);
            if(value_pos == -1) continue;
            values[getAssociatedIndex(change.substring(0, value_pos))] = Integer.parseInt(change.substring(value_pos + 1).trim());
        }
        return values;
    }
    private static int getAssociatedIndex(String field)
    {
        switch(field.toLowerCase().trim())
        {
            case "water": return 0;
            case "food": return 1;
            case "energy": return 2;
            default: return -1;
        }
    }
    public static String getChangeDescription(int[] values)
    {
        String desc = "";
        desc += "water: " + (values[0] < 0 ? "" : "+") + values[0] + ", ";
        desc += "food: " + (values[1] < 0 ? "" : "+") + values[1] + ", ";
        desc += "energy: " + (values[2] < 0 ? "" : "+") + values[2];
        return desc;
    }
}
class GameEvent
{
    public String eventDescripton;
    public GameEvent next;
    public GameEvent()
    {
        this.eventDescripton = "";
        this.next = null;
    }
    public GameEvent(String eventDescripton)
    {
        this.eventDescripton = eventDescripton;
        this.next = null;
    }
}
class Choice extends GameEvent
{
    private ArrayList<GameEvent> options;
    private ArrayList<int[]> stateChanges;
    public Choice()
    {
        super();
        this.options = new ArrayList<GameEvent>();
        this.stateChanges = new ArrayList<int[]>();
    }
    public Choice(String[] options)
    {
        super();
        this.options = new ArrayList<GameEvent>();
        this.stateChanges = new ArrayList<int[]>();
        for(int i = 0; i < options.length; i++){this.addOption(options[i]);}
    }
    public void addOption(String option)
    {
        int stateChangePosition = option.indexOf(Game.STATE_CHANGE_SYMBOL);
        this.stateChanges.add(State.parse(option.substring(stateChangePosition + 1, option.length() - 1)));
        if(this.next == null){this.next = new GameEvent(option.substring(0, stateChangePosition).trim());}
        else{this.options.add(new GameEvent(option.substring(0, stateChangePosition).trim()));}
    }
    public GameEvent getOption(int option)
    {
        if(option == -1) return this.next;
        return this.options.get(option);
    }
    public int[] getStateChange(int option){return this.stateChanges.get(option + 1);}
    public GameEvent select(int option, State s)
    {
        s.applyChanges(this.getStateChange(option));
        return this.getOption(option);
    }
    public GameEvent getLastOption(){return this.getOption(this.options.size() - 1);}
    public String[] getOptionDescriptions()
    {
        String[] options = new String[this.options.size() + 1];
        for(int i = 0; i <= this.options.size(); i++){options[i] = this.getOption(i-1).eventDescripton;}
        return options;
    }
}