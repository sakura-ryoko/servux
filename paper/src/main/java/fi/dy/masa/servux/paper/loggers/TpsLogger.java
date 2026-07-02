package fi.dy.masa.servux.paper.loggers;

import org.bukkit.Bukkit;
import org.bukkit.ServerTickManager;

import net.minecraft.nbt.CompoundTag;

/**
 * Paper-side mirror of Servux's {@code DataLoggerTPS} (Fabric side). Unlike Fabric, which needs a
 * custom Mixin accessor (@code IMixinServerTickManager) to reach the frozen/sprinting/stepping
 * tick-rate-manager state, Paper exposes all of it through fully public API:
 * {@code Bukkit.getTPS()}/{@code getAverageTickTime()} for the tps/mspt figures, and
 * {@code Bukkit.getServerTickManager()} (a public {@code org.bukkit.ServerTickManager}) for the
 * frozen/sprinting/stepping booleans - no NMS access needed anywhere in this class.
 * <p>
 * Produces a {@link CompoundTag} matching the exact field names of Fabric's
 * {@code fi.dy.masa.servux.loggers.data.TPSData} record/Codec: {@code mspt}, {@code tps},
 * {@code sprintTicks}, {@code frozen}, {@code sprinting}, {@code stepping}.
 *
 * @see <a href="../../../../../../../../../src/main/java/fi/dy/masa/servux/loggers/DataLoggerTPS.java">DataLoggerTPS.java (Fabric reference)</a>
 * @see <a href="../../../../../../../../../src/main/java/fi/dy/masa/servux/loggers/data/TPSData.java">TPSData.java (Fabric reference)</a>
 */
public final class TpsLogger
{
    private TpsLogger()
    {
    }

    public static CompoundTag buildNbt()
    {
        double tps = Bukkit.getTPS()[0];
        double mspt = Bukkit.getAverageTickTime();

        ServerTickManager manager = Bukkit.getServerTickManager();
        boolean frozen = manager.isFrozen();
        boolean sprinting = manager.isSprinting();
        boolean stepping = manager.isStepping();

        // No public Bukkit/Paper equivalent found for Fabric's "sprintTicks" (remaining
        // sprint-tick count, read via a custom Mixin accessor on Fabric). Approximated with
        // the frozen-ticks-remaining count while frozen (the closest public equivalent),
        // 0 otherwise.
        long sprintTicks = frozen ? manager.getFrozenTicksToRun() : 0L;

        CompoundTag nbt = new CompoundTag();
        nbt.putDouble("mspt", mspt);
        nbt.putDouble("tps", tps);
        nbt.putLong("sprintTicks", sprintTicks);
        nbt.putBoolean("frozen", frozen);
        nbt.putBoolean("sprinting", sprinting);
        nbt.putBoolean("stepping", stepping);

        return nbt;
    }
}
