package bagotricks.tuga;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

public class ProgramsUi {

    private ProgramsTab activeTab;

    private JDialog dialog;

    private boolean dialogBeenMoved;

    private boolean dialogBeenShown;

    private boolean examplesDone;

    private ProgramsTab examplesTab;

    private ProgramsTab myProgramsTab;

    private JTabbedPane tabbedPane;

    private final Map<String, ProgramsTab> tabs;

    private ProgramsTab trashTab;

    private final Controller controller;
    
    public ProgramsUi(JFrame owner, Controller controller) {
        this.controller = controller;
        tabs = new HashMap<>();
        createProgramsDialog(owner);
    }

    private void activateTab(ProgramsTab tab) {
        if (tab == activeTab) {
            return;
        }
        activeTab = tab;
        for (ProgramsTab otherTab : tabs.values()) {
            for (AbstractButton button : otherTab.buttons) {
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
            @Override
            public void valueChanged(ListSelectionEvent event) {
                Program selectedProgram = (Program) tab.listComponent.getSelectedValue();
                if (selectedProgram != null) {
                    if (selectedProgram != controller.getProgram()) {
                        controller.setProgram(selectedProgram);
                    }
                    activateTab(tab);
                }
            }
        });
        return tab;
    }

    private void createExamplesTab() {
        examplesTab = addProgramsTab("Examples", ProgramGroup.EXAMPLES);
        addPanelButton(examplesTab, BorderLayout.SOUTH, UI.createButton("Copy to My Programs", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                activeTab = null;
                controller.copyProgram();
            }
        }));
    }

    private void createMyProgramsTab() {
        myProgramsTab = addProgramsTab("My Programs", ProgramGroup.MY_PROGRAMS);
        JPanel buttonBar = new JPanel(new GridLayout(1, 4, 3, 3));
        addButton(myProgramsTab, buttonBar, UI.createButton("Rename", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                JOptionPane optionPane = new JOptionPane();
                optionPane.setMessageType(JOptionPane.QUESTION_MESSAGE);
                optionPane.setMessage("Choose a name for this program:");
                optionPane.setInitialSelectionValue(controller.getProgram().getName());
                optionPane.setWantsInput(true);
                optionPane.setOptions(new Object[]{"OK"});
                JDialog renameDialog = optionPane.createDialog(dialog, "Rename");
                renameDialog.setVisible(true);
                String name = (String) optionPane.getInputValue();
                if ("OK".equals(optionPane.getValue()) && name != null && !name.trim().equals("") && !name.trim().equals(controller.getProgram().getName())) {
                    controller.renameCurrent(name.trim());
                }
            }
        }));
        addButton(myProgramsTab, buttonBar, UI.createButton("New", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                controller.newProgram();
            }
        }));
        addButton(myProgramsTab, buttonBar, UI.createButton("Copy", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                controller.copyProgram();
            }
        }));
        addButton(myProgramsTab, buttonBar, UI.createButton("Delete", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                controller.deleteProgram();
            }
        }));
        myProgramsTab.panel.add(buttonBar, BorderLayout.SOUTH);
    }

    private void createProgramsDialog(JFrame owner) {
        dialog = new JDialog(owner, "Programs", false);
        dialog.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent event) {
                if (dialogBeenShown) {
                    dialogBeenMoved = true;
                }
            }

            @Override
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
        
        controller.addPropertyChangeListener("programList", new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateProgramLists();
            }
        });
    }

    private void createTrashTab() {
        trashTab = addProgramsTab("Trash", ProgramGroup.TRASH);
        addPanelButton(trashTab, BorderLayout.SOUTH, UI.createButton("Move Back to My Programs", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                controller.restoreProgram();
                activeTab = null;
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
            ProgramsTab tab = tabs.get(controller.getProgram().getGroup());
            tabbedPane.setSelectedComponent(tab.panel);
            activateTab(tab);
            tab.listComponent.requestFocus();
        }
        updateTabList(myProgramsTab);
        updateTabList(trashTab);
    }

    private void updateTabList(ProgramsTab tab) {
        tab.listModel.clear();
        List<String> names = new ArrayList<>(controller.getLibrary().getGroupPrograms(tab.group).keySet());
        for (String name : names) {
            tab.listModel.addElement(controller.getLibrary().getProgramByNameAndGroup(tab.group, name));
        }
        if (tab.group.equals(controller.getProgram().getGroup())) {
            tab.listComponent.setSelectedIndex(names.indexOf(controller.getProgram().getName()));
        }
    }

    public JDialog getDialog() {
        return dialog;
    }

    public boolean hasDialogBeenMoved() {
        return dialogBeenMoved;
    }
}
