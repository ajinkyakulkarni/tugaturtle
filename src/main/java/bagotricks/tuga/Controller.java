package bagotricks.tuga;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Set;

public class Controller {

    private final PropertyChangeSupport pcs;

    private final Engine engine;

    private final Library library;

    private Program program;

    private String programName;

    public Controller(Engine engine, Library library) {
        pcs = new PropertyChangeSupport(this);
        this.engine = engine;
        this.library = library;
    }

    public void newProgram() {
        Program newProgram = library.newProgram();
        setProgram(newProgram);
        fireProgramListChange();
    }

    public void copyProgram() {
        Program newProgram = library.newProgram();
        library.rename(newProgram, program.getName());
        newProgram.writeContent(program.getContent());
        setProgram(newProgram);
        fireProgramListChange();
    }

    public void renameCurrent(String newName) {
        library.rename(program, newName);
        setProgramName(newName);
        fireProgramListChange();
    }

    public void deleteProgram() {
        library.updateGroup(program, ProgramGroup.TRASH);
        // Select another program from my programs to display or if there are
        // none create a new program
        Set<String> myPrograms = library.getProgramsIds(ProgramGroup.MY_PROGRAMS);
        if (myPrograms.isEmpty()) {
            newProgram();
        } else {
            String id = myPrograms.iterator().next();
            Program nextProgram = library.getProgram(id);
            setProgram(nextProgram);
        }
        fireProgramListChange();
    }

    public void restoreProgram() {
        library.updateGroup(program, ProgramGroup.MY_PROGRAMS);
        fireProgramListChange();
        fireProgramChange();
    }

    private void fireProgramListChange() {
        this.pcs.firePropertyChange("programList", null, null);
    }

    private void fireProgramChange() {
        this.pcs.firePropertyChange("program", null, null);
    }

    public Engine getEngine() {
        return engine;
    }

    public Library getLibrary() {
        return library;
    }

    public Program getProgram() {
        return program;
    }

    public void setProgram(Program program) {
        Program oldProgram = this.program;
        this.program = program;
        library.setMostRecentProgram(program);
        this.pcs.firePropertyChange("program", oldProgram, program);
        setProgramName(program.getName());
    }

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        String oldProgramName = this.programName;
        this.programName = programName;
        this.pcs.firePropertyChange("programName", oldProgramName, programName);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.removePropertyChangeListener(listener);
    }

}
