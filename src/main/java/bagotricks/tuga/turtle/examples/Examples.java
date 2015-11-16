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
        List<Program> programs = new ArrayList<>();
        for (String name : NAMES) {
            Program program = new Program();
            program.setContent(getContent(name));
            program.setGroup(ProgramGroup.EXAMPLES);
            program.setId(name);
            program.setName(name);
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
