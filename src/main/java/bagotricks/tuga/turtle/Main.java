package bagotricks.tuga.turtle;

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
			MainUi ui = new MainUi();
			ui.firstContent = Examples.getContent("Angle Patterns");
			ui.engine = new TurtleEngine();
			ui.examples = Examples.getAll();
			ui.icon = ImageIO.read(Main.class.getResource("turtle128.png"));
			ui.id = "Tuga Turtle";
			ui.title = "Tuga Turtle (from Bagotricks.com)";
			SwingUtilities.invokeAndWait(ui);
		} catch (Exception e) {
			Thrower.throwAny(e);
		}
	}

}
