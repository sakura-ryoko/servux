package fi.dy.masa.servux.network.payload;

import net.minecraft.util.Identifier;

/**
 * Used in the TYPES HashMap for registering Channels.
 * This helps provide a more idiot-proof method to help with the work flow in a more abstract manner,
 * And this class is where the actual Channel Identifier gets created.
 */
public class PayloadCodec implements IPayloadType
{
    private final PayloadType type;
    private final Identifier id;
    private final String key;
    private final String namespace;
    private final String path;
    private boolean play_registered;
    private boolean config_registered;

    protected PayloadCodec(PayloadType type, String key, String namespace, String path)
    {
        this.type = type;
        this.key = key;
        this.namespace = namespace;
        this.path = path;
        this.id = new Identifier(this.namespace, this.path);
        this.play_registered = false;
        this.config_registered = false;
    }
    public PayloadType getType() { return this.type; }
    public String getKey() { return this.key; }
    public String getNamespace() { return this.namespace; }
    public String getPath() { return this.path; }
    public Identifier getId() { return this.id; }
    public void registerPlayCodec() { this.play_registered = true; }
    public void registerConfigCodec() { this.config_registered = true; }
    public boolean isPlayRegistered() { return this.play_registered; }
    public boolean isConfigRegistered() { return this.config_registered; }
}
