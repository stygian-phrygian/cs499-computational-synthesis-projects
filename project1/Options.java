import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.concurrent.locks.ReentrantLock;

/** 
    A widget which displays a labelled options choice.  If the options
    is boolean, then a checkbox is displayed.  Otherwise a JComboBox is displayed.
*/


public class Options extends JPanel
    {
    public static final Font SMALL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 10); 

    JComboBox combo;
    JCheckBox checkbox;
    JLabel label;
    int optionNumber;
    
    ReentrantLock lock = new ReentrantLock();
    
    void _setValue(double val)
    	{
    	lock.lock();
		try
			{
			if (checkbox != null)
				{
				checkbox.setSelected(val == 1);
				}
			else
				{
				combo.setSelectedIndex((int)val);
				}
			}
		finally
			{
			lock.unlock();
			}
    	}
    	
    int _getValue()
    	{
    	lock.lock();
		try
			{
			if (checkbox != null)
				{
				return (checkbox.isSelected() ? 1 : 0);
				}
			else
				{
				return combo.getSelectedIndex();
				}
			}
		finally
			{
			lock.unlock();
			}
    	}
 
    class OptionsModule extends Module
        {
        public void setValue(double value) 
            {
            _setValue(value);
            }
                
        public double getValue()
            {
            return _getValue();
            }

        public double tick(long tickCount) { return 0; }  // unused
        };

    OptionsModule optionsModule = new OptionsModule();

    public Module getModule() { return optionsModule; }
 
    
    public Options(String title, boolean checked)
    	{
		checkbox = new JCheckBox(title);
		checkbox.setFont(SMALL_FONT);
		checkbox.putClientProperty("JComponent.sizeVariant", "small");
		checkbox.setSelected(checked);
		checkbox.addItemListener(new ItemListener()
			{
			public void itemStateChanged(ItemEvent e)
				{
				int val = (checkbox.isSelected() ? 1 : 0);
				}
			});
		setLayout(new BorderLayout());
		add(checkbox, BorderLayout.CENTER);
    	}
    	
    public Options(String title, String[] options, int def)
        {
            label = new JLabel(title);
            label.setFont(SMALL_FONT);

            combo = new JComboBox(options)
                {
                public Dimension getMinimumSize() 
                    {
                    return getPreferredSize(); 
                    }
                };

            combo.putClientProperty("JComponent.sizeVariant", "small");
            combo.setEditable(false);
            combo.setFont(SMALL_FONT);
            combo.setMaximumRowCount(32);
            combo.setSelectedIndex(def);

            combo.addItemListener(new ItemListener()
                {
                public void itemStateChanged(ItemEvent e)
                    {
                    int val = combo.getSelectedIndex();
                    }
                });
                        
            setLayout(new BorderLayout());
            add(combo, BorderLayout.CENTER);
            add(label, BorderLayout.NORTH);
            }
    }
