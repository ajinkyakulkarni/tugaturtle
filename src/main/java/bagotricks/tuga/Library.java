package bagotricks.tuga;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Library {

    private static final Pattern NAME_PATTERN = Pattern
            .compile("(.+?)(?: (\\d+))?");

    private static final String _PROGRAM_PREFIX = "program_";

    static final Charset CHARSET = Charset.forName("UTF-8");

    private static final String GROUP_SUFFIX = "_group";

    private static final String MOST_RECENT_PROGRAM = "mostRecentProgram";

    private static final String NAME_SUFFIX = "_name";

    private static final String PROGRAM_COUNT = "programCount";

    private static final Pattern PROGRAM_PATTERN = Pattern.compile("("
            + _PROGRAM_PREFIX
            + "\\d+)(_.*)");

    private static final String PROGRAM_PREFIX = _PROGRAM_PREFIX;

    public static String readAll(InputStream input) {
        try {
            try (Reader reader = new InputStreamReader(input, CHARSET)) {
                StringBuilder buffer = new StringBuilder();
                char[] chars = new char[4096];
                int count;
                while ((count = reader.read(chars)) >= 0) {
                    buffer.append(chars, 0, count);
                }
                return buffer.toString();
            }
        } catch (Exception e) {
            throw Thrower.throwAny(e);
        }
    }

    private final File directory;

    private List<Program> examples;

    private final String firstContent;

    private final File infoFile;

    private Program mostRecentProgram;

    private final Map<String, Map<String, String>> programGroupToNameToId;

    private final Map<String, Program> programIdToProgram;

    private final File programsDirectory;

    private Properties properties;

    public Library(String dirName, List<Program> examples, String firstContent) {
        this.firstContent = firstContent;
        File userHome = new File(System.getProperty("user.home"));
        File dataDir = new File(userHome, "Application Data");
        if (dataDir.isDirectory()) {
            directory = new File(dataDir, dirName);
        } else {
            directory = new File(userHome, "." + dirName);
        }
        programsDirectory = new File(directory, "Programs");
        infoFile = new File(directory, "tuga.properties");
        programGroupToNameToId = new TreeMap<>();
        initGroup(ProgramGroup.EXAMPLES);
        initGroup(ProgramGroup.MY_PROGRAMS);
        initGroup(ProgramGroup.TRASH);
        programIdToProgram = new TreeMap<>();
        System.out.println(this.directory.getAbsolutePath());
        setExamples(examples);
        update();
    }

    public Map<String, String> getGroupPrograms(String group) {
        return programGroupToNameToId.get(group);
    }

    public Program getMostRecentProgram() {
        return mostRecentProgram;
    }

    public Program getProgram(String id) {
        Program program = programIdToProgram.get(id);
        if (program == null) {
            File file = new File(this.programsDirectory, id + ".rb");
            program = new Program();
            program.setContent(readAll(file));
            program.setId(id);
            program.setFile(file);
            program.setGroup(properties.getProperty(
                    id + GROUP_SUFFIX,
                    ProgramGroup.MY_PROGRAMS));
            program.setName(properties.getProperty(id + NAME_SUFFIX));
            programIdToProgram.put(id, program);
        }
        return program;
    }

    public Program getProgramByNameAndGroup(String group, String name) {
        Map<String, String> nameToId = getGroupPrograms(group);
        String id = nameToId == null ? null : nameToId.get(name);
        return id == null ? null : getProgram(id);
    }

    public Set<String> getProgramsIds(String group) {
        return new TreeSet<>(getGroupPrograms(group).values());
    }

    private void initGroup(String group) {
        programGroupToNameToId.put(group, new TreeMap<String, String>(new NameComparator()));
    }

    private void loadProperties() {
        try (FileInputStream in = new FileInputStream(infoFile)) {
            properties.load(in);
        } catch (Exception e) {
            Thrower.throwAny(e);
        }
    }

    private void mkdirs(File dir) {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("failed mkdirs for "
                        + dir.getAbsolutePath());
            }
        }
    }

    public Program newProgram() {
        try {
            int programCount = Integer.parseInt(properties.getProperty(
                    PROGRAM_COUNT,
                    "0"));
            Program program = new Program();
            program.setContent("");
            program.setGroup(ProgramGroup.MY_PROGRAMS);
            while (true) {
                programCount++;
                String id = PROGRAM_PREFIX + programCount;
                File file = new File(programsDirectory, id + ".rb");
                if (file.createNewFile()) {
                    program.setId(id);
                    program.setFile(file);
                    break;
                }
            }
            program.setName("Program " + programCount);
            properties.setProperty(PROGRAM_COUNT, String.valueOf(programCount));
            properties.setProperty(program.getId() + GROUP_SUFFIX, program.getGroup());
            properties.setProperty(program.getId() + NAME_SUFFIX, program.getName());
            storeAndUpdate();
            return program;
        } catch (Exception e) {
            throw Thrower.throwAny(e);
        }
    }

    private String readAll(File file) {
        try {
            InputStream input = new FileInputStream(file);
            return readAll(input);
        } catch (Exception e) {
            throw Thrower.throwAny(e);
        }
    }

    private void setExamples(List<Program> examples) {
        this.examples = examples;
        Map<String, String> examplesNameToId = getGroupPrograms(ProgramGroup.EXAMPLES);
        for (Program program : examples) {
            programIdToProgram.put(program.getId(), program);
            examplesNameToId.put(program.getName(), program.getId());
        }
    }

    public void setMostRecentProgram(Program program) {
        properties.setProperty(MOST_RECENT_PROGRAM, program.getId());
        storeAndUpdate();
    }

    public void setProgramName(String id, String name) {
        properties.setProperty(id + NAME_SUFFIX, name);
        storeAndUpdate();
    }

    private void storeAndUpdate() {
        storeProperties();
        update();
    }

    private void storeProperties() {
        try {
            FileOutputStream out = new FileOutputStream(infoFile);
            try {
                properties.store(out, "Tuga Turtle Info File");
            } finally {
                out.close();
            }
        } catch (Exception e) {
            Thrower.throwAny(e);
        }
    }

    private void update() {
        if (properties == null) {
            mkdirs(programsDirectory);
            properties = new Properties();
            if (infoFile.exists()) {
                loadProperties();
            }
            if (properties.containsKey(MOST_RECENT_PROGRAM)) {
                mostRecentProgram = getProgram((String) properties
                        .get(MOST_RECENT_PROGRAM));
            } else {
                Program program = newProgram();
                properties.setProperty(MOST_RECENT_PROGRAM, program.getId());
                mostRecentProgram = program;
                program.writeContent(firstContent);
                storeProperties();
            }
        }
        programIdToProgram.clear();
        for (Iterator<?> e = properties.entrySet().iterator(); e.hasNext();) {
            // TODO Should I instead be looping on the program files
            // themselves?
            Map.Entry<?, ?> entry = (Map.Entry) e.next();
            String key = (String) entry.getKey();
            Matcher matcher = PROGRAM_PATTERN.matcher(key);
            if (matcher.matches()) {
                String id = matcher.group(1);
                if (!programIdToProgram.containsKey(id)) {
                    getProgram(id);
                }
            }
        }
        for (Program example : examples) {
            programIdToProgram.put(example.getId(), example);
        }
        programGroupToNameToId.get(ProgramGroup.MY_PROGRAMS).clear();
        programGroupToNameToId.get(ProgramGroup.TRASH).clear();
        for (Program program : programIdToProgram.values()) {
            Map<String, String> group = programGroupToNameToId.get(program.getGroup());
            group.put(program.getName(), program.getId());
        }
    }

    public void updateGroup(Program program, String group) {
        String name = program.getName();
        if (getProgramByNameAndGroup(group, name) != null) {
            String newName = pickSimilarName(getGroupPrograms(group), name);
            program.setName(newName);
            setProgramName(program.getId(), newName);
        }
        program.setGroup(group);
        properties.setProperty(program.getId() + GROUP_SUFFIX, program.getGroup());
        storeAndUpdate();
    }

    /**
     * Renames the program. If another program in this group already has the
     * wantedName, it is given a number to make it unique.
     */
    public void rename(Program program, String wantedName) {
        Map<String, String> programs = getGroupPrograms(program.getGroup());
        String newName = pickSimilarName(programs, wantedName);
        setProgramName(program.getId(), newName);
    }

    private String pickSimilarName(Map<String, String> programs, String wantedName) {
        if (programs.containsKey(wantedName)) {
            Matcher matcher = nameMatcher(wantedName);
            String mainName = matcher.group(1);
            String numberString = matcher.group(2);
            BigInteger number = numberString != null ? new BigInteger(
                    numberString) : BigInteger.ONE;
            do {
                number = number.add(BigInteger.ONE);
                wantedName = mainName + " " + number;
            } while (programs.containsKey(wantedName));
        }
        return wantedName;
    }

    /**
     * A quick hack for sorting while understanding trailing numbers. Better
     * would be a more general solution as has been implemented by some people.
     */
    public static class NameComparator implements Comparator<String> {

        @Override
        public int compare(String name1, String name2) {
            Matcher matcher1 = nameMatcher(name1);
            Matcher matcher2 = nameMatcher(name2);
            if (matcher1.group(1).equals(matcher2.group(1))) {
                // Main names match. Check numbers.
                String number1 = matcher1.group(2);
                String number2 = matcher2.group(2);
                if (number1 != null && number2 != null) {
                    // Both are numbers, so compare them.F
                    return new BigInteger(number1)
                            .compareTo(new BigInteger(number2));
                }
            }
            return name1.compareTo(name2);
        }
    }

    private static Matcher nameMatcher(String name) {
        Matcher matcher = NAME_PATTERN.matcher(name);
        if (!matcher.matches()) {
            throw new RuntimeException("doesn't match???: " + name);
        }
        return matcher;
    }

}
