package fi.dy.masa.servux.loggers;

import java.util.concurrent.TimeUnit;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickRateManager;
import fi.dy.masa.servux.loggers.data.TPSData;
import fi.dy.masa.servux.mixin.server.IMixinServerTickRateManager;

public class DataLoggerTPS extends DataLoggerBase<CompoundTag>
{
    public static final Codec<CompoundTag> CODEC = CompoundTag.CODEC;

    public DataLoggerTPS(DataLogger type)
    {
        super(type);
    }

    @Override
    public CompoundTag getResult(MinecraftServer server)
    {
        try
        {
            return (CompoundTag) TPSData.CODEC.encodeStart(server.registryAccess().createSerializationContext(NbtOps.INSTANCE), this.build(server)).getOrThrow();
        }
        catch (Exception e)
        {
            return new CompoundTag();
        }
    }
    
    private TPSData build(MinecraftServer server)
    {
        ServerTickRateManager tickManager = server.tickRateManager();
        boolean frozen = tickManager.isFrozen();
        boolean sprinting = tickManager.isSprinting();
        final double mspt = (double) server.getAverageTickTimeNanos() / TimeUnit.MILLISECONDS.toNanos(1L);
        double tps = 1000.0D / Math.max(sprinting ? 0.0 : tickManager.millisecondsPerTick(), mspt);

        if (frozen)
        {
            tps = 0.0d;
        }
        
        return new TPSData(mspt,
                        tps,
                        ((IMixinServerTickRateManager) tickManager).servux_getStringTicks(),
                        frozen,
                        sprinting,
                        tickManager.isSteppingForward()
        );
    }
}
