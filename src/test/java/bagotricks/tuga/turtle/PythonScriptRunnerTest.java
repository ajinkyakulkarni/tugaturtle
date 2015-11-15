package bagotricks.tuga.turtle;

import org.junit.Test;
import org.mockito.InOrder;
import static org.mockito.Mockito.*;

public class PythonScriptRunnerTest {

    @Test(expected = RuntimeException.class)
    public void testBadScript() {
        String script
                = "asdf";
        Tuga tuga = mock(Tuga.class);
        run(script, tuga);
    }

    @Test
    public void testWalk() {
        String script
                = "walk(100)";
        Tuga tuga = mock(Tuga.class);
        run(script, tuga);

        verify(tuga).walk(100.0);
    }

    @Test
    public void testJump() {
        String script
                = "jump(100)";
        Tuga tuga = mock(Tuga.class);
        run(script, tuga);

        verify(tuga).jump(100.0);
    }

    @Test
    public void testTurnLeft() {
        String script
                = "turn(left)\n"
                + "walk(100)";
        Tuga tuga = mock(Tuga.class);
        run(script, tuga);

        InOrder inOrder = inOrder(tuga);

        inOrder.verify(tuga).turn(90.0);
        inOrder.verify(tuga).walk(100.0);
    }

    @Test
    public void testTurnRight() {
        String script
                = "turn(right)\n"
                + "walk(100)";
        Tuga tuga = mock(Tuga.class);
        run(script, tuga);

        InOrder inOrder = inOrder(tuga);

        inOrder.verify(tuga).turn(-90.0);
        inOrder.verify(tuga).walk(100.0);
    }

    @Test
    public void testTurnAround() {
        String script
                = "turn(around)\n"
                + "walk(100)";
        Tuga tuga = mock(Tuga.class);
        run(script, tuga);

        InOrder inOrder = inOrder(tuga);

        inOrder.verify(tuga).turn(180.0);
        inOrder.verify(tuga).walk(100.0);
    }

    @Test
    public void testPenUp() {
        String script
                = "pen(up)\n"
                + "walk(100)";
        Tuga tuga = mock(Tuga.class);
        run(script, tuga);

        InOrder inOrder = inOrder(tuga);

        inOrder.verify(tuga).pen(false);
        inOrder.verify(tuga).walk(100.0);
    }

    @Test
    public void testPenDown() {
        String script
                = "pen(up)\n"
                + "pen(down)\n"
                + "walk(100)";
        Tuga tuga = mock(Tuga.class);
        run(script, tuga);

        InOrder inOrder = inOrder(tuga);

        inOrder.verify(tuga).pen(false);
        inOrder.verify(tuga).pen(true);
        inOrder.verify(tuga).walk(100.0);
    }

    @Test
    public void testColor() {
        String script
                = "color(red)\n"
                + "walk(100)\n"
                + "color(blue)\n"
                + "walk(50)\n"
                + "color(silver)\n"
                + "walk(25)\n";
        Tuga tuga = mock(Tuga.class);
        run(script, tuga);

        InOrder inOrder = inOrder(tuga);

        inOrder.verify(tuga).color(Colors.rgb("red"));
        inOrder.verify(tuga).walk(100.0);
        inOrder.verify(tuga).color(Colors.rgb("blue"));
        inOrder.verify(tuga).walk(50.0);
        inOrder.verify(tuga).color(Colors.rgb("silver"));
        inOrder.verify(tuga).walk(25.0);
    }

    @Test
    public void testColor3() {
        String script
                = "color3(100, 0, 0)\n"
                + "walk(100)\n"
                + "color3(0, 0, 100)\n"
                + "walk(50)\n";
        Tuga tuga = mock(Tuga.class);
        run(script, tuga);

        InOrder inOrder = inOrder(tuga);

        NamedColor red = Colors.color("red");
        NamedColor blue = Colors.color("blue");
        inOrder.verify(tuga).color3(red.r, red.g, red.b);
        inOrder.verify(tuga).walk(100.0);
        inOrder.verify(tuga).color3(blue.r, blue.g, blue.b);
        inOrder.verify(tuga).walk(50.0);
    }

    private void run(String script, Tuga tuga) {
        ScriptRunner instance = new PythonScriptRunner();
        instance.init(tuga);
        instance.execute("", script);
    }
}
