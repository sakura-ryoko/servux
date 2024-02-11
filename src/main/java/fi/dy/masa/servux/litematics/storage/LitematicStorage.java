package fi.dy.masa.servux.litematics.storage;

import fi.dy.masa.servux.dataproviders.LitematicsDataProvider;
import fi.dy.masa.servux.litematics.LitematicState;
import fi.dy.masa.servux.litematics.placement.LitematicPlacement;
import fi.dy.masa.servux.litematics.utils.LitematicUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class LitematicStorage implements ILitematicStorage
{
    private final HashMap<LitematicPlacement, Long> buf = new HashMap<>();
    @Override
    public LitematicState getLitematicState(final LitematicPlacement placement)
    {
        final File file = getLitematicPath(placement);
        if (file.isFile())
        {
            if (isDownloading(placement))
            {
                return LitematicState.LITEMATIC_DOWNLOADING;
            }
            if ((buf.containsKey(placement)))
            {
                if (buf.get(placement) == file.lastModified() || hashComp(file, placement))
                {
                    return LitematicState.LITEMATIC_PRESENT;
                }
            }
            return LitematicState.LITEMATIC_DESYNC;
        }
        return LitematicState.NO_LITEMATIC;
    }

    @Override
    @Nullable
    public File createLitematic(LitematicPlacement placement) {
        if (getLitematicState(placement).isFileReady())
        {
            throw new IllegalArgumentException("file is ready");
        }
        final File file = getLitematicPath(placement);
        if (file.exists())
        {
            if (!file.delete())
                throw new SecurityException("file failed to be deleted");
        }
        try {
            if (file.createNewFile())
            {
                if (file.exists())
                {
                    return file;
                }
            }
        }
        catch (IOException ignored) {}
        return null;
    }

    @Override
    public File getLitematic(LitematicPlacement placement)
    {
        if (getLitematicState(placement).isFileReady())
            return getLitematicPath(placement);
        else return null;
    }

    private File getLitematicPath(LitematicPlacement placement)
    {
        final File path = LitematicsDataProvider.INSTANCE.getLitematicFolder();
        return new File(path, placement.getHash().toString() + ".litematic");
    }
    private boolean hashComp(File file, LitematicPlacement placement)
    {
        UUID hash = null;
        try {
            hash = LitematicUtils.createChecksum(new FileInputStream(file));
        }
        catch (Exception ignored)
        { }
        if (hash == null)
            return false;
        if (hash.equals(placement.getHash()))
        {
            buf.put(placement, file.lastModified());
            return true;
        }
        return false;
    }

    private boolean isDownloading(LitematicPlacement placement)
    {
        return LitematicsDataProvider.INSTANCE.getDownloadState(placement);
    }
}
