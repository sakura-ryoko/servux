package fi.dy.masa.servux.util;

import com.google.common.collect.ImmutableList;

public enum EasyPlacementProtocol
{
    AUTO                ("auto",                  "auto"),
    V3                  ("v3",                    "v3"),
    V2                  ("v2",                    "v2"),
    SLAB_ONLY           ("slabs_only",            "slabs_only"),
    NONE                ("none",                  "none");

    public static final ImmutableList<EasyPlacementProtocol> VALUES = ImmutableList.copyOf(values());

    private final String configString;
    private final String translationKey;

    EasyPlacementProtocol(String configString, String translationKey)
    {
        this.configString = configString;
        this.translationKey = translationKey;
    }

    public static EasyPlacementProtocol fromStringStatic(String name)
    {
        for (EasyPlacementProtocol val : VALUES)
        {
            if (val.configString.equalsIgnoreCase(name))
            {
                return val;
            }
        }

        return EasyPlacementProtocol.AUTO;
    }
}
