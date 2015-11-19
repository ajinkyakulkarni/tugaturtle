package bagotricks.tuga.turtle;

import bagotricks.tuga.Controller;
import bagotricks.tuga.Engine;
import bagotricks.tuga.Library;
import javax.imageio.ImageIO;
import javax.swing.*;

import bagotricks.tuga.Thrower;
import bagotricks.tuga.MainUi;
import bagotricks.tuga.turtle.examples.Examples;

public class Main {

    public static void main(String[] args) {
        try {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // TODO No system laf. Oh well.
            }
            
            Library library = new Library("Tuga Turtle", Examples.getAll(), Examples.getContent("Angle Patterns"));
            
            Engine engine = new TurtleEngine();
            engine.init();
            Controller controller = new Controller(engine, library);
            
            MainUi ui = new MainUi(controller);
            ui.icon = ImageIO.read(Main.class.getResource("turtle128.png"));
            ui.title = "Tuga Turtle (from Bagotricks.com)";
            SwingUtilities.invokeAndWait(ui);
        } catch (Exception e) {
            Thrower.throwAny(e);
        }
    }

}
