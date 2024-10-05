package fi.dy.masa.servux.dataproviders;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import me.lucko.fabric.api.permissions.v0.Permissions;

import com.mojang.serialization.DataResult;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import fi.dy.masa.servux.network.ServerPlayHandler;
import fi.dy.masa.servux.network.packet.ServuxScrollerHandler;
import fi.dy.masa.servux.network.packet.ServuxScrollerPacket;
import fi.dy.masa.servux.settings.IServuxSetting;
import fi.dy.masa.servux.settings.ServuxIntSetting;

public class ScrollerDataProvider extends DataProviderBase
{
    public static final ScrollerDataProvider INSTANCE = new ScrollerDataProvider();
    protected final static ServuxScrollerHandler<ServuxScrollerPacket.Payload> HANDLER = ServuxScrollerHandler.getInstance();
    protected final NbtCompound metadata = new NbtCompound();
    protected ServuxIntSetting permissionLevel = new ServuxIntSetting(this, "permission_level", 0, 4, 0);
    protected ServuxIntSetting massCraftPermissionLevel = new ServuxIntSetting(this, "mass_craft_permission_level", 0, 4, 0);
    protected List<IServuxSetting<?>> settings = List.of(this.permissionLevel, this.massCraftPermissionLevel);

    protected ScrollerDataProvider()
    {
        super("scroller_data",
                ServuxScrollerHandler.CHANNEL_ID,
                ServuxScrollerPacket.PROTOCOL_VERSION,
              0, Reference.MOD_ID+ ".provider.scroller_data",
              "Item Scroller Data provider for Crafting Services");

        this.metadata.putString("name", this.getName());
        this.metadata.putString("id", this.getNetworkChannel().toString());
        this.metadata.putInt("version", this.getProtocolVersion());
        this.metadata.putString("servux", Reference.MOD_STRING);
    }

    @Override
    public List<IServuxSetting<?>> getSettings()
    {
        return settings;
    }

    @Override
    public void registerHandler()
    {
        ServerPlayHandler.getInstance().registerServerPlayHandler(HANDLER);
        if (this.isRegistered() == false)
        {
            HANDLER.registerPlayPayload(ServuxScrollerPacket.Payload.ID, ServuxScrollerPacket.Payload.CODEC, IPluginServerPlayHandler.BOTH_SERVER);
            this.setRegistered(true);
        }
        HANDLER.registerPlayReceiver(ServuxScrollerPacket.Payload.ID, HANDLER::receivePlayPayload);
    }

    @Override
    public void unregisterHandler()
    {
        HANDLER.unregisterPlayReceiver();
        ServerPlayHandler.getInstance().unregisterServerPlayHandler(HANDLER);
    }

    @Override
    public IPluginServerPlayHandler<?> getPacketHandler()
    {
        return HANDLER;
    }

    public void sendMetadata(ServerPlayerEntity player)
    {
        if (this.hasPermission(player) == false)
        {
            // No Permission
            Servux.debugLog("scroller_service: Denying access for player {}, Insufficient Permissions", player.getName().getLiteralString());
            return;
        }

        NbtCompound nbt = new NbtCompound();
        nbt.copyFrom(this.metadata);

        Servux.debugLog("scrollerDataChannel: sendMetadata to player {}", player.getName().getLiteralString());

        // Sends Metadata handshake, it doesn't succeed the first time, so using networkHandler
        if (player.networkHandler != null)
        {
            HANDLER.sendPlayPayload(player.networkHandler, new ServuxScrollerPacket.Payload(ServuxScrollerPacket.MetadataResponse(this.metadata)));
        }
        else
        {
            HANDLER.sendPlayPayload(player, new ServuxScrollerPacket.Payload(ServuxScrollerPacket.MetadataResponse(this.metadata)));
        }
    }

    public void onPacketFailure(ServerPlayerEntity player)
    {
        // Do something when packets fail, if required
    }

    public void requestMassCraft(ServerPlayerEntity player, @Nullable NbtCompound data)
    {
        if (this.hasPermissionsForMassCraft(player) == false)
        {
            return;
        }

        // Do Something
    }

    public void refreshRecipeManager(ServerPlayerEntity player, @Nullable NbtCompound data)
    {
        if (this.hasPermission(player) == false)
        {
            return;
        }

        ServerWorld world = player.getServerWorld();
        Collection<RecipeEntry<?>> recipes = world.getRecipeManager().values();
        NbtCompound nbt = new NbtCompound();
        NbtList list = new NbtList();

        if (data != null)
        {
            Servux.debugLog("scrollerDataChannel: received RecipeManager request from {}, client version: {}", player.getName().getLiteralString(), data.getString("version"));
        }

        recipes.forEach((recipeEntry ->
        {
            DataResult<NbtElement> dr = Recipe.CODEC.encodeStart(NbtOps.INSTANCE, recipeEntry.value());

            if (dr.result().isPresent())
            {
                NbtCompound entry = new NbtCompound();
                entry.putString("id_reg", recipeEntry.id().getRegistry().toString());
                entry.putString("id_value", recipeEntry.id().getValue().toString());
                entry.put("recipe", dr.result().get());
                list.add(entry);
            }
        }));

        nbt.put("RecipeManager", list);

        // Use Packet Splitter
        HANDLER.encodeServerData(player, ServuxScrollerPacket.ResponseS2CStart(nbt));
    }

    public boolean hasPermissionsForMassCraft(ServerPlayerEntity player)
    {
        return Permissions.check(player, this.permNode + ".mass_craft", this.massCraftPermissionLevel.getValue());
    }

    @Override
    public boolean hasPermission(ServerPlayerEntity player)
    {
        return Permissions.check(player, this.permNode, this.permissionLevel.getValue());
    }

    @Override
    public void onTickEndPre()
    {
        // NO-OP
    }

    @Override
    public void onTickEndPost()
    {
        // NO-OP
    }
}
