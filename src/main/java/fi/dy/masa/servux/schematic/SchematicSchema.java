package fi.dy.masa.servux.schematic;

public record SchematicSchema(int litematicVersion, int minecraftDataVersion)
{
    @Override
    public String toString()
    {
        return "V" + this.litematicVersion() + " / DataVersion " + this.minecraftDataVersion();
    }
}
