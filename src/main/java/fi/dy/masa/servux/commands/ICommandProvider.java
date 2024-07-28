package fi.dy.masa.servux.commands;

import fi.dy.masa.servux.interfaces.IServerCommand;

public interface ICommandProvider
{
    void registerCommand(IServerCommand command);
    void unregisterCommand(IServerCommand command);
}
