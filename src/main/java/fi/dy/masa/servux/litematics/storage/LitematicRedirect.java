package fi.dy.masa.servux.litematics.storage;

import fi.dy.masa.servux.litematics.LitematicState;
import fi.dy.masa.servux.litematics.placement.LitematicPlacement;
import fi.dy.masa.servux.litematics.utils.LitematicUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LitematicRedirect implements ILitematicStorage
{
    private final ILitematicStorage fs;
    private final Map<UUID, Redirect> redir = new HashMap<>();

    public LitematicRedirect() { this.fs = new LitematicStorage(); }

    @Override
    public LitematicState getLitematicState(LitematicPlacement placement)
    {
        final UUID hash = placement.getHash();
        if (this.redir.containsKey(hash))
        {
            if (hash.equals(this.redir.get(hash).getHash()))
                return LitematicState.LITEMATIC_PRESENT;
            else
                return fs.getLitematicState(placement);
        }
        return fs.getLitematicState(placement);
    }

    @Override
    public File createLitematic(LitematicPlacement placement)
    {
        return fs.createLitematic(placement);
    }

    @Override
    public File getLitematic(LitematicPlacement placement)
    {
        final UUID hash = placement.getHash();
        if (this.redir.containsKey(hash))
        {
            final Redirect red = this.redir.get(hash);
            if (red.exists() && hash.equals(red.getHash()))
                return red.redir;
            else
                this.redir.remove(hash);
        }
        return fs.getLitematic(placement);
    }
    public void addRedirect(final File file)
    {
        final Redirect red = new Redirect(file);
        this.redir.put(red.getHash(), red);
    }
    private static class Redirect
    {
        File redir;
        UUID hash = null;
        long hashTS;
        Redirect(File file)
        {
            this.redir = file;
            this.hash = getHash();
        }
        private UUID getHash()
        {
            if (this.hashTS == this.redir.lastModified())
                return this.hash;
            try {
                this.hash = LitematicUtils.createChecksum(new FileInputStream(this.redir));
            }
            catch (NoSuchAlgorithmException | IOException e)
            {
                //throw new RuntimeException(e);
                return null;
            }
            this.hashTS = this.redir.lastModified();
            return this.hash;
        }
        boolean exists() { return this.redir.exists() && this.redir.canRead(); }
    }
}
