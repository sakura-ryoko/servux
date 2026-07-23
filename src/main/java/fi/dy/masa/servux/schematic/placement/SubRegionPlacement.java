package fi.dy.masa.servux.schematic.placement;

import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

import fi.dy.masa.servux.Servux;

public class SubRegionPlacement
{
    public static final Codec<SubRegionPlacement> CODEC = RecordCodecBuilder.create(
            inst -> inst.group(
                    PrimitiveCodec.STRING.fieldOf("Name").forGetter(get -> get.name),
                    BlockPos.CODEC.fieldOf("DefaultPos").forGetter(get -> get.defaultPos),
                    BlockPos.CODEC.fieldOf("Pos").forGetter(get -> get.pos),
                    Rotation.CODEC.fieldOf("Rotation").forGetter(get -> get.rotation),
                    Mirror.CODEC.fieldOf("Mirror").forGetter(get -> get.mirror),
                    PrimitiveCodec.BOOL.fieldOf("Enabled").forGetter(get -> get.enabled),
                    PrimitiveCodec.BOOL.fieldOf("RenderingEnabled").forGetter(get -> get.renderingEnabled),
                    PrimitiveCodec.BOOL.fieldOf("IgnoreEntities").forGetter(get -> get.ignoreEntities),
                    PrimitiveCodec.INT.fieldOf("CoordinateLockMask").forGetter(get -> get.coordinateLockMask)
            ).apply(inst, SubRegionPlacement::new)
    );
    public static final StreamCodec<@NotNull ByteBuf, @NotNull Mirror> BLOCK_MIRROR_PACKET_CODEC = ByteBufCodecs.STRING_UTF8.map(Mirror::valueOf, Mirror::getSerializedName);
    public static final StreamCodec<@NotNull ByteBuf, @NotNull SubRegionPlacement> PACKET_CODEC = new StreamCodec<>()
    {
        @Override
        public void encode(@NonNull ByteBuf buf, SubRegionPlacement value)
        {
            ByteBufCodecs.STRING_UTF8.encode(buf, value.name);
            BlockPos.STREAM_CODEC.encode(buf, value.defaultPos);
            BlockPos.STREAM_CODEC.encode(buf, value.pos);
            Rotation.STREAM_CODEC.encode(buf, value.rotation);
            BLOCK_MIRROR_PACKET_CODEC.encode(buf, value.mirror);
            ByteBufCodecs.BOOL.encode(buf, value.enabled);
            ByteBufCodecs.BOOL.encode(buf, value.renderingEnabled);
            ByteBufCodecs.BOOL.encode(buf, value.ignoreEntities);
            ByteBufCodecs.INT.encode(buf, value.coordinateLockMask);
        }

        @Override
        public @NonNull SubRegionPlacement decode(@NonNull ByteBuf buf)
        {
            return new SubRegionPlacement(
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    BlockPos.STREAM_CODEC.decode(buf),
                    BlockPos.STREAM_CODEC.decode(buf),
                    Rotation.STREAM_CODEC.decode(buf),
                    BLOCK_MIRROR_PACKET_CODEC.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.INT.decode(buf)
            );
        }
    };

    private final String name;
    private final BlockPos defaultPos;
    private BlockPos pos;
    public Rotation rotation = Rotation.NONE;
    public Mirror mirror = Mirror.NONE;
    public boolean enabled = true;
    private boolean renderingEnabled = false;
    public boolean ignoreEntities;
    private int coordinateLockMask;

    public SubRegionPlacement(BlockPos pos, String name)
    {
        this.pos = pos;
        this.defaultPos = pos;
        this.name = name;
    }

    private SubRegionPlacement(String name, BlockPos defPos, BlockPos pos, Rotation rot, Mirror mirror, Boolean enabled, Boolean renderingEnabled, Boolean ignoreEntities, Integer coordinateLockMask)
    {
        this(defPos, name);
        this.pos = pos;
        this.rotation = rot;
        this.mirror = mirror;
        this.enabled = enabled;
        this.renderingEnabled = renderingEnabled;
        this.ignoreEntities = ignoreEntities;
        this.coordinateLockMask = coordinateLockMask;
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public boolean ignoreEntities()
    {
        return this.ignoreEntities;
    }

    public boolean matchesRequirement(RequiredEnabled required)
    {
        if (required == RequiredEnabled.ANY)
        {
            return true;
        }

        if (required == RequiredEnabled.PLACEMENT_ENABLED)
        {
            return this.isEnabled();
        }

        Servux.LOGGER.warn("RequiredEnabled.RENDERING_ENABLED is not supported on server side!");
        return false;
    }

    public String getName()
    {
        return this.name;
    }

    public BlockPos getPos()
    {
        return this.pos;
    }

    public Rotation getRotation()
    {
        return this.rotation;
    }

    public Mirror getMirror()
    {
        return this.mirror;
    }

    void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    void toggleEnabled()
    {
        this.setEnabled(! this.isEnabled());
    }

    void toggleIgnoreEntities()
    {
        this.ignoreEntities = ! this.ignoreEntities;
    }

    void setPos(BlockPos pos)
    {
        this.pos = pos;
    }

    void setRotation(Rotation rotation)
    {
        this.rotation = rotation;
    }

    void setMirror(Mirror mirror)
    {
        this.mirror = mirror;
    }

    void resetToOriginalValues()
    {
        this.pos = this.defaultPos;
        this.rotation = Rotation.NONE;
        this.mirror = Mirror.NONE;
        this.enabled = true;
        this.ignoreEntities = false;
    }

    public boolean isRegionPlacementModifiedFromDefault()
    {
        return this.isRegionPlacementModified(this.defaultPos);
    }

    public boolean isRegionPlacementModified(BlockPos originalPosition)
    {
        return this.isEnabled() == false ||
               this.ignoreEntities() ||
               this.getMirror() != Mirror.NONE ||
               this.getRotation() != Rotation.NONE ||
               this.getPos().equals(originalPosition) == false;
    }

    public enum RequiredEnabled
    {
        ANY,
        PLACEMENT_ENABLED,
        RENDERING_ENABLED;
    }
}
