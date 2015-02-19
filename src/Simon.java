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

public class Simon {
    enum Location{TOP, BOTTOM, LEFT, RIGHT};
    enum Move{ BLUE   (SensorPort.S4, 415), 
               WHITE  (SensorPort.S3, 247), 
               BLACK  (SensorPort.S1, 207),
               YELLOW (SensorPort.S2, 311);
        public final TouchSensor sensor;
        public final int tone;
        private static long lastObserved = 0;
        private static final long MINIMUM_DELAY = 120, MAXIMUM_DELAY = 750;
        Move(SensorPort port, int tone){
            this.sensor = new TouchSensor(port);
            this.tone = tone;
        }
        public static Move lastMove = BLUE;
        public static final Move[] VALUES = Move.values();
        private static final int SIZE = VALUES.length;
        private static final Random RANDOM = new Random();
        public static Move random()  {
            return VALUES[RANDOM.nextInt(SIZE)];
        }
        public boolean isSelected(){
            return this.sensor.isPressed() == false;          
        }
        public static Move getNextSelected(){
            long start = System.currentTimeMillis();
            while((System.currentTimeMillis() - lastObserved) < MINIMUM_DELAY);
            while(System.currentTimeMillis() - start < MAXIMUM_DELAY){
                for(Move m : Move.VALUES)
                    if(m.isSelected()){
                        while(m.isSelected())
                               ;
                        lastObserved = System.currentTimeMillis();
                        lastMove = m;
                        return m;
                    }
            }
            lastObserved = System.currentTimeMillis();
            return null;
        }
    };
    LinkedList<Move> gameQueue;
    int tone_length, pause_length, sequence_delay, lose_tone;
    final int WIN_ROUND = 20;
    final int SW = LCD.SCREEN_WIDTH;
    final int SH = LCD.SCREEN_HEIGHT;
    Graphics g;
    Simon(){
       gameQueue = new LinkedList<Move>();
       tone_length = 420;
       sequence_delay = 800;
       pause_length = 150;
       g = new Graphics();
    }
    void playGame(){
        boolean successfulRun = false;
        do{
            gameQueue.add(Move.random());
            adjustDifficulty();
            displayPath();
            successfulRun = getResponsePath();
            Delay.msDelay(sequence_delay);
        }while(successfulRun && gameQueue.size() < WIN_ROUND); 
        if(successfulRun) win();
        else lose();
    }
    private void lose(){
        g.setFont(Font.getFont(0, 0, Font.SIZE_LARGE));
        g.drawString("You Lose!", SW / 2, 32, Graphics.HCENTER | Graphics.BASELINE);
        Sound.playTone(65, 1500);
    }
    private void win(){
        g.setFont(Font.getFont(0, 0, Font.SIZE_LARGE));
        g.drawString("You Win!", SW / 2, 32, Graphics.HCENTER | Graphics.BASELINE);
        int tone = Move.lastMove.tone;
        Sound.playTone(tone, 70);
        Delay.msDelay(75);
        for(int i =0; i < 3; i++){
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
    public void reset(){
        gameQueue.clear();
        g.setFont(Font.getFont(0, 0, Font.SIZE_MEDIUM));
        g.drawString("Play again?", SW / 2, SH - 10, Graphics.HCENTER | Graphics.BASELINE);
        g.drawString("Push Enter!", SW / 2, SH, Graphics.HCENTER | Graphics.BASELINE);
        Button.waitForAnyPress();
        LCD.clear();
    }
    private void adjustDifficulty() {
        int length = gameQueue.size();
        if(length <= 5){
            tone_length = 420;
        }
        if(length >= 6 && length <= 13){
            tone_length = 320;
        }
        if(length >= 14 && length <= 31){
            tone_length = 220;
        }
    }
    void displayPath(){
        for(Move m : gameQueue){
            //LCD.drawString(m.toString(), 2, LCD.CELL_HEIGHT /2 );
            drawButton(m);
            Sound.playTone(m.tone, tone_length);
            Delay.msDelay(tone_length + pause_length);
            LCD.clear();
        }
    }
    public void drawButton(Move m){
        Font large = Font.getFont(0, 0, Font.SIZE_LARGE);
        Image base = Image.createImage(SW, large.getHeight());
        Graphics bg = base.getGraphics();
        bg.setFont(large);
        switch(m){
        case BLUE://Top
            bg.drawString(m.name(), SW / 2, 0, Graphics.HCENTER);
            g.drawImage(base, 0, 0, 0);
            break;
        case YELLOW://Bottom
            bg.drawString(m.name(), SW / 2, 0, Graphics.HCENTER);
            Image rotImage = Image.createImage(base, 0, 0, SW, base.getHeight(), Sprite.TRANS_ROT180);
            g.drawImage(base, 0, SH - 1, Graphics.BOTTOM);
            break;
        case WHITE://Left
            bg.drawString(m.name(), SH / 2, 0, Graphics.HCENTER);
            rotImage = Image.createImage(base, 0, 0, SH, base.getHeight(), Sprite.TRANS_ROT90);
            g.drawImage(rotImage, 0, 0, 0);
            break;
        case BLACK://Right
            bg.drawString(m.name(), SH / 2, 0, Graphics.HCENTER);
            rotImage = Image.createImage(base, 0, 0, SH, base.getHeight(), Sprite.TRANS_ROT270);
            g.drawImage(rotImage, SW - 1, 0, Graphics.RIGHT);
            break;
        default:
            break;
        
        }
    }
    public boolean getResponsePath(){
        Move selected;
        for(Move m : gameQueue){
            selected = Move.getNextSelected();
            if(selected == null)
                return false;
            if(selected.equals(m))
                Sound.playTone(m.tone, tone_length);
            else return false;
        }
        return true;
    }
    public static void main(String[] args){
        Simon s = new Simon();
        while(true){
            s.playGame();
            s.reset();
        }
    }
}
