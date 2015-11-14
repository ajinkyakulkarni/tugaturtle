package bagotricks.tuga.turtle;

import bagotricks.tuga.Engine;
import bagotricks.tuga.RunListener;
import bagotricks.tuga.StopException;
import bagotricks.tuga.Thrower;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.ImageCapabilities;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.image.VolatileImage;
import java.io.StringReader;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.builtin.IRubyObject;

public class TurtleEngine implements Engine {

    private static final Pattern RUBY_FRAME_PATTERN = Pattern.compile("([^:]*):(\\d*)(?::((?:in `([^']*)')|.*))?");

    private final Turtle turtle;

    private final Drawing drawing;

    /**
     * provides the ability to draw incrementally rather than redrawing the
     * whole thing every time.
     */
    private VolatileImage drawingBuffer;

    private int drawnStepCount;

    private RunListener listener;

    private boolean paused;

    private boolean stopNext;

    public float[] color(float[] val) {
        if (val.length == 3) {
            return color3(val[0], val[1], val[2]);
        } else {
            throw new RuntimeException("Expected array with length 3 but array had length " + val.length);
        }
    }

    public float[] color3(float red, float green, float blue) {
        turtle.penColor = new Color(red / 100, green / 100, blue / 100);
        return new float[]{red, green, blue};
    }

    public void jump(double distance) {
        move(distance, false);
    }

    public void pen(boolean down) {
        turtle.penDown = down;
    }

    public void turn(double angle) {
        angle += turtle.angle;
        // double turns = angle / 360;
        // turns = turns < 0 ? Math.
        // TODO Finish normalizing angle.
        turtle.angle = angle;
        onStep();
    }

    public void walk(double distance) {
        move(distance, turtle.penDown);
    }

    private final ScriptEngine engine;

    /**
     * Latch to ensure the JRuby engine is initialized before scripts can
     * execute.
     */
    private final CountDownLatch engineReadySignal;

    public TurtleEngine() {
        System.setProperty("org.jruby.embed.localvariable.behavior", "persistent");
        engineReadySignal = new CountDownLatch(1);
        this.drawing = new Drawing();
        this.turtle = new Turtle();
        ScriptEngineManager manager = new ScriptEngineManager();
        engine = manager.getEngineByName("jruby");
        // Initialize the engine in a separate thread to hide the startup time.
        new Thread() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                try {
                    createRubyApi();
                } catch (ScriptException ex) {
                }
                final long elapsed = System.currentTimeMillis() - start;
                System.out.println(String.format("JRuby initialized in %d ms", elapsed));
                engineReadySignal.countDown();
            }
        }.start();
    }

    private void createRubyApi() throws ScriptException {
        // Insert the TurtleEngine into JRuby.
        engine.put("$tuga", this);

        // Colors
        engine.eval("def aqua; [0, 100, 100] end");
        engine.eval("def black; [0, 0, 0] end");
        engine.eval("def blue; [0, 0, 100] end");
        engine.eval("def brown; [74, 56, 56] end"); // "saddlebrown" by CSS 3 specs - their brown is very red and dark.
        engine.eval("def gray; [50, 50, 50] end");
        engine.eval("def green; [0, 50, 0] end");
        engine.eval("def fuschia; [100, 0, 100] end");
        engine.eval("def lime; [0, 100, 0] end");
        engine.eval("def maroon; [50, 0, 0] end");
        engine.eval("def navy; [0, 0, 50] end");
        engine.eval("def olive; [50, 50, 0] end");
        engine.eval("def orange; [100, 65, 0] end");
        engine.eval("def purple; [50, 0, 50] end");
        engine.eval("def red; [100, 0, 0] end");
        engine.eval("def silver; [75, 75, 75] end");
        engine.eval("def tan; [82, 71, 55] end");
        engine.eval("def teal; [0, 50, 50] end");
        engine.eval("def white; [100, 100, 100] end");
        engine.eval("def yellow; [100, 100, 0] end");
        // Directions
        engine.eval("def around; 180 end");
        engine.eval("def left; 90 end");
        engine.eval("def right; -90 end");
        // Pen positions
        engine.eval("def down; true end");
        engine.eval("def up; false end");

        // Bind the Java operations to simple Ruby methods
        engine.eval("def color(val); $tuga.color(val) end");
        engine.eval("def color3(r,b,g); $tuga.color3(r,b,g) end");
        engine.eval("def jump(dist); $tuga.jump(dist) end");
        engine.eval("def pen(down); $tuga.pen(down) end");
        engine.eval("def turn(angle); $tuga.turn(angle) end");
        engine.eval("def walk(dist); $tuga.walk(dist) end");
    }

    @Override
    public void execute(String name, String script) {
        try {
            engineReadySignal.await();
        } catch (InterruptedException ex) {
        }
        reset();
        onStep();

        try {
            engine.eval(new StringReader(script));
        } catch (ScriptException ex) {
            if (ex.getCause() != null) {
                Throwable cause = ex;
                while (cause.getCause() != null) {
                    cause = cause.getCause();
                }
                if (cause instanceof StopException) {
                    // This was triggered on purpose. Expose it.
                    throw (StopException) cause;
                }
            }
            throw buildException(ex);
        } finally {
            paused = false;
            stopNext = false;
        }
    }

    @SuppressWarnings("unchecked")
    private RuntimeException buildException(ScriptException e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause instanceof RaiseException) {
            // This was triggered on purpose. Expose it.
            return buildException((RaiseException) cause);
        }
        return new RuntimeException(cause);
    }

    private RuntimeException buildException(RaiseException e) {
        String message = e.getMessage();
        int lineNumber = -1;
        StringBuilder buffer = new StringBuilder();
        buffer.append("\n\nStack trace:\n");
        IRubyObject backtrace = e.getException().backtrace();
        for (String rubyFrame : convertToList(String.class, backtrace)) {
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

    @Override
    public void paintCanvas(Component component, Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
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
                        for (Path path : drawing.paths) {
                            bufferGraphics.setColor(path.color);
                            bufferGraphics.setStroke(new BasicStroke((float) path.width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
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
        g = (Graphics2D) g.create();
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

    @Override
    public void reset() {
        synchronized (this) {
            if (drawingBuffer != null) {
                drawingBuffer.flush();
            }
            drawingBuffer = null;
            turtle.reset();
            drawing.reset();
        }
    }

    @Override
    public void setListener(RunListener listener) {
        this.listener = listener;
    }

    /**
     * Stops the next thread that calls onStep.
     */
    @Override
    public void stop() {
        synchronized (this) {
            stopNext = true;
            paused = false;
            notify();
        }
    }

    @Override
    public synchronized boolean togglePause() {
        if (paused) {
            paused = false;
            notify();
        } else {
            paused = true;
        }
        return paused;
    }

    public Drawing getDrawing() {
        return drawing;
    }

    /**
     * Converts a Ruby array to a Java List of type clazz.
     *
     * @param <C>
     * @param clazz
     * @param arrayObject
     * @return
     */
    @SuppressWarnings("unchecked")
    private static <C> List<C> convertToList(Class<C> clazz, IRubyObject arrayObject) {
        return arrayObject.convertToArray();
    }

}
