package bagotricks.tuga;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

public class ProgramsUi {

	private ProgramsTab activeTab;

	JDialog dialog;

	boolean dialogBeenMoved;

	private boolean dialogBeenShown;

	private boolean examplesDone;

	private ProgramsTab examplesTab;

	Library library;

	private MainUi main;

	private ProgramsTab myProgramsTab;

	private JTabbedPane tabbedPane;

	private Map<String, ProgramsTab> tabs;

	private ProgramsTab trashTab;

	public ProgramsUi(MainUi main) {
		this.main = main;
		library = new Library(main.id, main.examples, main.firstContent);
		tabs = new HashMap<String, ProgramsTab>();
		createProgramsDialog();
	}

	private void activateTab(ProgramsTab tab) {
		if (tab == activeTab)
			return;
		activeTab = tab;
		for (ProgramsTab otherTab: tabs.values()) {
			for (AbstractButton button: otherTab.buttons) {
				button.setEnabled(tab == otherTab);
			}
			if (tab != otherTab) {
				otherTab.listComponent.clearSelection();
			}
		}
	}

	private void addButton(ProgramsTab tab, JPanel panel, JButton button) {
		panel.add(button);
		tab.buttons.add(button);
	}

	private void addPanelButton(ProgramsTab tab, Object constraint, JButton button) {
		tab.panel.add(button, constraint);
		tab.buttons.add(button);
	}

	private ProgramsTab addProgramsTab(String text, String group) {
		final ProgramsTab tab = new ProgramsTab();
		tabs.put(group, tab);
		tab.group = group;
		tab.listModel = new DefaultListModel();
		tab.listComponent = new JList(tab.listModel);
		tab.listComponent.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tab.listComponent.setMinimumSize(new Dimension(200, 300));
		tab.panel = new JPanel(new BorderLayout());
		tab.panel.add(new JScrollPane(tab.listComponent), BorderLayout.CENTER);
		tabbedPane.addTab(text, tab.panel);
		tab.listComponent.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent event) {
				Program selectedProgram = (Program)tab.listComponent.getSelectedValue();
				if (selectedProgram != null) {
					if (selectedProgram != main.program) {
						main.setProgram(selectedProgram);
					}
					activateTab(tab);
				}
			}
		});
		return tab;
	}

	private void copyProgram() {
		Program program = library.newProgram();
		program.renameTo(main.program.name);
		program.setContent(main.program.content);
		main.setProgram(program);
		updateProgramLists();
	}

	private void createExamplesTab() {
		examplesTab = addProgramsTab("Examples", ProgramGroup.EXAMPLES);
		addPanelButton(examplesTab, BorderLayout.SOUTH, main.createButton("Copy to My Programs", new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				activeTab = null;
				copyProgram();
			}
		}));
	}

	private void createMyProgramsTab() {
		myProgramsTab = addProgramsTab("My Programs", ProgramGroup.MY_PROGRAMS);
		JPanel buttonBar = new JPanel(new GridLayout(1, 4, 3, 3));
		addButton(myProgramsTab, buttonBar, main.createButton("Rename", new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				JOptionPane optionPane = new JOptionPane();
				optionPane.setMessageType(JOptionPane.QUESTION_MESSAGE);
				optionPane.setMessage("Choose a name for this program:");
				optionPane.setInitialSelectionValue(main.program.name);
				optionPane.setWantsInput(true);
				optionPane.setOptions(new Object[] {"OK"});
				JDialog renameDialog = optionPane.createDialog(dialog, "Rename");
				renameDialog.setVisible(true);
				String name = (String)optionPane.getInputValue();
				if ("OK".equals(optionPane.getValue()) && name != null && !name.trim().equals("") && !name.trim().equals(main.program.name)) {
					main.program.renameTo(name.trim());
					main.updateProgramLabel();
					updateProgramLists();
				}
			}
		}));
		addButton(myProgramsTab, buttonBar, main.createButton("New", new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				main.setProgram(library.newProgram());
				updateProgramLists();
			}
		}));
		addButton(myProgramsTab, buttonBar, main.createButton("Copy", new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				copyProgram();
			}
		}));
		addButton(myProgramsTab, buttonBar, main.createButton("Delete", new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				int index = myProgramsTab.listModel.indexOf(main.program);
				if (index == myProgramsTab.listModel.getSize() - 1) {
					// It was the last program, so select the previous
					// instead of the next.
					index--;
				}
				main.program.setGroup(ProgramGroup.TRASH);
				updateProgramLists();
				Program nextProgram;
				if (index < 0) {
					// No programs left, so make a new one.
					nextProgram = library.newProgram();
				} else {
					nextProgram = (Program)myProgramsTab.listModel.elementAt(index);
				}
				main.setProgram(nextProgram);
				updateProgramLists();
			}
		}));
		myProgramsTab.panel.add(buttonBar, BorderLayout.SOUTH);
	}

	private void createProgramsDialog() {
		dialog = new JDialog(main.frame, "Programs", false);
		dialog.addComponentListener(new ComponentAdapter() {
			public void componentMoved(ComponentEvent event) {
				if (dialogBeenShown) {
					dialogBeenMoved = true;
				}
			}

			public void componentShown(ComponentEvent event) {
				dialogBeenShown = true;
			}
		});
		JPanel contentPane = new JPanel(new BorderLayout());
		tabbedPane = new JTabbedPane();
		createMyProgramsTab();
		createExamplesTab();
		createTrashTab();
		contentPane.add(tabbedPane, BorderLayout.CENTER);
		dialog.setContentPane(contentPane);
		dialog.pack();
	}

	private void createTrashTab() {
		trashTab = addProgramsTab("Trash", ProgramGroup.TRASH);
		addPanelButton(trashTab, BorderLayout.SOUTH, main.createButton("Move Back to My Programs", new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				main.program.setGroup(ProgramGroup.MY_PROGRAMS);
				activeTab = null;
				updateProgramLists();
				main.updateProgramLabel();
				main.updateProgramStatus();
			}
		}));
	}

	void updateProgramLists() {
		if (!examplesDone) {
			examplesDone = true;
			updateTabList(examplesTab);
			activeTab = null;
		}
		if (activeTab == null) {
			// First time through.
			ProgramsTab tab = tabs.get(main.program.group);
			tabbedPane.setSelectedComponent(tab.panel);
			activateTab(tab);
			tab.listComponent.requestFocus();
		}
		updateTabList(myProgramsTab);
		updateTabList(trashTab);
	}

	private void updateTabList(ProgramsTab tab) {
		tab.listModel.clear();
		List<String> names = new ArrayList<String>(library.getGroupPrograms(tab.group).keySet());
		for (String name: names) {
			tab.listModel.addElement(library.getProgramByNameAndGroup(tab.group, name));
		}
		if (tab.group.equals(main.program.group)) {
			tab.listComponent.setSelectedIndex(names.indexOf(main.program.name));
		}
	}

}
