package fi.dy.masa.servux.litematics.players;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerIdentityProvider
{
    private final Map<UUID, PlayerIdentity> players = new HashMap<>();

    public PlayerIdentityProvider()
    {
        players.put(PlayerIdentity.MISSING_PLAYER_UUID, PlayerIdentity.MISSING_PLAYER);
    }

    public PlayerIdentity putPlayer(final GameProfile profile)
    {
        return putPlayer(profile.getId(), profile.getName());
    }
    public PlayerIdentity putPlayer(final UUID uuid, final String name)
    {
        return players.putIfAbsent(uuid, new PlayerIdentity(uuid, name));
    }
    @Nullable
    public PlayerIdentity getPlayer(final UUID uuid)
    {
        return players.getOrDefault(uuid, null);
    }
    public PlayerIdentity updateName(final UUID uuid, final String name)
    {
        if (players.containsKey(uuid))
            return players.replace(uuid, new PlayerIdentity(uuid, name));
        else
            return putPlayer(uuid, name);
    }
    public PlayerIdentity fromJson(final JsonObject obj)
    {
        if (!obj.has("uuid") || !obj.has("name"))
        {
            return PlayerIdentity.MISSING_PLAYER;
        }

        final UUID jsonUUID = UUID.fromString(obj.get("uuid").getAsString());
        return players.computeIfAbsent(jsonUUID, key -> new PlayerIdentity(jsonUUID, obj.get("name").getAsString()));
    }
}
