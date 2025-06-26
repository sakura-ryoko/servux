package fi.dy.masa.servux.loggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fi.dy.masa.servux.mixin.server.IMixinServerTickManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickManager;

import java.util.concurrent.TimeUnit;

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
            return (NbtCompound) Data.CODEC.encodeStart(server.getRegistryManager().getOps(NbtOps.INSTANCE), this.build(server)).getOrThrow();
        }
        catch (Exception e)
        {
            return new NbtCompound();
        }
    }
    
    private Data build(MinecraftServer server)
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
        
        return new Data(mspt, tps,
                        ((IMixinServerTickManager) tickManager).servux_getStringTicks(),
                        frozen, sprinting,
                        tickManager.isStepping()
        );
    }
    
    public record Data(double mspt, double tps, double sprintTicks, boolean frozen, boolean sprinting, boolean stepping)
    {
        public static Codec<Data> CODEC = RecordCodecBuilder.create(
                (inst) -> inst.group(
                        PrimitiveCodec.DOUBLE.fieldOf("mspt").forGetter(Data::mspt),
                        PrimitiveCodec.DOUBLE.fieldOf("tps").forGetter(Data::tps),
                        PrimitiveCodec.DOUBLE.fieldOf("sprintTicks").forGetter(Data::sprintTicks),
                        PrimitiveCodec.BOOL.fieldOf("frozen").forGetter(Data::frozen),
                        PrimitiveCodec.BOOL.fieldOf("sprinting").forGetter(Data::sprinting),
                        PrimitiveCodec.BOOL.fieldOf("stepping").forGetter(Data::stepping)
                                    ).apply(inst, Data::new));
    }
}
