package fi.dy.masa.servux.litematics.storage;

import fi.dy.masa.servux.litematics.LitematicState;
import fi.dy.masa.servux.litematics.placement.LitematicPlacement;

import java.io.File;

public interface ILitematicStorage
{
    LitematicState getLitematicState(LitematicPlacement placement);
    File createLitematic(LitematicPlacement placement);
    File getLitematic(LitematicPlacement placement);
}
