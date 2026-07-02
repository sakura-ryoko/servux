package fi.dy.masa.servux.paper.util;

import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Paper-side replacement for Fabric's Mixin-based {@code NbtView}/{@code IMixinNbtWriteView}:
 * extracts the raw {@link CompoundTag} that {@link Entity#saveWithoutId(ValueOutput)} writes into
 * a {@link TagValueOutput}, since that method takes the new "View" serialization API rather than
 * a plain NBT compound, and {@code TagValueOutput} has no public getter for the result.
 * <p>
 * Paper has no Mixin system, so this uses plain reflection on the same private field Fabric's
 * {@code @Accessor("output")} targets - functionally identical, just without bytecode injection.
 * This is the one piece of the entity_data channel relying on a private-field name instead of a
 * stable public API; low risk (single field, tied to the whole NBT View API rather than likely to
 * change independently), but worth double-checking first if anything in this channel misbehaves.
 *
 * @see <a href="../../../../../../../../../src/main/java/fi/dy/masa/servux/util/nbt/NbtView.java">NbtView.java (Fabric reference)</a>
 * @see <a href="../../../../../../../../../src/main/java/fi/dy/masa/servux/mixin/nbt/IMixinNbtWriteView.java">IMixinNbtWriteView.java (Fabric reference)</a>
 */
public final class NbtViewHelper
{
    private static final Logger LOGGER = LoggerFactory.getLogger("servux-paper-NbtView");
    private static final Field OUTPUT_FIELD;

    static
    {
        try
        {
            OUTPUT_FIELD = TagValueOutput.class.getDeclaredField("output");
            OUTPUT_FIELD.setAccessible(true);
        }
        catch (NoSuchFieldException e)
        {
            throw new ExceptionInInitializerError(e);
        }
    }

    private NbtViewHelper()
    {
    }

    /** Serializes {@code entity} via the vanilla {@code saveWithoutId(ValueOutput)} path and extracts the resulting NBT. */
    public static CompoundTag saveWithoutId(Entity entity, RegistryAccess registryAccess)
    {
        ProblemReporter reporter = new ProblemReporter.ScopedCollector(LOGGER);
        ValueOutput output = TagValueOutput.createWithContext(reporter, registryAccess);

        entity.saveWithoutId(output);

        try
        {
            return (CompoundTag) OUTPUT_FIELD.get(output);
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException("NbtViewHelper: failed to read TagValueOutput#output via reflection", e);
        }
    }
}
