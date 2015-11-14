package bagotricks.tuga;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class MainUi implements RunListener, Runnable {

	private static final String GO_TEXT = "Go";

	private JComponent canvas;

	public String firstContent;

	private boolean doReset;

	public Engine engine;

	public List<Program> examples;

	JFrame frame;

	private JButton goButton;

	public BufferedImage icon;

	public String id;

	private boolean ignoreUpdate;

	Program program;

	private JLabel programLabel;

	private ProgramsUi programsUi;

	private boolean running;

	private JTextArea textArea;

	public String title;

	JButton createButton(String text, ActionListener listener) {
		JButton button = new JButton(text);
		button.addActionListener(listener);
		return button;
	}

	private Component createCanvasArea() {
		canvas = new JPanel() {

			private static final long serialVersionUID = 1815382581283574623L;

			protected void paintComponent(Graphics graphics) {
				engine.paintCanvas(this, graphics);
			}

		};
		canvas.setBackground(Color.WHITE);
		canvas.setBorder(BorderFactory.createLoweredBevelBorder());
		canvas.setMinimumSize(new Dimension(450, 450));
		canvas.setPreferredSize(new Dimension(450, 450));
		return canvas;
	}

	private Component createDevArea() {
		BorderLayout devAreaLayout = new BorderLayout();
		devAreaLayout.setVgap(3);
		JPanel devArea = new JPanel(devAreaLayout);
		JPanel toolBar = new JPanel(new GridLayout(1, 2, 3, 3));
		toolBar.add(createButton("Reset", new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				synchronized (this) {
					if (running) {
						doReset = true;
						engine.stop();
						return;
					}
				}
				engine.reset();
				canvas.repaint();
			}
		}));
		toolBar.add(goButton = createButton(GO_TEXT, new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				runProgram();
			}
		}));
		devArea.add(toolBar, BorderLayout.NORTH);
		textArea = new TextEditor();
		textArea.getDocument().addDocumentListener(new DocumentListener() {

			public void changedUpdate(DocumentEvent event) {
				// TODO Who cares.
			}

			public void insertUpdate(DocumentEvent event) {
				try {
					if (ignoreUpdate)
						return;
					String text = textArea.getText(event.getOffset(), event.getLength());
					program.content = textArea.getText(); // TODO Remove line
					program.insertText(event.getOffset(), text);
				} catch (Exception e) {
					Thrower.throwAny(e);
				}
			}

			public void removeUpdate(DocumentEvent event) {
				try {
					if (ignoreUpdate)
						return;
					program.content = textArea.getText(); // TODO Remove line
					program.removeText(event.getOffset(), event.getLength());
				} catch (Exception e) {
					Thrower.throwAny(e);
				}
			}

		});
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setMinimumSize(new Dimension(200, 300));
		scrollPane.setPreferredSize(new Dimension(200, 300));
		devArea.add(scrollPane, BorderLayout.CENTER);
		return devArea;
	}

	private Component createTopBar() {
		JPanel topBar = new JPanel(new BorderLayout());
		topBar.setBackground(new Color(0, 128, 0));
		topBar.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		programLabel = new JLabel();
		programLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
		programLabel.setForeground(Color.WHITE);
		programLabel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		topBar.add(programLabel, BorderLayout.CENTER);
		JPanel toolBar = new JPanel();
		toolBar.setOpaque(false);
		FlowLayout toolBarLayout = new FlowLayout();
		toolBarLayout.setHgap(0);
		toolBarLayout.setVgap(0);
		toolBar.setLayout(toolBarLayout);
		programsUi = new ProgramsUi(this);
		final JDialog programsDialog = programsUi.dialog;
		final JButton programsButton = new JButton("Programs...");
		programsButton.setOpaque(false);
		programsButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if (programsDialog.isVisible()) {
					programsDialog.setVisible(false);
					return;
				}
				if (!programsUi.dialogBeenMoved) {
					int right = programsButton.getX() + programsButton.getWidth();
					int left = right - programsDialog.getWidth();
					int buttonBottom = programsButton.getY() + programsButton.getHeight();
					int top = buttonBottom + 3;
					Point point = new Point(left, top);
					SwingUtilities.convertPointToScreen(point, programsButton);
					programsDialog.setLocation(point);
				}
				programsDialog.setVisible(true);
			}
		});
		toolBar.add(programsButton);
		topBar.add(toolBar, BorderLayout.EAST);
		return topBar;
	}

	public void onStep() {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					canvas.repaint();
				}
			});
			Thread.sleep(20);
		} catch (Exception e) {
			Thrower.throwAny(e);
		}
	}

	public void run() {
		engine.setListener(this);
		frame = new JFrame(title);
		if (icon != null)
			frame.setIconImage(icon);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		BorderLayout contentLayout = new BorderLayout();
		contentLayout.setHgap(3);
		contentLayout.setVgap(3);
		JPanel contentPane = new JPanel(contentLayout);
		contentPane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		contentPane.add(createTopBar(), BorderLayout.NORTH);
		JSplitPane splitPane = new JSplitPane();
		splitPane.setBorder(null);
		splitPane.setLeftComponent(createDevArea());
		splitPane.setRightComponent(createCanvasArea());
		contentPane.add(splitPane, BorderLayout.CENTER);
		frame.setContentPane(contentPane);
		frame.pack();
		program = programsUi.library.getMostRecentProgram();
		programsUi.updateProgramLists();
		updateProgramContent();
		textArea.requestFocus();
		frame.setVisible(true);
	}

	private void runProgram() {
		synchronized (this) {
			if (running) {
				goButton.setText(engine.togglePause() ? "Continue" : "Pause");
				return;
			} else {
				goButton.setText("Pause");
			}
			running = true;
		}
		new Thread() {
			public void run() {
				Exception failure = null;
				try {
					engine.execute("Current Program", textArea.getText());
				} catch (StopException e) {
					// Requested by the user. Just ignore it.
				} catch (Exception e) {
					failure = e;
				} finally {
					synchronized (this) {
						running = false;
						if (doReset) {
							engine.reset();
							doReset = false;
						}
					}
					final Exception finalFailure = failure;
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							goButton.setText(GO_TEXT);
							canvas.repaint();
							if (finalFailure != null) {
								JOptionPane.showMessageDialog(frame, finalFailure.getMessage(), "Program Error", JOptionPane.ERROR_MESSAGE);
							}
						}
					});
				}
			}
		}.start();
	}

	void setProgram(Program program) {
		this.program = program;
		programsUi.library.setMostRecentProgram(program);
		updateProgramLabel();
		updateProgramContent();
	}

	private void updateProgramContent() {
		ignoreUpdate = true;
		try {
			textArea.setText(program.content);
			updateProgramStatus();
		} finally {
			ignoreUpdate = false;
		}
	}

	void updateProgramLabel() {
		programLabel.setText(program.name);
	}

	void updateProgramStatus() {
		textArea.setEditable(program.group.equals(ProgramGroup.MY_PROGRAMS));
		textArea.setForeground(textArea.isEditable() ? SystemColor.textText : SystemColor.textInactiveText);
	}

}
