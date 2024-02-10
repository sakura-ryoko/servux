package fi.dy.masa.servux.litematics;

public enum LitematicState
{
    NO_LITEMATIC(true, false),
    LITEMATIC_DESYNC(true, false),
    LITEMATIC_DOWNLOADING(false, false),
    LITEMATIC_PRESENT(false, true);
    private final boolean downloadReady;
    private final boolean fileReady;
    LitematicState(final boolean downloadReady, final boolean fileReady)
    {
        this.downloadReady = downloadReady;
        this.fileReady = fileReady;
    }
    public boolean isDownloadReady() { return this.downloadReady; }
    public boolean isFileReady() { return this.fileReady; }
}
