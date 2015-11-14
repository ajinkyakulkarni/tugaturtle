package bagotricks.tuga.turtle;

import java.awt.Color;
import org.junit.Test;
import static org.junit.Assert.*;

public class TurtleEngineTest {

    private static final double DELTA = 0.01;

    @Test
    public void testWalk() {
        String script
                = "walk 100";
        Drawing drawing = run(script);
        Drawing expected = new Drawing();
        expected.lastPath().steps.add(new Step(0.0, 100.0, true, Color.black));

        assertDrawingEquals(expected, drawing);
    }

    @Test
    public void testJump() {
        String script
                = "jump 100";
        Drawing drawing = run(script);
        Drawing expected = new Drawing();
        expected.lastPath().steps.add(new Step(0.0, 100.0, false, Color.black));

        assertDrawingEquals(expected, drawing);
    }

    @Test
    public void testTurnLeft() {
        String script
                = "turn left\n"
                + "walk 100";
        Drawing drawing = run(script);
        Drawing expected = new Drawing();
        expected.lastPath().steps.add(new Step(-100.0, 0.0, true, Color.black));

        assertDrawingEquals(expected, drawing);
    }

    @Test
    public void testTurnRight() {
        String script
                = "turn right\n"
                + "walk 100";
        Drawing drawing = run(script);
        Drawing expected = new Drawing();
        expected.lastPath().steps.add(new Step(100.0, 0.0, true, Color.black));

        assertDrawingEquals(expected, drawing);
    }

    @Test
    public void testTurnAround() {
        String script
                = "turn around\n"
                + "walk 100";
        Drawing drawing = run(script);
        Drawing expected = new Drawing();
        expected.lastPath().steps.add(new Step(0.0, -100.0, true, Color.black));

        assertDrawingEquals(expected, drawing);
    }

    @Test
    public void testPenUp() {
        String script
                = "pen up\n"
                + "walk 100";
        Drawing drawing = run(script);
        Drawing expected = new Drawing();
        expected.lastPath().steps.add(new Step(0.0, 100.0, false, Color.black));

        assertDrawingEquals(expected, drawing);
    }

    @Test
    public void testPenDown() {
        String script
                = "pen up\n"
                + "pen down\n"
                + "walk 100";
        Drawing drawing = run(script);
        Drawing expected = new Drawing();
        expected.lastPath().steps.add(new Step(0.0, 100.0, true, Color.black));

        assertDrawingEquals(expected, drawing);
    }

    @Test
    public void testColor() {
        String script
                = "color red\n"
                + "walk 100\n"
                + "color blue\n"
                + "walk 100";
        Drawing drawing = run(script);
        Drawing expected = new Drawing();
        expected.lastPath().steps.add(new Step(0.0, 100.0, true, Color.red));
        expected.lastPath().steps.add(new Step(0.0, 200.0, true, Color.blue));

        assertDrawingEquals(expected, drawing);
    }

    private Drawing run(String script) {
        TurtleEngine instance = new TurtleEngine();
        instance.execute("", script);
        Drawing drawing = instance.getDrawing();
        return drawing;
    }

    private void assertDrawingEquals(Drawing expected, Drawing actual) {
        assertEquals(expected.paths.size(), actual.paths.size());

        for (int i = 0; i < expected.paths.size(); i++) {
            Path expectedPath = expected.paths.get(i);
            Path actualPath = actual.paths.get(i);
            assertPathEquals(expectedPath, actualPath);
        }
    }

    private void assertPathEquals(Path expected, Path actual) {
        assertEquals(expected.color, actual.color);
        assertEquals(expected.width, actual.width, DELTA);
        assertEquals(expected.steps.size(), actual.steps.size());

        for (int i = 0; i < expected.steps.size(); i++) {
            Step expectedStep = expected.steps.get(i);
            Step actualStep = actual.steps.get(i);
            assertStepEquals(expectedStep, actualStep);
        }
    }

    private void assertStepEquals(Step expected, Step actual) {
        assertEquals(expected.color, actual.color);
        assertEquals(expected.penDown, actual.penDown);
        assertEquals(expected.x, actual.x, DELTA);
        assertEquals(expected.y, actual.y, DELTA);
    }
}
