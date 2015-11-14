package bagotricks.tuga.turtle;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.util.List;
import java.util.regex.*;

import org.jruby.*;
import org.jruby.exceptions.*;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.*;
import org.jruby.runtime.callback.*;

import bagotricks.tuga.*;

public class TurtleEngine implements Engine {

	private static final Pattern RUBY_FRAME_PATTERN = Pattern.compile("([^:]*):(\\d*)(?::((?:in `([^']*)')|.*))?");

	private static double convertToDouble(IRubyObject doubleObject) {
		return doubleObject.convertToFloat().getDoubleValue();
	}

	@SuppressWarnings("unchecked")
	private static <C> List<C> convertToList(Class<C> clazz, IRubyObject arrayObject) {
		return arrayObject.convertToArray();
	}

	private final Callback color = new Callback() {

		public IRubyObject execute(IRubyObject recv, IRubyObject[] args) {
			if (args.length > 0) {
				float red, green, blue;
				if (args.length == 1) {
					// Should be an array of RGB.
					List<Number> numbers = convertToList(Number.class, args[0]);
					red = numbers.get(0).floatValue();
					green = numbers.get(1).floatValue();
					blue = numbers.get(2).floatValue();
				} else {
					red = (float)convertToDouble(args[0]);
					green = (float)convertToDouble(args[1]);
					blue = (float)convertToDouble(args[2]);
				}
				turtle.penColor = new Color(red / 100, green / 100, blue / 100);
			}
			float[] rgb = turtle.penColor.getRGBComponents(null);
			RubyFloat[] rubyRgb = new RubyFloat[3];
			for (int i = 0; i < 3; i++) {
				rubyRgb[i] = RubyFloat.newFloat(recv.getRuntime(), rgb[i]);
			}
			return RubyArray.newArray(recv.getRuntime(), rubyRgb);
		}

		public Arity getArity() {
			return Arity.singleArgument();
		}

	};

	private Drawing drawing = new Drawing();

	/**
	 * provides the ability to draw incrementally rather than redrawing the whole thing every time.
	 */
	private VolatileImage drawingBuffer;

	private int drawnStepCount;

	private final Callback jump = new Callback() {

		public IRubyObject execute(IRubyObject recv, IRubyObject[] args) {
			double distance = convertToDouble(args[0]);
			move(distance, false);
			return null;
		}

		public Arity getArity() {
			return Arity.singleArgument();
		}

	};

	public RunListener listener;

	public boolean paused;

	private final Callback pen = new Callback() {

		public IRubyObject execute(IRubyObject recv, IRubyObject[] args) {
			turtle.penDown = args[0].isTrue();
			return null;
		}

		public Arity getArity() {
			return Arity.singleArgument();
		}

	};

	private boolean stopNext;

	private final Callback turn = new Callback() {

		public IRubyObject execute(IRubyObject recv, IRubyObject[] args) {
			double angle = convertToDouble(args[0]);
			angle += turtle.angle;
			// double turns = angle / 360;
			// turns = turns < 0 ? Math.
			// TODO Finish normalizing angle.
			turtle.angle = angle;
			onStep();
			return null;
		}

		public Arity getArity() {
			return Arity.singleArgument();
		}

	};

	public Turtle turtle = new Turtle();

	private final Callback walk = new Callback() {

		public IRubyObject execute(IRubyObject recv, IRubyObject[] args) {
			double distance = convertToDouble(args[0]);
			move(distance, turtle.penDown);
			return null;
		}

		public Arity getArity() {
			return Arity.singleArgument();
		}

	};

	public TurtleEngine() {
		// Get things preloaded.
		new Thread() {
			public void run() {
				Ruby.getDefaultInstance().evalScript("");
			}
		}.start();
	}

