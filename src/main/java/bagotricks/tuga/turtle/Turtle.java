package bagotricks.tuga.turtle;

import java.awt.*;

public class Turtle {

	public double angle;

	public Color penColor;

	public boolean penDown;

	public double x;

	public double y;

	public Turtle() {
		reset();
	}

	public void reset() {
		angle = 90;
		penColor = Color.BLACK;
		penDown = true;
		x = 0;
		y = 0;
	}

}
