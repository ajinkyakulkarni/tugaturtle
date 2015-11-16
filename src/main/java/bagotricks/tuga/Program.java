package bagotricks.tuga;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class Program {

    private String content;

    private File file;

    private String group;

    private String id;

    private String name;

    public void insertText(int index, String text) {
        writeFile();
    }

    public void removeText(int begin, int end) {
        writeFile();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void writeContent(String content) {
        this.content = content;
        writeFile();
    }

    private void writeFile() {
        try (Writer writer = new OutputStreamWriter(
                new FileOutputStream(file),
                Library.CHARSET)) {
            writer.write(content);
        } catch (Exception e) {
            Thrower.throwAny(e);
        }
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Override
    public String toString() {
        return name;
    }

}
