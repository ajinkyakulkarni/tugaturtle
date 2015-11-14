package bagotricks.tuga;

import java.awt.Component;
import java.awt.Graphics;

public interface Engine {

	public abstract void execute(String name, String script);

	public abstract void paintCanvas(Component component, Graphics graphics);

	public abstract void reset();

	public abstract void setListener(RunListener listener);

	public abstract void stop();

	public abstract boolean togglePause();

}