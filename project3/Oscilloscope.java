// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.awt.geom.*;
import javax.swing.*;
import java.util.concurrent.locks.*;

/**
   A display for Modulation signals.
*/

public class Oscilloscope extends JComponent
    {
    public static final int WAVE_SIZE = 100;
    public static final int WAVE_HEIGHT = 100;
    public static final int BORDER = 8;
    public static final float STROKE_WIDTH = 1f;
    
    private Module amplitude = new ConstantValue(1.0);
    
    Color axisColor = DARK_GREEN;
    Stroke stroke;

    // are we currently triggered?
    boolean trig = false;
    
    // wave is a circular buffer of the current wave information; and wavePos is our position in it
    double[] wave = new double[WAVE_SIZE];
    int wavePos = 0;

    static final Color DARK_GREEN = new Color(0, 127, 0);
    static final Color DARK_BLUE = new Color(0, 0, 180);

    public static final int DEFAULT_DELAY = 64;
    public int delay = DEFAULT_DELAY;
    int count = DEFAULT_DELAY - 1;
        
    public int getDelay() 
        {
        return delay; 
        }
                
    public void setDelay(int val) 
        { 
        if (val >= 1) 
            {
			delay = val; 
			count = delay - 1;
            }
        }

    public OModule getModule() { return new OModule(); }

    ReentrantLock lock = new ReentrantLock();
                        
    public class OModule extends Module
        {
        public void setAmplitudeModule(Module amplitude) {
            Oscilloscope.this.amplitude = amplitude;
            }

        public double getAmplitudeModuleOutputAsValue() {
            return getAmplitudeModule().getValue();
            }

        public Module getAmplitudeModule() {
            return Oscilloscope.this.amplitude;
            }

        public double tick(long tickCount)
            {
            // this will be race condition but I gotta do it, it's too expensive otherwise
            count++;
            if (count >= delay)
				{
				lock.lock();
				try
					{
						{
						count = 0;
						wave[wavePos] = getAmplitudeModuleOutputAsValue() * 2 - 1.0;
						wavePos++;
						if (wavePos >= wave.length) wavePos = 0;
						}
					return wave[wavePos];
					}
				finally
					{
					lock.unlock();
					}
				}
			return 0;
            }
        };

    public Oscilloscope()
        {
        stroke = new BasicStroke(STROKE_WIDTH);

        Timer timer = new Timer(25, new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                repaint();
                SwingUtilities.invokeLater(new Runnable() { public void run() { } });
                }
            });
        timer.start();
        }
        
    public Dimension getMinimumSize() { return new Dimension(WAVE_SIZE + BORDER * 2, WAVE_HEIGHT + BORDER * 2); }
    public Dimension getPreferredSize() { return new Dimension(WAVE_SIZE + BORDER * 2, WAVE_HEIGHT + BORDER * 2); }
    public Dimension getMaximumSize() { return new Dimension(WAVE_SIZE + BORDER * 2, WAVE_HEIGHT + BORDER * 2); }
    
    public void paintComponent(Graphics graphics)
        {
        int p = 0;
        double[] w = null;
        boolean done = false;
        
        lock.lock();
        try
            {
            p = wavePos;
            w = (double[]) (wave.clone());
            done = true;
            }
        finally
            {
            lock.unlock();
            }
        if (!done) return;
                        
        // fill with black regardless
        Graphics2D g = (Graphics2D) graphics;
        int height = getHeight();
        int width = getWidth();
        g.setColor(Color.BLACK);
        g.fill(new Rectangle2D.Double(0, 0, width, height));

        // draw axis                                        
        g.setColor(trig ? DARK_BLUE : DARK_GREEN);
        g.draw(new Line2D.Double(BORDER, BORDER + WAVE_HEIGHT / 2.0, WAVE_SIZE + BORDER, BORDER + WAVE_HEIGHT / 2.0));
        g.draw(new Line2D.Double(BORDER + WAVE_SIZE / 2.0, BORDER, BORDER + WAVE_SIZE / 2.0, WAVE_HEIGHT + BORDER));

        // Draw the wave
        g.setColor(Color.WHITE);
        g.setStroke(stroke);
        int q = 0;
        for(int i = 0; i < WAVE_SIZE - 1; i++)
            {
            q = p + 1;
            if (q >= WAVE_SIZE) q = 0;
            double x = i + BORDER;
            double y = WAVE_HEIGHT * (1 - ((w[p] + 1) / 2)) + BORDER;
            double xx = x + 1;
            double yy = WAVE_HEIGHT * (1 - ((w[q] + 1) / 2)) + BORDER;
            g.draw(new Line2D.Double(x, y, xx, yy));
            p++;
            if (p >= WAVE_SIZE) p = 0;
            }
        }
    }
