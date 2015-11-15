package bagotricks.tuga.turtle;

import java.util.HashMap;
import java.util.Map;

public class Colors {

    public static final NamedColor[] COLORS = new NamedColor[]{
        new NamedColor("aqua", 0, 100, 100),
        new NamedColor("black", 0, 0, 0),
        new NamedColor("blue", 0, 0, 100),
        new NamedColor("brown", 74, 56, 56), // "saddlebrown" by CSS 3 specs - their brown is very red and dark.
        new NamedColor("gray", 50, 50, 50),
        new NamedColor("green", 0, 50, 0),
        new NamedColor("fuschia", 100, 0, 100),
        new NamedColor("lime", 0, 100, 0),
        new NamedColor("maroon", 50, 0, 0),
        new NamedColor("navy", 0, 0, 50),
        new NamedColor("olive", 50, 50, 0),
        new NamedColor("orange", 100, 65, 0),
        new NamedColor("purple", 50, 0, 50),
        new NamedColor("red", 100, 0, 0),
        new NamedColor("silver", 75, 75, 75),
        new NamedColor("tan", 82, 71, 55),
        new NamedColor("teal", 0, 50, 50),
        new NamedColor("white", 100, 100, 100),
        new NamedColor("yellow", 100, 100, 0)
    };

    public static final Map<String, NamedColor> INDEX = new HashMap<>();

    static {
        for (NamedColor color : Colors.COLORS) {
            INDEX.put(color.name, color);
        }
    }
    
    public static NamedColor color(String name) {
        return INDEX.get(name);
    }

    public static float[] rgb(String name) {
        NamedColor color = INDEX.get(name);
        if (color == null) {
            return new float[]{0f, 0f, 0f};
        }
        return color.rgb();
    }
}
