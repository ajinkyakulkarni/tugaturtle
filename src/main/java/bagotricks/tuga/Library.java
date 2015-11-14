package bagotricks.tuga;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
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
			Reader reader = new InputStreamReader(input, CHARSET);
			try {
				StringBuffer buffer = new StringBuffer();
				char[] chars = new char[4096];
				int count;
				while ((count = reader.read(chars)) >= 0) {
					buffer.append(chars, 0, count);
				}
				return buffer.toString();
			} finally {
				reader.close();
			}
		} catch (Exception e) {
			throw Thrower.throwAny(e);
		}
	}

	private File directory;

	private List<Program> examples;

	private String firstContent;

	private File infoFile;

	private Program mostRecentProgram;

	private Map<String, Map<String, String>> programGroupToNameToId;

	private Map<String, Program> programIdToProgram;

	private File programsDirectory;

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
		programGroupToNameToId = new TreeMap<String, Map<String, String>>();
		initGroup(ProgramGroup.EXAMPLES);
		initGroup(ProgramGroup.MY_PROGRAMS);
		initGroup(ProgramGroup.TRASH);
		programIdToProgram = new TreeMap<String, Program>();
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
			program.content = readAll(file);
			program.id = id;
			program.file = file;
			program.group = properties.getProperty(
					id + GROUP_SUFFIX,
					ProgramGroup.MY_PROGRAMS);
			program.library = this;
			program.name = properties.getProperty(id + NAME_SUFFIX);
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
		return new TreeSet<String>(getGroupPrograms(group).values());
	}

	private void initGroup(String group) {
		programGroupToNameToId.put(group, new TreeMap<String, String>(new Comparator<String>() {
			public int compare(String name1, String name2) {
				return Program.compareNames(name1, name2);
			}
		}));
	}

	private void loadProperties() {
		try {
			FileInputStream in = new FileInputStream(infoFile);
			try {
				properties.load(in);
			} finally {
				in.close();
			}
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
			program.content = "";
			program.group = ProgramGroup.MY_PROGRAMS;
			program.library = this;
			while (true) {
				programCount++;
				program.id = PROGRAM_PREFIX + programCount;
				program.file = new File(programsDirectory, program.id + ".rb");
				if (program.file.createNewFile()) break;
			}
			program.name = "Program " + programCount;
			properties.setProperty(PROGRAM_COUNT, String.valueOf(programCount));
			properties.setProperty(program.id + GROUP_SUFFIX, program.group);
			properties.setProperty(program.id + NAME_SUFFIX, program.name);
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
		for (Program program: examples) {
			programIdToProgram.put(program.id, program);
			examplesNameToId.put(program.name, program.id);
		}
	}

	public void setMostRecentProgram(Program program) {
		properties.setProperty(MOST_RECENT_PROGRAM, program.id);
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
				mostRecentProgram = getProgram((String)properties
						.get(MOST_RECENT_PROGRAM));
			} else {
				Program program = newProgram();
				properties.setProperty(MOST_RECENT_PROGRAM, program.id);
				mostRecentProgram = program;
				program.setContent(firstContent);
				storeProperties();
			}
		}
		programIdToProgram.clear();
		for (Iterator<?> e = properties.entrySet().iterator(); e.hasNext();) {
			// TODO Should I instead be looping on the program files
			// themselves?
			Map.Entry<?, ?> entry = (Map.Entry)e.next();
			String key = (String)entry.getKey();
			Matcher matcher = PROGRAM_PATTERN.matcher(key);
			if (matcher.matches()) {
				String id = matcher.group(1);
				if (!programIdToProgram.containsKey(id)) {
					getProgram(id);
				}
			}
		}
		for (Program example: examples) {
			programIdToProgram.put(example.id, example);
		}
		programGroupToNameToId.get(ProgramGroup.MY_PROGRAMS).clear();
		programGroupToNameToId.get(ProgramGroup.TRASH).clear();
		for (Program program: programIdToProgram.values()) {
			Map<String, String> group = programGroupToNameToId.get(program.group);
			group.put(program.name, program.id);
		}
	}

	void updateGroup(Program program) {
		properties.setProperty(program.id + GROUP_SUFFIX, program.group);
		storeAndUpdate();
	}

}
