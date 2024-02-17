package fi.dy.masa.servux.litematics.placement;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.servux.dataproviders.data.LitematicsDataProvider;
import fi.dy.masa.servux.litematics.players.PlayerIdentity;
import fi.dy.masa.servux.litematics.utils.LitematicUtils;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class LitematicPlacement
{
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private final UUID uuid;
    private final String fileName;
    private final UUID hash;
    private PlayerIdentity owner;
    private PlayerIdentity modifiedBy;
    private LocalDateTime modifiedTime;
    private LitematicPosition origin;
    private BlockRotation rot;
    private BlockMirror mirror;
    private SubRegion subRegion = new SubRegion();
    private Object mats;
    // Not implemented

    public LitematicPlacement(final UUID uuid, final String fileName, final UUID hash, final PlayerIdentity owner)
    {
        this.uuid = uuid;
        this.fileName = fileName;
        this.hash = hash;
        this.owner = owner;
        this.modifiedBy = owner;
        this.modifiedTime = LocalDateTime.now();
    }
    public LitematicPlacement(final UUID uuid, final File file, final PlayerIdentity owner)
    {
        this(uuid, removeExt(file), genHash(file), owner);
    }
    private static String removeExt(final File file)
    {
        final String fileName = file.getName();
        final int pos = fileName.lastIndexOf(".");
        return fileName.substring(0, pos);
    }
    private static UUID genHash(final File file)
    {
        UUID hash;
        try {
            hash = LitematicUtils.createChecksum(new FileInputStream(file));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return hash;
    }
    public JsonObject toJson()
    {
        final JsonObject obj = new JsonObject();
        obj.add("id", new JsonPrimitive(this.uuid.toString()));

        obj.add("file_name", new JsonPrimitive(this.fileName));
        obj.add("hash", new JsonPrimitive(this.hash.toString()));

        obj.add("origin", this.origin.toJson());
        obj.add("rotation", new JsonPrimitive(this.rot.name()));
        obj.add("mirror", new JsonPrimitive(this.mirror.name()));

        obj.add("owner", this.owner.toJson());
        if (!this.owner.equals(this.modifiedBy))
        {
            obj.add("lastModifiedBy", this.modifiedBy.toJson());
        }
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
        String time = timeFormatter.format(this.modifiedTime);
        obj.add("lastModifiedTime", new JsonPrimitive(time));

        if (this.subRegion.isModified())
        {
            obj.add("subregionData", this.subRegion.toJson());
        }

        return obj;
    }
    public static LitematicPlacement fromJson(final JsonObject obj)
    {
        if (obj.has("id")
                && obj.has("file_name")
                && obj.has("hash")
                && obj.has("origin")
                && obj.has("rotation")
                && obj.has("mirror")) {
            final UUID id = UUID.fromString(obj.get("id").getAsString());
            final String name = obj.get("file_name").getAsString();
            final UUID hashValue = UUID.fromString(obj.get("hash").getAsString());

            PlayerIdentity owner = PlayerIdentity.MISSING_PLAYER;
            if (obj.has("owner"))
            {
                owner = LitematicsDataProvider.INSTANCE.getPlayerIdentityProvider()
                        .fromJson(obj.get("owner").getAsJsonObject());
            }

            final LitematicPlacement newPlacement = new LitematicPlacement(id, name, hashValue, owner);

            final LitematicPosition pos = LitematicPosition.fromJson(obj.get("origin").getAsJsonObject());
            if (pos == null)
            {
                return null;
            }
            newPlacement.origin = pos;
            newPlacement.rot = BlockRotation.valueOf(obj.get("rotation").getAsString());
            newPlacement.mirror = BlockMirror.valueOf(obj.get("mirror").getAsString());

            if (obj.has("lastModifiedBy"))
            {
                newPlacement.modifiedBy = LitematicsDataProvider.INSTANCE.getPlayerIdentityProvider()
                        .fromJson(obj.get("lastModifiedBy").getAsJsonObject());
            }
            else
            {
                newPlacement.modifiedBy = owner;
            }

            if (obj.has("lastModifiedTime"))
            {
                newPlacement.modifiedBy = LitematicsDataProvider.INSTANCE.getPlayerIdentityProvider()
                        .fromJson(obj.get("lastModifiedBy").getAsJsonObject());
            }
            else
            {
                newPlacement.modifiedTime = LocalDateTime.now();
            }

            if (obj.has("subregionData"))
            {
                newPlacement.subRegion = SubRegion.fromJson(obj.get("subregionData"));
            }

            return newPlacement;
        }

        return null;
    }
    public UUID getUuid() { return this.uuid; }
    public String getName() { return this.fileName; }
    public UUID getHash() { return this.hash; }
    public String getDim() { return this.origin.getDimension(); }
    public BlockPos getPos() { return this.origin.getBlockPos(); }
    public LitematicPosition getOrigin() { return this.origin; }
    public BlockRotation getRot() { return this.rot; }
    public BlockMirror getMirror() { return this.mirror; }
    public PlayerIdentity getOwner() { return this.owner; }
    public PlayerIdentity getModifiedBy() { return this.modifiedBy; }
    public LocalDateTime getModifiedTime() { return this.modifiedTime; }
    public String getModifiedTimeAsString()
    {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
        return dateTimeFormatter.format(this.modifiedTime);
    }
    public void setOwner(final PlayerIdentity id) { this.owner = id; }
    public void setModifiedBy(final PlayerIdentity id) { this.modifiedBy = id; }
    public void setModifiedTime() { this.modifiedTime = LocalDateTime.now(); }
    public void setModifiedTime(LocalDateTime time) { this.modifiedTime = time; }
    public void setModifiedTime(final String formatted)
    {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
        this.modifiedTime = LocalDateTime.parse(formatted, dateTimeFormatter);
    }
    public Object getMatList() { return this.mats; }
    public void setMatList(Object o) { this.mats = o; }
    public SubRegion getSubRegion() { return this.subRegion; }
    public void setSubRegion(SubRegion subRegion) { this.subRegion = subRegion; }

    public LitematicPlacement move(final String dimension, final BlockPos pos, final BlockRotation rot, final BlockMirror mirror) {
        move(new LitematicPosition(pos, dimension), rot, mirror);
        return this;
    }

    public LitematicPlacement move(LitematicPosition origin, BlockRotation rot, BlockMirror mirror)
    {
        this.origin = origin;
        this.rot = rot;
        this.mirror = mirror;
        return this;
    }
}
