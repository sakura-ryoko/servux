package fi.dy.masa.servux.util;

public class FileUtils
{
    public static String getNameWithoutExtension(String name)
    {
        int i = name.lastIndexOf(".");
        return i != -1 ? name.substring(0, i) : name;
    }
}
