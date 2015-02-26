import java.util.LinkedList;
import java.util.Random;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.Sprite;

import lejos.nxt.Button;
import lejos.nxt.LCD;
import lejos.nxt.SensorPort;
import lejos.nxt.Sound;
import lejos.nxt.TouchSensor;
import lejos.util.Delay;
/**
 * Drives an instance of the game Simon on the NXT using LeJOS
 * @author whmeitzler
 * 
 */
public class Simon {
    /**
     * 
     * @author whmeitzler
     * An inner data type to organize Color attributes
     * Each Color has an associated TouchSensor (Attached to a given SensorPort)
     * and a tone frequency (in Hz, as an int). 
     * 
     * The Color class is also responsible for generating new random colors and 
     * determining which Color was most recently selected by the user
     * 
     * All hardware is abstracted away in this class. 
     */
    enum Color {
        BLUE(SensorPort.S4, 415), WHITE(SensorPort.S3, 247), BLACK(
                SensorPort.S1, 207), YELLOW(SensorPort.S2, 311);
        
        public final TouchSensor sensor;// A Color's physical sensor
        public final int tone; // A Color's tone frequency
        private static final long MINIMUM_DELAY = 50, MAXIMUM_DELAY = 1000;
        private static final Random RANDOM = new Random();// For randomizing
                                                          // Colors
        public static final Color[] VALUES = Color.values();// A static instance
                                                            // of the Colors
        public boolean prevoiuslySelected = false;          //If this color has been selected on prevoius check
                                                            //Updated by getNextSelected()
        private static final int SIZE = VALUES.length;// Duh
        private static long lastObserved = 0;// last time a check was made
                                             // (Debounce minimization)
        public static Color lastColor = BLUE;// Useful to Simon

        /**
         * Creates an object of type Color. For use by enum only.
         * @param port The SensorPort this Color is associated with
         * @param tone The tone frequency (in Hz) this Color plays
         */
        Color(SensorPort port, int tone) {
            this.sensor = new TouchSensor(port);
            this.tone = tone;
        }
        /**
         * Selects a random instance of Color and returns it
         * Uses a Random object to make selection. 
         * @return A randomly selected Color
         */
        public static Color random() {
            return VALUES[RANDOM.nextInt(SIZE)];
        }
        /**
         * Immediately checks if a given Color is selected in hardware
         * Use this rather than checking if the Color's Sensor is pressed-
         * the physical robot inverts this. 
         * @return
         */
        public boolean isSelected() {
            return this.sensor.isPressed() == false;
        }
        /**
         * Returns the next selected Color within a given time period
         * Returns null if the time period is exceeded
         * If this method is called twice in a row, the method waits until a 
         * minimum delay has occurred for the hardware to settle. 
         * Time period is controlled by Color.MAXIMUM_DELAY
         * The minimum time between polls is controlled by Color.MIMIMUM_DELAY
         * @return The next Color selected (Can be null!)
         */
        public static Color getNextSelected() {
            long start = System.currentTimeMillis();
            while ((System.currentTimeMillis() - lastObserved) < MINIMUM_DELAY)
                ;
            while (System.currentTimeMillis() - start < MAXIMUM_DELAY) {
                for (Color m : Color.VALUES)
                    if (m.isSelected() && m.prevoiuslySelected == false) {
                        m.prevoiuslySelected = true;
                        lastObserved = System.currentTimeMillis();
                        lastColor = m;
                        return m;
                    }else{
                        m.prevoiuslySelected = false;
                    }
                
            }
            lastObserved = System.currentTimeMillis();
            return null;
        }
    };
    LinkedList<Color> gameQueue;
    int tone_length, pause_length, sequence_delay, lose_tone;
    final int WIN_ROUND = 20;
    // LCD constants
    final int SW = LCD.SCREEN_WIDTH;
    final int SH = LCD.SCREEN_HEIGHT;
    Graphics g;
    /**
     * Default constructor. 
     */
    Simon() {
        gameQueue = new LinkedList<Color>();
        tone_length = 420;
        sequence_delay = 800;
        pause_length = 150;
        g = new Graphics();
    }

    /**
     * Play a single game of Simon
     * Returns when player has won or lost
     */
    void playGame() {
        boolean successfulRun = false;
        do {
            gameQueue.add(Color.random());
            adjustDifficulty();
            displayPath();
            successfulRun = getResponsePath();
            if(successfulRun){
                displayLevel();
                Sound.beepSequenceUp();
                Delay.msDelay(200);
            }    
            if(successfulRun){
                Sound.beepSequenceUp();
                displayLevel();
                Delay.msDelay(sequence_delay);
            }    
        } while (successfulRun && gameQueue.size() < WIN_ROUND);
        if (successfulRun)
            win();
        else
            lose();
    }

    /**
     * Informs the user he has lost
     * Now with extra mocking!
     */
    private void lose() {
        g.setFont(Font.getFont(0, 0, Font.SIZE_LARGE));
        g.drawString("You Lose!", SW / 2, 32, Graphics.HCENTER
                | Graphics.BASELINE);
        Sound.playTone(65, 1500);
    }

