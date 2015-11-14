package bagotricks.tuga.turtle;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Path {

	public Color color = Color.BLACK;

	public List<Step> steps;

	public double width = 4.5;

	public Path() {
		steps = new ArrayList<Step>();
		steps.add(new Step(0, 0, false, null));
	}

}
