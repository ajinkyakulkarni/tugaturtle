package bagotricks.tuga.turtle;

import java.util.ArrayList;
import java.util.List;

public class Drawing {

	public List<Path> paths;

	public Drawing() {
		reset();
	}

	public Path lastPath() {
		return paths.get(paths.size() - 1);
	}

	public void reset() {
		paths = new ArrayList<Path>();
		paths.add(new Path());
	}

}
