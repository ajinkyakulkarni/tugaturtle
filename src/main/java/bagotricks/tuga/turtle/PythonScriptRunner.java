package bagotricks.tuga.turtle;

import bagotricks.tuga.StopException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.concurrent.CountDownLatch;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyFile;

public class PythonScriptRunner implements ScriptRunner {

    private final ScriptEngine engine;

    /**
     * Latch to ensure the engine is initialized before scripts can execute.
     */
    private final CountDownLatch engineReadySignal;

    public PythonScriptRunner() {
        engineReadySignal = new CountDownLatch(1);
        ScriptEngineManager manager = new ScriptEngineManager();
        engine = manager.getEngineByName("jython");
        if (engine == null) {
            throw new Error("Jython not found");
        }
    }

    @Override
    public void init(final Tuga tuga) {
        // Initialize the engine in a separate thread to hide the startup time.
        new Thread() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                try {
                    createApi(tuga);
                } catch (ScriptException ex) {
                }
                final long elapsed = System.currentTimeMillis() - start;
                System.out.println(String.format("Jython initialized in %d ms", elapsed));
                engineReadySignal.countDown();
            }
        }.start();
    }

    private void createApi(Tuga tuga) throws ScriptException {
        // Insert Tuga into JRuby.
        engine.put("tuga", tuga);
        engine.put("blue", new float[]{0, 0, 100});

        // Colors
        for (NamedColor color : Colors.COLORS) {
            engine.put(color.name, color.rgb());
        }

        // Directions
        engine.eval("around = 180");
        engine.eval("left = 90.0");
        engine.eval("right = -90");
        // Pen positions
        engine.eval("down = True");
        engine.eval("up = False");

        // Bind the Java operations to simple Python methods
        engine.eval("def color(val): tuga.color(val)");
        engine.eval("def color3(r,b,g): tuga.color3(r,b,g)");
        engine.eval("def jump(dist): tuga.jump(dist)");
        engine.eval("def pen(down): tuga.pen(down)");
        engine.eval("def turn(angle): tuga.turn(angle)");
        engine.eval("def walk(dist): tuga.walk(dist)");
    }

    @Override
    public void execute(String name, String script) {
        try {
            engineReadySignal.await();
        } catch (InterruptedException ex) {
        }

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
        }
    }

    private RuntimeException buildException(ScriptException e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause instanceof PyException) {
            // This was triggered on purpose. Expose it.
            return buildException((PyException) cause);
        }
        return new RuntimeException(cause);
    }

    private RuntimeException buildException(PyException e) {
        // TODO more parsing of the exception to get user friendly information.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Py.displayException(e.type, e.value, e.traceback, new PyFile(new PrintStream(out)));
        RuntimeException exception = new RuntimeException("Error: " + out.toString(), e);
        return exception;
    }

}