    /**
     * Informs the user he has won!
     * Plays a small victory jingle
     */
    private void win() {
        g.setFont(Font.getFont(0, 0, Font.SIZE_LARGE));
        g.drawString("You Win!", SW / 2, 32, Graphics.HCENTER
                | Graphics.BASELINE);
        int tone = Color.lastColor.tone;
        Sound.playTone(tone, 70);
        Delay.msDelay(75);
        for (int i = 0; i < 3; i++) {
            Sound.playTone(tone, 70);
            Delay.msDelay(100);
        }
        Sound.playTone(tone, 70);
        Delay.msDelay(100);
        Sound.playTone(310, 100);
        Delay.msDelay(100);
        Sound.playTone(252, 100);
        Delay.msDelay(100);
        Sound.playTone(209, 100);
        Delay.msDelay(100);
        Sound.playTone(415, 100);
        Delay.msDelay(100);
        Sound.playTone(415, 100);
        Delay.msDelay(100);
        Sound.playTone(415, 100);
        Delay.msDelay(100);
    }

    /**
     * Clears out the game queue and offers the user to start another game
     */
    public void reset() {
        gameQueue.clear();
        g.setFont(Font.getFont(0, 0, Font.SIZE_MEDIUM));
        g.drawString("Play again?", SW / 2, SH - 10, Graphics.HCENTER
                | Graphics.BASELINE);
        g.drawString("Push Enter!", SW / 2, SH, Graphics.HCENTER
                | Graphics.BASELINE);
        Button.waitForAnyPress();
        LCD.clear();
    }

    /**
     * Increases the speed of the game at points as defined by original MB game
     */
    private void adjustDifficulty() {
        int length = gameQueue.size();
        if (length <= 5) {
            tone_length = 420;
        }
        if (length >= 6 && length <= 13) {
            tone_length = 320;
        }
        if (length >= 14 && length <= 31) {
            tone_length = 220;
        }
    }

    /**
     * Shows the current state of the game queue to the user
     * Remembering that state is his job!
     */
    void displayPath() {
        for (Color m : gameQueue) {
            displayColor(m);
            Sound.playTone(m.tone, tone_length);
            Delay.msDelay(tone_length + pause_length);
            LCD.clear();
        }
    }

    /**
     * Paints the appropriate color and location to the LCD to indicate a Color
     * @param Which color to display. Will return immediately if m is null. 
     */
    public void displayColor(Color m) {
        if(m == null)
            return;
        LCD.clear();
        Font large = Font.getFont(0, 0, Font.SIZE_LARGE);
        Image base = Image.createImage(SW, large.getHeight());
        Graphics bg = base.getGraphics();
        bg.setFont(large);
        switch (m) {
        case BLUE:// Top
            bg.drawString(m.name(), SW / 2, 0, Graphics.HCENTER);
            g.drawImage(base, 0, 0, 0);
            break;
        case YELLOW:// Bottom
            bg.drawString(m.name(), SW / 2, 0, Graphics.HCENTER);
            Image rotImage = Image.createImage(base, 0, 0, SW,
                    base.getHeight(), Sprite.TRANS_ROT180);
            g.drawImage(base, 0, SH - 1, Graphics.BOTTOM);
            break;
        case WHITE:// Left
            bg.drawString(m.name(), SH / 2, 0, Graphics.HCENTER);
            rotImage = Image.createImage(base, 0, 0, SH, base.getHeight(),
                    Sprite.TRANS_ROT90);
            g.drawImage(rotImage, 0, 0, 0);
            break;
        case BLACK:// Right
            bg.drawString(m.name(), SH / 2, 0, Graphics.HCENTER);
            rotImage = Image.createImage(base, 0, 0, SH, base.getHeight(),
                    Sprite.TRANS_ROT270);
            g.drawImage(rotImage, SW - 1, 0, Graphics.RIGHT);
            break;
        default:
            break;
        }
    }
    /**
     * Prints the level (The length of the game queue) to the center of the screen
     */
    public void displayLevel(){
        Font large = Font.getFont(0, 0, Font.SIZE_LARGE);
        Image base = Image.createImage(SW, large.getHeight());
        Graphics bg = base.getGraphics();
        bg.setFont(large);
        String score = "" + gameQueue.size();//gameQueue.size();
        bg.drawString(score, SW / 2, 0, Graphics.HCENTER);
        g.drawImage(base, 0, SH/2 - large.getHeight()/2, 0);
    }
    /**
     * Gets responses from the player, checking each against the game path
     * @return True if the player was successful replicating the game path
     */
    public boolean getResponsePath() {
        Color selected;
        for (Color m : gameQueue) {
            selected = Color.getNextSelected();
            if (selected == null)
                return false;
            if (selected.equals(m))
                Sound.playTone(m.tone, tone_length);
            else
                return false;
        }
        return true;
    }

    /**
     * main method
     * @param args 
     */
    public static void main(String[] args) {
        Simon s = new Simon();
        while (true) {
            Delay.msDelay(1000);
            s.playGame();
            s.reset();
        }
    }
}