	@SuppressWarnings("unchecked")
	private RuntimeException buildException(RaiseException e) {
		String message = e.getMessage();
		int lineNumber = -1;
		StringBuffer buffer = new StringBuffer();
		buffer.append("\n\nStack trace:\n");
		IRubyObject backtrace = e.getException().backtrace();
		for (String rubyFrame: convertToList(String.class, backtrace)) {
			buffer.append(rubyFrame);
			buffer.append("\n");
			Matcher matcher = RUBY_FRAME_PATTERN.matcher(rubyFrame);
			if (!matcher.matches()) {
				throw new RuntimeException("unparsed ruby stack frame: " + rubyFrame);
			}
			// TODO Check that it's from the right file.
			if (lineNumber == -1) {
				lineNumber = Integer.parseInt(matcher.group(2));
			}
		}
		if (lineNumber == -1) {
			Matcher matcher = RUBY_FRAME_PATTERN.matcher(e.getMessage());
			if (matcher.matches()) {
				lineNumber = Integer.parseInt(matcher.group(2));
			}
			message = matcher.group(3).trim();
			buffer.setLength(0);
		}
		RuntimeException exception = new RuntimeException("Error: " + message + (lineNumber >= 1 ? " on line " + lineNumber : "") + buffer.toString(), e);
		return exception;
	}

	public void execute(String name, String script) {
		reset();
		onStep();
		IRuby ruby = Ruby.getDefaultInstance();
		// ruby.setSafeLevel(4);
		initCommands(ruby);
		try {
			ruby.loadScript(name, new StringReader(script), false);
		} catch (RaiseException e) {
			if (e.getCause() != null) {
				Throwable cause = e;
				while (cause.getCause() != null) {
					cause = cause.getCause();
				}
				if (cause instanceof StopException) {
					// This was triggered on purpose. Expose it.
					throw (StopException)cause;
				}
			}
			System.err.println(e.getException().backtrace());
			System.err.println(e.getMessage());
			e.printStackTrace();
			throw buildException(e);
		} finally {
			paused = false;
			stopNext = false;
		}
	}

	private void initCommands(IRuby ruby) {
		RubyModule object = ruby.getObject();
		// Colors
		ruby.evalScript("def aqua; [0, 100, 100] end");
		ruby.evalScript("def black; [0, 0, 0] end");
		ruby.evalScript("def blue; [0, 0, 100] end");
		ruby.evalScript("def brown; [74, 56, 56] end"); // "saddlebrown" by CSS 3 specs - their brown is very red and dark.
		ruby.evalScript("def gray; [50, 50, 50] end");
		ruby.evalScript("def green; [0, 50, 0] end");
		ruby.evalScript("def fuschia; [100, 0, 100] end");
		ruby.evalScript("def lime; [0, 100, 0] end");
		ruby.evalScript("def maroon; [50, 0, 0] end");
		ruby.evalScript("def navy; [0, 0, 50] end");
		ruby.evalScript("def olive; [50, 50, 0] end");
		ruby.evalScript("def orange; [100, 65, 0] end");
		ruby.evalScript("def purple; [50, 0, 50] end");
		ruby.evalScript("def red; [100, 0, 0] end");
		ruby.evalScript("def silver; [75, 75, 75] end");
		ruby.evalScript("def tan; [82, 71, 55] end");
		ruby.evalScript("def teal; [0, 50, 50] end");
		ruby.evalScript("def white; [100, 100, 100] end");
		ruby.evalScript("def yellow; [100, 100, 0] end");
		// Directions
		ruby.evalScript("def around; 180 end");
		ruby.evalScript("def left; 90 end");
		ruby.evalScript("def right; -90 end");
		// Pen positions
		ruby.evalScript("def down; true end");
		ruby.evalScript("def up; false end");
		// Java methods
		object.defineMethod("color", color);
		object.defineMethod("jump", jump);
		object.defineMethod("pen", pen);
		object.defineMethod("turn", turn);
		object.defineMethod("walk", walk);
	}

