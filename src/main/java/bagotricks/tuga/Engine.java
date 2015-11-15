package bagotricks.tuga;

import java.awt.Component;
import java.awt.Graphics;

public interface Engine {

    void init();

    void execute(String name, String script);

    void paintCanvas(Component component, Graphics graphics);

    void reset();

    void setListener(RunListener listener);

    void stop();

    boolean togglePause();
}
