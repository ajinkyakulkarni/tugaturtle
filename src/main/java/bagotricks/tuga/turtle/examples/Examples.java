package bagotricks.tuga.turtle.examples;

import java.util.ArrayList;
import java.util.List;

import bagotricks.tuga.Library;
import bagotricks.tuga.Program;
import bagotricks.tuga.ProgramGroup;

public class Examples {

	private static final String[] NAMES = {"Angle Patterns", "Basic Square", "Dashed Line", "House", "Spiral", "Square Function", "Wanderer", "Wanderer Plus",};

	private static final List<Program> PROGRAMS = buildPrograms();

	private static List<Program> buildPrograms() {
		List<Program> programs = new ArrayList<Program>();
		for (int n = 0; n < NAMES.length; n++) {
			String name = NAMES[n];
			Program program = new Program();
			program.content = getContent(name);
			program.group = ProgramGroup.EXAMPLES;
			program.id = name;
			program.name = name;
			programs.add(program);
		}
		return programs;
	}

	public static List<Program> getAll() {
		return PROGRAMS;
	}

	public static String getContent(String name) {
		return Library.readAll(Examples.class.getResourceAsStream(name + ".rb"));
	}

}
