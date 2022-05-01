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
        String name = JOptionPane.showInputDialog(null, "What's your name !", "Dialog", JOptionPane.QUESTION_MESSAGE);
        if(name == null) System.exit(0);
        JFrame f = new JFrame("Adventure Story !");
        try{session = new Game(name, f, new BufferedReader(new FileReader(new File("gamedata.txt"))));}
        catch(Exception e)
        {
            System.err.println("Game initialization failed : " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        f.setBounds(Math.max((WIDTH - (int)(1.04 * Game.media[session.state.gif].getWidth(null)))/2, 0),
                    Math.max((HEIGHT - (int)(1.08 * Game.media[session.state.gif].getHeight(null)))/2, 0),
                    Math.min((int)(1.08 * Game.media[session.state.gif].getWidth(null)), WIDTH),
                    Math.min((int)(1.16 * Game.media[session.state.gif].getHeight(null)), HEIGHT));
        display = new JPanel()
        {
            public void paint(Graphics g)
            {
                session.next();
                session.draw(g, this.getSize());
            }
        };
        display.setSize(f.getSize());
        f.addKeyListener(new KeyAdapter()
        {
            public void keyPressed(KeyEvent e)
            {
                if(session.request.isAlive())
                {
                    if(e.getKeyCode() == 10)
                    {
                        session.selected = true;
                    }
                    else if(e.getKeyCode() == 38)
                    {
                        session.considering -= 1;
                        session.considering = (int)Math.max(0, session.considering);
                    }
                    else if(e.getKeyCode() == 40)
                    {
                        session.considering += 1;
                        session.considering = (int)Math.min(session.considering, ((Choice)session.current).getOptionDescriptions().length - 1);
                    }
                }
                else if(e.getKeyCode() == 10 && !session.state.transitioning)session.toggle();
            }
        });
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
    public static final char STATE_CHANGE_SYMBOL = '{', STATE_CHANGE_SEPERATOR = ':';
    public static final String PRINT_DELIMITER = "  ", STATE_CHANGE_DELIMITER = ",", START_SYMBOL = ":", END_SYMBOL = "|";
    public static final Image[] media = {new ImageIcon(Run.class.getResource("sample.gif")).getImage(),
                                         new ImageIcon(Run.class.getResource("another.gif")).getImage()};
    public String playerName;
    public GameEvent history, current, history_curr;
    private GameEvent gamedata;
    public State state;
    public int considering;
    public boolean on_standby, ended, selected;
    public Thread request;
    public Game(String playerName, JFrame frame, BufferedReader dataStream) throws Exception
    {
        this.playerName = playerName;
        this.history = null;
        this.current = null;
        this.history_curr = null;
        String line = dataStream.readLine().trim();
        int stateChangePosition = line.indexOf(Game.STATE_CHANGE_SYMBOL);
        if(stateChangePosition == -1) this.gamedata = new GameEvent(line, new int[State.STATE_SIZE]);
        else this.gamedata = new GameEvent(line.substring(0, stateChangePosition).trim(), State.parse(line.substring(stateChangePosition + 1, line.length() - 1)));
        this.state = new State(frame);
        this.on_standby = true;
        this.ended = false;
        this.selected = false;
        this.considering = 0;
        this.request = new Thread();
        getGameData(dataStream, this.gamedata);
        dataStream.close();
        //printGameData(this.gamedata, "");
    }
    private static void getGameData(BufferedReader br, GameEvent root) throws Exception
    {
        String line = null;
        boolean next_option = false;
        while((line = br.readLine()) != null)
        {
            if(next_option && line.length() > 1)
            {
                if(line.endsWith(END_SYMBOL)){((Choice)root).addOption(line.trim().substring(1, line.length() - 1).trim());}
                else
                {
                    ((Choice)root).addOption(line.trim().substring(1).trim());
                    getGameData(br, ((Choice)root).getLastOption());
                }
            }
            else if(line.endsWith(END_SYMBOL))
            {
                if(line.length() > 1)
                {
                    line = line.trim().substring(0, line.length() - 1).trim();
                    int stateChangePosition = line.indexOf(Game.STATE_CHANGE_SYMBOL);
                    if(stateChangePosition == -1) root.next = new GameEvent(line);
                    else root.next = new GameEvent(line.substring(0, stateChangePosition).trim(), State.parse(line.substring(stateChangePosition + 1, line.length() - 1)));
                }
                return;
            }
            else
            {
                if(line.endsWith(START_SYMBOL))
                {
                    line = line.trim().substring(0, line.length() - 1).trim();
                    int stateChangePosition = line.indexOf(Game.STATE_CHANGE_SYMBOL);
                    if(stateChangePosition == -1) root.next = new Choice(line);
                    else root.next = new Choice(line.substring(0, stateChangePosition).trim(), State.parse(line.substring(stateChangePosition + 1, line.length() - 1)));
                    next_option = true;
                }
                else
                {
                    line = line.trim();
                    int stateChangePosition = line.indexOf(Game.STATE_CHANGE_SYMBOL);
                    if(stateChangePosition == -1) root.next = new GameEvent(line);
                    else root.next = new GameEvent(line.substring(0, stateChangePosition).trim(), State.parse(line.substring(stateChangePosition + 1, line.length() - 1)));
                    next_option = false;
                }
                root = root.next;
            }
        }
    }
    public void next()
    {
        if(on_standby || ended || request.isAlive()) return;
        if(current == null)
        {
            this.state.applyChanges(gamedata.stateChange);
            current = gamedata;
            this.toggle();
            return;
        }
        if(history == null)
        {
            history = new GameEvent(current.eventDescripton, current.stateChange);
            history_curr = history;
        }
        else
        {
            history_curr.next = new GameEvent(current.eventDescripton, current.stateChange);
            history_curr = history_curr.next;
        }
        if(current instanceof Choice)
        {
            this.request = new Thread(new Runnable(){
                public void run()
                {
                    while(!selected){try{Thread.sleep(1);}catch(InterruptedException e){}}
                    current = ((Choice)current).select(considering - 1, state);
                    selected = false;
                }
            });
            this.request.start();
        }
        else
        {
            if(current.next == null) ended = true;
            else this.state.applyChanges(current.next.stateChange);
            current = current.next;
        }
        this.toggle();
    }
    private static void printGameData(GameEvent event, String delim)
    {
        while(event != null)
        {
            System.out.println(delim + event.eventDescripton + " -> " + State.getChangeDescription(event.stateChange));
            if(event instanceof Choice)
            {
                String[] opts = ((Choice)event).getOptionDescriptions();
                for(int i = 0; i < opts.length; i++)
                {
                    System.out.println(delim + (i+1) + ") " + opts[i] + " -> " + State.getChangeDescription(((Choice)event).getStateChange(i)));
                    printGameData(((Choice)event).getOption(i-1).next, delim + PRINT_DELIMITER);
                }
                return;
            }
            else event = event.next;
        }
    }
    private static int getFittableUpto(String str, int maxLen)
    {
        if(str.length() <= maxLen) return str.length();
        int i = maxLen - 1;
        for(; i >= 0; i--) if(str.charAt(i) == ' ')break;
        return i;
    }
    public void draw(Graphics g, Dimension d)
    {
        // base
        g.setColor(this.state.base);
        g.fillRect(0, 0, (int)d.getWidth(), (int)d.getHeight());
        // gif
        g.drawImage(media[this.state.gif],
                    (int)Math.max((d.getWidth() - Game.media[this.state.gif].getWidth(null))/2, 0),
                    (int)Math.max((d.getHeight() - Game.media[this.state.gif].getHeight(null))/2, 0),
                    media[this.state.gif].getWidth(null),
                    media[this.state.gif].getHeight(null), null, null);
        // inverted boundary
        Graphics2D g2d = (Graphics2D)g;
        g2d.setColor(new Color(255 - this.state.base.getRed(), 255 - this.state.base.getGreen(), 255 - this.state.base.getBlue()));
        g2d.setStroke(new BasicStroke(10.0f));
        g2d.drawRoundRect(0, 0, d.width, d.height, 20, 20);
        g2d.setStroke(new BasicStroke());
        // current event text
        g2d.setColor(new Color(0, 0, 0, 127));
        g2d.fillRect(0, (int)(0.8 * d.getHeight()), (int)d.getWidth(), (int)(0.2 * d.getHeight()));
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Helevetica", Font.PLAIN, (int)(0.02 * (d.getHeight() + d.getWidth()))));
        if(this.request.isAlive())
        {
            String[] opts = ((Choice)this.current).getOptionDescriptions();
            int y = (int)(0.84 * d.getHeight()), x = (int)(0.01 * d.getWidth()), delta = g.getFont().getSize() + 3, index, maxLen;
            g2d.setColor(this.state.blinker);
            g2d.drawString(">>", x, y + this.considering * delta);
            x += (int)(1.3 * delta);
            maxLen = (int)Math.floor(2 * d.getWidth() / g.getFont().getSize()) - 2;
            g2d.setColor(Color.WHITE);
            for(int i = 0; i < opts.length; i++)
            {
                while(opts[i] != "")
                {
                    index = getFittableUpto(opts[i], maxLen);
                    g2d.drawString(opts[i].substring(0, index), x, y);
                    if(index == opts[i].length()) opts[i] = "";
                    else opts[i] = opts[i].substring(index, opts[i].length()).trim();
                    y += delta;
                }
            }
        }
        else
        {
            String text = "Welcome " + this.playerName + " ! Use Enter and UP / DOWN arrow keys !";
            if(this.ended) text = "Thanks for playing !";
            else if(this.state.transitioning && this.history_curr != null) text = this.history_curr.eventDescripton;
            else if(!this.state.transitioning && this.current != null) text = this.current.eventDescripton;
            int y = (int)(0.84 * d.getHeight()), x = (int)(0.01 * d.getWidth()), delta = g.getFont().getSize() + 3, index, maxLen;
            maxLen = (int)Math.floor(2 * d.getWidth() / g.getFont().getSize());
            while(text != "")
            {
                index = getFittableUpto(text, maxLen);
                g2d.drawString(text.substring(0, index), x, y);
                if(index == text.length()) text = "";
                else text = text.substring(index, text.length()).trim();
                y += delta;
            }
        }
        // overlay
        g.setColor(this.state.overlay);
        g.fillRect(0, 0, (int)d.getWidth(), (int)d.getHeight());
    }
    public void toggle(){this.on_standby = !this.on_standby;}
}
class State
{
    public static final int MAX_BAR_VALUE = 100, STATE_SIZE = 4;
    private int water_bar_value, food_bar_value, energy_bar_value;
    public int gif;
    private JFrame frame;
    private boolean slow_base_change;
    public Color base, overlay, blinker;
    public boolean transitioning;
    public State(JFrame frame)
    {
        this.water_bar_value = (int)(0.75 * MAX_BAR_VALUE);
        this.food_bar_value = (int)(0.75 * MAX_BAR_VALUE);
        this.energy_bar_value = (int)(0.75 * MAX_BAR_VALUE);
        this.base = new Color(255, 255, 255, 255);
        this.overlay = new Color(0, 0, 0, 0);
        this.slow_base_change = false;
        this.gif = 0;
        this.frame = frame;
        this.transitioning = false;
        new Thread(new Runnable(){
            public void run()
            {
                Random r = new Random();
                int value = 255, delta = 5, sleep = 1 + r.nextInt(8);
                while(true)
                {
                    value += delta;
                    sleep += 1;
                    sleep = Math.min(sleep, 25);
                    if(value <= 0 || value >= 255)
                    {
                        delta = -delta;
                        value = Math.min(255, Math.max(0, value));
                        sleep = 1 + r.nextInt(10);
                        try
                        {
                            if(!slow_base_change)Thread.sleep(300 + r.nextInt(700));
                            else Thread.sleep(10000);
                        }
                        catch(InterruptedException e){}
                    }
                    base = new Color(value, value, value);
                    try{Thread.sleep(sleep);}
                    catch(InterruptedException e){}
                }
            }
        }).start();
        new Thread(new Runnable(){
            public void run()
            {
                int alpha = 255;
                while(true)
                {
                    while(alpha >= 0)
                    {
                        blinker = new Color(255, 255, 255, alpha);
                        alpha--;
                        try{Thread.sleep(1);}
                        catch(InterruptedException e){}
                    }
                    try{Thread.sleep(300);}
                    catch(InterruptedException e){}
                    while(alpha < 255)
                    {
                        alpha++;
                        blinker = new Color(255, 255, 255, alpha);
                        try{Thread.sleep(1);}
                        catch(InterruptedException e){}
                    }
                    try{Thread.sleep(300);}
                    catch(InterruptedException e){}
                }
            }
        }).start();
    }
    public void applyChanges(int[] values)
    {
        transitioning = true;
        this.water_bar_value += values[0];
        this.food_bar_value += values[1];
        this.energy_bar_value += values[2];
        new Thread(new Runnable()
        {
            public void run()
            {
                int new_gif = values[3], alpha = 0;
                if(new_gif == gif)
                {
                    transitioning = false;
                    return;
                }
                while(alpha <= 255)
                {
                    overlay = new Color(0, 0, 0, alpha);
                    alpha++;
                    try{Thread.sleep(3);}
                    catch(InterruptedException e){}
                }
                gif = new_gif;
                transitioning = false;
                Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
                int WIDTH = (int)d.getWidth(), HEIGHT = (int)d.getHeight();
                frame.setBounds(Math.max((WIDTH - (int)(1.04 * Game.media[gif].getWidth(null)))/2, 0),
                                Math.max((HEIGHT - (int)(1.08 * Game.media[gif].getHeight(null)))/2, 0),
                                Math.min((int)(1.08 * Game.media[gif].getWidth(null)), WIDTH),
                                Math.min((int)(1.16 * Game.media[gif].getHeight(null)), HEIGHT));
                try{Thread.sleep(50);}
                catch(InterruptedException e){}
                while(alpha > 0)
                {
                    alpha--;
                    overlay = new Color(0, 0, 0, alpha);
                    try{Thread.sleep(3);}
                    catch(InterruptedException e){}
                }
            }
        }).start();
        this.water_bar_value = Math.min(MAX_BAR_VALUE, Math.max(0, water_bar_value));
        this.food_bar_value = Math.min(MAX_BAR_VALUE, Math.max(0, food_bar_value));
        this.energy_bar_value = Math.min(MAX_BAR_VALUE, Math.max(0, energy_bar_value));
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
            case "gif": return 3;
            default: return -1;
        }
    }
    public static String getChangeDescription(int[] values)
    {
        String desc = "";
        desc += "water: " + (values[0] < 0 ? "" : "+") + values[0] + ", ";
        desc += "food: " + (values[1] < 0 ? "" : "+") + values[1] + ", ";
        desc += "energy: " + (values[2] < 0 ? "" : "+") + values[2] + ", ";
        desc += "gif: " + values[3];
        return desc;
    }
    public void toggle_base_change(){this.slow_base_change = !this.slow_base_change;}
}
class GameEvent
{
    public String eventDescripton;
    public GameEvent next;
    public int[] stateChange;
    public GameEvent()
    {
        this.eventDescripton = "";
        this.next = null;
        this.stateChange = new int[State.STATE_SIZE];
    }
    public GameEvent(String eventDescripton)
    {
        this.eventDescripton = eventDescripton;
        this.next = null;
        this.stateChange = new int[State.STATE_SIZE];
    }
    public GameEvent(String eventDescripton, int[] stateChange)
    {
        this.eventDescripton = eventDescripton;
        this.next = null;
        this.stateChange = stateChange;
    }
}
class Choice extends GameEvent
{
    public ArrayList<GameEvent> options;
    public ArrayList<int[]> stateChanges;
    public Choice()
    {
        super();
        this.options = new ArrayList<GameEvent>();
        this.stateChanges = new ArrayList<int[]>();
    }
    public Choice(String desc)
    {
        super(desc);
        this.options = new ArrayList<GameEvent>();
        this.stateChanges = new ArrayList<int[]>();
    }
    public Choice(String desc, int[] stateChange)
    {
        super(desc, stateChange);
        this.options = new ArrayList<GameEvent>();
        this.stateChanges = new ArrayList<int[]>();
    }
    public Choice(String desc, String[] options)
    {
        super(desc);
        this.options = new ArrayList<GameEvent>();
        this.stateChanges = new ArrayList<int[]>();
        for(int i = 0; i < options.length; i++){this.addOption(options[i]);}
    }
    public void addOption(String option)
    {
        int stateChangePosition = option.indexOf(Game.STATE_CHANGE_SYMBOL);
        if(stateChangePosition == -1)
        {
            this.stateChanges.add(new int[State.STATE_SIZE]);
            stateChangePosition = option.length();
        }
        else this.stateChanges.add(State.parse(option.substring(stateChangePosition + 1, option.length() - 1)));
        if(this.next == null){this.next = new GameEvent(option.substring(0, stateChangePosition).trim());}
        else{this.options.add(new GameEvent(option.substring(0, stateChangePosition).trim()));}
    }
    public GameEvent getOption(int option)
    {
        if(option == -1) return this.next;
        return this.options.get(option);
    }
    public int[] getStateChange(int option)
    {
        if(option == -1) return this.stateChange;
        return this.stateChanges.get(option);
    }
    public GameEvent select(int option, State s)
    {
        s.applyChanges(this.getStateChange(option));
        return this.getOption(option);
    }
    public String[] getOptionDescriptions()
    {
        String[] options = new String[this.options.size() + 1];
        for(int i = 0; i <= this.options.size(); i++){options[i] = this.getOption(i-1).eventDescripton;}
        return options;
    }
    public GameEvent getLastOption(){return this.getOption(this.options.size() - 1);}
}