package fi.dy.masa.servux.network;

import net.minecraft.util.Identifier;

import java.util.Objects;

public class PayloadTypes implements IPayloadType
{
    public enum PayloadType {
        STRING,
        DATA,
        CARPET_HELLO
    }
    private final PayloadType type;
    private String path;
    private final String name;
    private final Identifier id;

    public PayloadTypes(PayloadType type, String namespace)
    {
        this.type = type;
        this.name = namespace;
        this.setType();
        this.id = new Identifier(this.name, this.path);
    }
    public Identifier getIdentifier()
    {
        if (checkValidType())
            return this.id;
        else return null;
    }
    private void setType()
    {
        switch (this.type) {
            case DATA -> this.path = "data";
            case STRING -> this.path = "string";
            case CARPET_HELLO -> this.path = "hello";
            default -> this.path = "invalid";
        }
    }
    private boolean checkValidType()
    {
        return !Objects.equals(this.path, "invalid");
    }
}
