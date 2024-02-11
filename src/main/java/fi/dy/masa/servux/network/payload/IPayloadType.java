package fi.dy.masa.servux.network.payload;

import net.minecraft.util.Identifier;

public interface IPayloadType
{
    PayloadType getType();
    String getKey();
    String getNamespace();
    String getPath();
    Identifier getId();
    void registerPlayCodec();
    void registerConfigCodec();
    boolean isPlayRegistered();
    boolean isConfigRegistered();
}
