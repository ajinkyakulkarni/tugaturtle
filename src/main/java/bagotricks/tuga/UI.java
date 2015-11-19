package bagotricks.tuga;

import java.awt.event.ActionListener;
import javax.swing.JButton;

public class UI {

    public static JButton createButton(String text, ActionListener listener) {
        JButton button = new JButton(text);
        button.addActionListener(listener);
        return button;
    }

}
