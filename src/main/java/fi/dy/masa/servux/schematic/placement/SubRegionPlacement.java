package fi.dy.masa.servux.schematic.placement;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.netty.buffer.ByteBuf;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import fi.dy.masa.servux.Servux;

import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;

public class SubRegionPlacement
{
    public static final Codec<SubRegionPlacement> CODEC = RecordCodecBuilder.create(
            inst -> inst.group(
                    PrimitiveCodec.STRING.fieldOf("Name").forGetter(get -> get.name),
                    BlockPos.CODEC.fieldOf("DefaultPos").forGetter(get -> get.defaultPos),
                    BlockPos.CODEC.fieldOf("Pos").forGetter(get -> get.pos),
                    BlockRotation.CODEC.fieldOf("Rotation").forGetter(get -> get.rotation),
                    BlockMirror.CODEC.fieldOf("Mirror").forGetter(get -> get.mirror),
                    PrimitiveCodec.BOOL.fieldOf("Enabled").forGetter(get -> get.enabled),
                    PrimitiveCodec.BOOL.fieldOf("RenderingEnabled").forGetter(get -> get.renderingEnabled),
                    PrimitiveCodec.BOOL.fieldOf("IgnoreEntities").forGetter(get -> get.ignoreEntities),
                    PrimitiveCodec.INT.fieldOf("CoordinateLockMask").forGetter(get -> get.coordinateLockMask)
            ).apply(inst, SubRegionPlacement::new)
    );
    public static final PacketCodec<ByteBuf, BlockMirror> BLOCK_MIRROR_PACKET_CODEC = PacketCodecs.STRING.xmap(BlockMirror::valueOf, BlockMirror::asString);
    public static final PacketCodec<ByteBuf, BlockRotation> BLOCK_ROTATION_PACKET_CODEC = PacketCodecs.STRING.xmap(BlockRotation::valueOf, BlockRotation::asString);
    public static final PacketCodec<ByteBuf, SubRegionPlacement> PACKET_CODEC = new PacketCodec<>()
    {
        @Override
        public void encode(ByteBuf buf, SubRegionPlacement value)
        {
            PacketCodecs.STRING.encode(buf, value.name);
            BlockPos.PACKET_CODEC.encode(buf, value.defaultPos);
            BlockPos.PACKET_CODEC.encode(buf, value.pos);
            BLOCK_ROTATION_PACKET_CODEC.encode(buf, value.rotation);
            BLOCK_MIRROR_PACKET_CODEC.encode(buf, value.mirror);
            PacketCodecs.BOOLEAN.encode(buf, value.enabled);
            PacketCodecs.BOOLEAN.encode(buf, value.renderingEnabled);
            PacketCodecs.BOOLEAN.encode(buf, value.ignoreEntities);
            PacketCodecs.INTEGER.encode(buf, value.coordinateLockMask);
        }

        @Override
        public SubRegionPlacement decode(ByteBuf buf)
        {
            return new SubRegionPlacement(
                    PacketCodecs.STRING.decode(buf),
                    BlockPos.PACKET_CODEC.decode(buf),
                    BlockPos.PACKET_CODEC.decode(buf),
                    BLOCK_ROTATION_PACKET_CODEC.decode(buf),
                    BLOCK_MIRROR_PACKET_CODEC.decode(buf),
                    PacketCodecs.BOOLEAN.decode(buf),
                    PacketCodecs.BOOLEAN.decode(buf),
                    PacketCodecs.BOOLEAN.decode(buf),
                    PacketCodecs.INTEGER.decode(buf)
            );
        }
    };

    private final String name;
    private final BlockPos defaultPos;
    private BlockPos pos;
    public BlockRotation rotation = BlockRotation.NONE;
    public BlockMirror mirror = BlockMirror.NONE;
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

    private SubRegionPlacement(String name, BlockPos defPos, BlockPos pos, BlockRotation rot, BlockMirror mirror, Boolean enabled, Boolean renderingEnabled, Boolean ignoreEntities, Integer coordinateLockMask)
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

    public BlockRotation getRotation()
    {
        return this.rotation;
    }

    public BlockMirror getMirror()
    {
        return this.mirror;
    }

    void toggleIgnoreEntities()
    {
        this.ignoreEntities = ! this.ignoreEntities;
    }

    void setPos(BlockPos pos)
    {
        this.pos = pos;
    }

    void setRotation(BlockRotation rotation)
    {
        this.rotation = rotation;
    }

    void setMirror(BlockMirror mirror)
    {
        this.mirror = mirror;
    }

    void resetToOriginalValues()
    {
        this.pos = this.defaultPos;
        this.rotation = BlockRotation.NONE;
        this.mirror = BlockMirror.NONE;
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
               this.getMirror() != BlockMirror.NONE ||
               this.getRotation() != BlockRotation.NONE ||
               this.getPos().equals(originalPosition) == false;
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();
        JsonArray arr = new JsonArray();

        arr.add(this.pos.getX());
        arr.add(this.pos.getY());
        arr.add(this.pos.getZ());

        obj.add("pos", arr);
        obj.add("name", new JsonPrimitive(this.getName()));
        obj.add("rotation", new JsonPrimitive(this.rotation.name()));
        obj.add("mirror", new JsonPrimitive(this.mirror.name()));
        obj.add("locked_coords", new JsonPrimitive(0));
        obj.add("enabled", new JsonPrimitive(this.enabled));
        obj.add("rendering_enabled", new JsonPrimitive(true));
        obj.add("ignore_entities", new JsonPrimitive(this.ignoreEntities));

        return obj;
    }


    public enum RequiredEnabled
    {
        ANY,
        PLACEMENT_ENABLED,
        RENDERING_ENABLED;
    }
}