	private void initDrawingBuffer(Component component) {
		try {
			int status = VolatileImage.IMAGE_OK;
			if (drawingBuffer == null || drawingBuffer.getWidth() != component.getWidth() || drawingBuffer.getHeight() != component.getHeight() || (status = drawingBuffer.validate(component.getGraphicsConfiguration())) != VolatileImage.IMAGE_OK) {
				if (drawingBuffer != null && status == VolatileImage.IMAGE_OK) {
					// We think we have a good image, but it's not the right size.
					drawingBuffer.flush();
				}
				if (status != VolatileImage.IMAGE_RESTORED) {
					drawingBuffer = component.createVolatileImage(component.getWidth(), component.getHeight(), new ImageCapabilities(true));
				}
				// In any case, start the image over again.
				drawnStepCount = 0;
				Graphics2D bufferGraphics = drawingBuffer.createGraphics();
				try {
					bufferGraphics.setColor(component.getBackground());
					bufferGraphics.fillRect(0, 0, component.getWidth(), component.getHeight());
				} finally {
					bufferGraphics.dispose();
				}
			}
		} catch (Exception e) {
			Thrower.throwAny(e);
		}
	}

	private void initGraphics(Graphics2D g, int width, int height) {
		g.translate(0.5 * width, 0.5 * height);
		double base = Math.min(width, height);
		g.scale(base / 1850, base / -1850);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	}

	private void move(double distance, boolean penDown) {
		synchronized (this) {
			turtle.x += distance * Math.cos(Math.toRadians(turtle.angle));
			turtle.y += distance * Math.sin(Math.toRadians(turtle.angle));
			drawing.lastPath().steps.add(new Step(turtle.x, turtle.y, penDown, turtle.penColor));
		}
		onStep();
	}

	private void onStep() {
		synchronized (this) {
			if (paused) {
				try {
					wait();
				} catch (InterruptedException e) {
					// Fine by me.
				}
			}
			if (stopNext) {
				stopNext = false;
				throw new StopException();
			}
		}
		if (listener != null) {
			listener.onStep();
		}
	}

	public void paintCanvas(Component component, Graphics graphics) {
		Graphics2D g = (Graphics2D)graphics.create();
		try {
			int width = component.getWidth();
			int height = component.getHeight();
			synchronized (this) {
				do {
					initDrawingBuffer(component);
					Graphics2D bufferGraphics = drawingBuffer.createGraphics();
					try {
						initGraphics(bufferGraphics, width, height);
						// TODO I don't really support multiple paths, so lose this idea?
						for (Path path: drawing.paths) {
							bufferGraphics.setColor(path.color);
							bufferGraphics.setStroke(new BasicStroke((float)path.width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
							for (; drawnStepCount < path.steps.size(); drawnStepCount++) {
								Step lastStep = drawnStepCount > 0 ? path.steps.get(drawnStepCount - 1) : null;
								Step step = path.steps.get(drawnStepCount);
								if (lastStep != null && step.penDown) {
									bufferGraphics.setColor(step.color);
									bufferGraphics.draw(new Line2D.Double(lastStep.x, lastStep.y, step.x, step.y));
								}
							}
						}
					} finally {
						bufferGraphics.dispose();
					}
					g.drawImage(drawingBuffer, 0, 0, null);
				} while (drawingBuffer.contentsLost());
				initGraphics(g, width, height);
				paintTurtle(g);
			}
		} finally {
			g.dispose();
		}
	}

	private void paintTurtle(Graphics2D g) {
		g = (Graphics2D)g.create();
		try {
			g.setColor(new Color(0, 128, 0));
			g.setStroke(new BasicStroke(9));
			g.translate(turtle.x, turtle.y);
			g.rotate(Math.toRadians(turtle.angle));
			g.translate(8, 0); // The turtle is a bit offset.
			GeneralPath path = new GeneralPath();
			path.moveTo(-20, -15);
			path.lineTo(-5, -15);
			path.lineTo(20, 0);
			path.lineTo(-5, 15);
			path.lineTo(-20, 15);
			path.closePath();
			g.draw(path);
		} finally {
			g.dispose();
		}
	}

	public void reset() {
		synchronized (this) {
			drawingBuffer.flush();
			drawingBuffer = null;
			turtle.reset();
			drawing.reset();
		}
	}

	public void setListener(RunListener listener) {
		this.listener = listener;
	}

	/**
	 * Stops the next thread that calls onStep.
	 */
	public void stop() {
		synchronized (this) {
			stopNext = true;
			paused = false;
			notify();
		}
	}

	public synchronized boolean togglePause() {
		if (paused) {
			paused = false;
			notify();
		} else {
			paused = true;
		}
		return paused;
	}

}
