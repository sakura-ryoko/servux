package fi.dy.masa.servux.litematics.players;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.UUID;

public class PlayerIdentity
{
    public static final UUID MISSING_PLAYER_UUID = UUID.fromString("4c1b738f-56fa-4011-8273-498c972424ea");
    public static final PlayerIdentity MISSING_PLAYER = new PlayerIdentity(MISSING_PLAYER_UUID, "No Player");

    private UUID uuid;
    private String playerName;

    public PlayerIdentity(UUID uuid, String player)
    {
        this.uuid = uuid;
        this.playerName = player;
    }
    public UUID getUUID() { return this.uuid; }
    public String getPlayerName() { return this.playerName; }
    public void updateUUID(UUID uuid) { this.uuid = uuid; }
    public void updatePlayerName(String name) { this.playerName = name; }

    public JsonObject toJson()
    {
        final JsonObject jsonObject = new JsonObject();

        jsonObject.add("uuid", new JsonPrimitive(this.uuid.toString()));
        jsonObject.add("name", new JsonPrimitive(this.playerName));

        return jsonObject;
    }
}
