package fi.dy.masa.servux.loggers;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;

public class DataLoggerMobCaps extends DataLoggerBase<NbtCompound>
{
    public static final Codec<NbtCompound> CODEC = NbtCompound.CODEC;

    public DataLoggerMobCaps(DataLogger type)
    {
        super(type);
    }

    @Override
    public NbtCompound getResult(MinecraftServer server)
    {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("MobCaps", "Test");
        return nbt;
    }
}
