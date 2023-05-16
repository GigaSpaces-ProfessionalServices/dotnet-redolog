# dotnet-redolog

With this project, a .NET space can ensure all changes are written to disk. In the case of a disruption, in case any in-memory contents have not been persisted to the database, a copy of the change has already been written to disk. This project contains code to flush the redo log, transform the redo log content (saved in sqlite) to yaml so that can be used by other processes. One example is to apply the updates back to the space.
