package fi.dy.masa.servux.loggers;

import java.util.concurrent.TimeUnit;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickManager;

import fi.dy.masa.servux.loggers.data.TPSData;
import fi.dy.masa.servux.mixin.server.IMixinServerTickManager;

public class DataLoggerTPS extends DataLoggerBase<NbtCompound>
{
    public static final Codec<NbtCompound> CODEC = NbtCompound.CODEC;

    public DataLoggerTPS(DataLogger type)
    {
        super(type);
    }

    @Override
    public NbtCompound getResult(MinecraftServer server)
    {
        try
        {
            return (NbtCompound) TPSData.CODEC.encodeStart(server.getRegistryManager().getOps(NbtOps.INSTANCE), this.build(server)).getOrThrow();
        }
        catch (Exception e)
        {
            return new NbtCompound();
        }
    }
    
    private TPSData build(MinecraftServer server)
    {
        ServerTickManager tickManager = server.getTickManager();
        boolean frozen = tickManager.isFrozen();
        boolean sprinting = tickManager.isSprinting();
        final double mspt = (double) server.getAverageNanosPerTick() / TimeUnit.MILLISECONDS.toNanos(1L);
        double tps = 1000.0D / Math.max(sprinting ? 0.0 : tickManager.getMillisPerTick(), mspt);

        if (frozen)
        {
            tps = 0.0d;
        }
        
        return new TPSData(mspt,
                        tps,
                        ((IMixinServerTickManager) tickManager).servux_getStringTicks(),
                        frozen,
                        sprinting,
                        tickManager.isStepping()
        );
    }
}
