package bagotricks.tuga.turtle;

import java.awt.*;

public class Step {

	public Color color;

	public boolean penDown;

	public double x;

	public double y;

	public Step(double x, double y, boolean penDown, Color color) {
		this.x = x;
		this.y = y;
		this.penDown = penDown;
		this.color = color;
	}

}
