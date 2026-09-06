package fi.dy.masa.servux.schematic;

import org.jspecify.annotations.NonNull;

public record SchematicSchema(int litematicVersion, int minecraftDataVersion)
{
    @Override
    public @NonNull String toString()
    {
        return "V" + this.litematicVersion() + " / DataVersion " + this.minecraftDataVersion();
    }
}
