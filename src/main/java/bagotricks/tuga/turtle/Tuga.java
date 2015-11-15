package bagotricks.tuga.turtle;

/**
 * Interface between the scripting language and the TurtleEngine.
 */
public interface Tuga {

    float[] color(float[] val);

    float[] color3(float red, float green, float blue);

    void jump(double distance);

    void pen(boolean down);

    void turn(double angle);

    void walk(double distance);

}
