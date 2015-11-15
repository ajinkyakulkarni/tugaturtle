package bagotricks.tuga.turtle;

import bagotricks.tuga.StopException;
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

public class RubyScriptRunner implements ScriptRunner {

    private static final Pattern RUBY_FRAME_PATTERN = Pattern.compile("([^:]*):(\\d*)(?::((?:in `([^']*)')|.*))?");

    private final ScriptEngine engine;

    /**
     * Latch to ensure the JRuby engine is initialized before scripts can
     * execute.
     */
    private final CountDownLatch engineReadySignal;

    public RubyScriptRunner() {
        System.setProperty("org.jruby.embed.localvariable.behavior", "persistent");
        engineReadySignal = new CountDownLatch(1);
        ScriptEngineManager manager = new ScriptEngineManager();
        engine = manager.getEngineByName("jruby");
    }

    @Override
    public void init(final Tuga tuga) {
        // Initialize the engine in a separate thread to hide the startup time.
        new Thread() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                try {
                    createRubyApi(tuga);
                } catch (ScriptException ex) {
                }
                final long elapsed = System.currentTimeMillis() - start;
                System.out.println(String.format("JRuby initialized in %d ms", elapsed));
                engineReadySignal.countDown();
            }
        }.start();
    }

    private void createRubyApi(Tuga tuga) throws ScriptException {
        // Insert Tuga into JRuby.
        engine.put("$tuga", tuga);

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
