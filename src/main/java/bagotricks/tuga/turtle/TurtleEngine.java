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

public class TurtleEngine implements Engine, Tuga {

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

    private final ScriptRunner runner;

    public TurtleEngine() {
        this.drawing = new Drawing();
        this.turtle = new Turtle();
        runner = new RubyScriptRunner();
    }

    @Override
    public void init() {
        runner.init(this);
    }

    @Override
    public void execute(String name, String script) {
        reset();
        onStep();

        try {
            runner.execute(name, script);
        } finally {
            paused = false;
            stopNext = false;
        }
    }

    @Override
    public float[] color(float[] val) {
        if (val.length == 3) {
            return color3(val[0], val[1], val[2]);
        } else {
            throw new RuntimeException("Expected array with length 3 but array had length " + val.length);
        }
    }

    @Override
    public float[] color3(float red, float green, float blue) {
        turtle.penColor = new Color(red / 100, green / 100, blue / 100);
        return new float[]{red, green, blue};
    }

    @Override
    public void jump(double distance) {
        move(distance, false);
    }

    @Override
    public void pen(boolean down) {
        turtle.penDown = down;
    }

    @Override
    public void turn(double angle) {
        angle += turtle.angle;
        // double turns = angle / 360;
        // turns = turns < 0 ? Math.
        // TODO Finish normalizing angle.
        turtle.angle = angle;
        onStep();
    }

    @Override
    public void walk(double distance) {
        move(distance, turtle.penDown);
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

}
