package bagotricks.tuga;

import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

public class TextEditor extends JTextArea {

	public TextEditor() {
		setTabSize(2);
		addKeyListener(new KeyListener() {

			public void keyPressed(KeyEvent event) {
				if (event.getKeyCode() == KeyEvent.VK_TAB) {
					if ((event.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) == 0) {
						indent();
					} else {
						dedent();
					}
					event.consume();
				}
			}

			public void keyReleased(KeyEvent event) {
				// TODO Auto-generated method stub
			}

			public void keyTyped(KeyEvent event) {
				// TODO Auto-generated method stub
			}

		});
		getDocument().addDocumentListener(new DocumentListener() {

			public void changedUpdate(DocumentEvent event) {
				// TODO Auto-generated method stub
			}

			public void insertUpdate(DocumentEvent event) {
				checkIndent(event.getOffset(), textAt(event.getOffset(), event.getLength()));
			}

			public void removeUpdate(DocumentEvent event) {
				// TODO Auto-generated method stub
			}

		});
	}

	protected void checkIndent(final int offset, final String text) {
		// Check for \r also?
		if (text.equals("\n")) {
			final StringBuilder indent = new StringBuilder();
			for (int i = offset - 1; i >= 0; i--) {
				char c = textAt(i, 1).charAt(0);
				if (c == '\r' || c == '\n') {
					break;
				} else if (Character.isWhitespace(c)) {
					indent.insert(0, c);
				} else {
					indent.setLength(0);
				}
			}
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					insert(indent.toString(), offset + text.length());
				}
			});
		}
	}

	private int countLeadingSpaces(String text) {
		int spaceCount = 0;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == ' ' || c == '\t') {
				spaceCount++;
			} else {
				break;
			}
		}
		return spaceCount;
	}

	protected void dedent() {
		int start = getSelectionStart();
		int end = getSelectionEnd();
		final List<Integer> lineStarts = findLineStarts(start, end);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				int pad = 0;
				for (int lineStart: lineStarts) {
					String text = textAt(lineStart - pad, 1);
					int spaceCount = countLeadingSpaces(text);
					replaceRange("", lineStart - pad, lineStart - pad + spaceCount);
					pad += spaceCount;
				}
			}
		});
	}

	private int findIndexOfLineStart(int seekStart) {
		try {
			return getLineStartOffset(getLineOfOffset(seekStart));
		} catch (Exception e) {
			throw Thrower.throwAny(e);
		}
	}

	private Integer findIndexOfNextLineStart(int seekStart) {
		try {
			int lineOfOffset = getLineOfOffset(seekStart);
			if (lineOfOffset < getLineCount() - 1) {
				return getLineStartOffset(lineOfOffset + 1);
			}
			return null;
		} catch (Exception e) {
			throw Thrower.throwAny(e);
		}
	}

	/**
	 * Indents lines for the current selection.
	 */
	public void indent() {
		final int start = getSelectionStart();
		final int end = getSelectionEnd();
		final List<Integer> lineStarts = findLineStarts(start, end);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				int pad = 0;
				for (int lineStart: lineStarts) {
					insert("\t", lineStart + pad);
					pad += 1;
				}
				if (start == (int)lineStarts.get(0) && start != end) {
					// Keep full line selected if at beginning.
					setSelectionStart(getSelectionStart() - 1);
				}
			}
		});
	}

	private List<Integer> findLineStarts(int start, int end) {
		final List<Integer> lineStarts = new ArrayList<Integer>();
		lineStarts.add(findIndexOfLineStart(start));
		Integer nextLineStart = start;
		while (true) {
			nextLineStart = findIndexOfNextLineStart(nextLineStart);
			if (nextLineStart == null || nextLineStart >= end) {
				break;
			}
			lineStarts.add(nextLineStart);
		}
		return lineStarts;
	}

	private String textAt(int offset, int length) {
		try {
			return getText(offset, length);
		} catch (Exception e) {
			throw Thrower.throwAny(e);
		}
	}

}
