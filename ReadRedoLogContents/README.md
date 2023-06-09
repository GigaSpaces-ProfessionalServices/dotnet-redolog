# ReadRedoLogContents

## Introduction and Process Flow

This is a .NET C# console application project that reads the contents of the redo log in yaml format. It will recreate the objects and re-run the operation by calling the appropriate GigaSpaces API, thereby writing, updating or removing objects from the space. The input yaml is the transformed contents of the GigaSpaces redo log which was originally stored as a Sqlite database file.

## Overview

There are 4 main files in this .NET project:

1. `Program.cs` - Processes the command line arguments, runs the main loop that processes the record objects that have been deserialized from the yaml.
2. `Record.cs` - A class that contains information of the replication that occurred and the data that was modified.
3. `SpaceReplay.cs` - Converts the record object into .NET POCOs. These .NET objects can be written to the space or other operations can be run, such as remove or change.
4. `ChangeContentParser`.cs - In the case of GigaSpace change operations, the operation is encoded in a specific text format. We parse this text to identify the operation and the change values.

## Running the Program

This program takes the following command line arguments:

* `--spaceName=<space name>`. The name of the space to connect to. This argument is required.
* `--lookupLocators=<lookup locators>`. The lookup locators used to connect to the space. This argument is required.
* `--lookupGroups=<lookup groups>`. The lookup groups used to connect to the space. This argument is required.
* `--redoLogYaml=<redolog.yaml>`. The filename containing the redo log contents. This argument is required.
* `--assembyFileName=<\path\to\assemby.dll>`. The assembly filename that contains the POCO class definitions. This argument is required. The assembly file is being used because we process text information and we dynamically create objects based on a classname or type.

## Notes

* We process and create objects from text. We have tested on a limited set of types. However, the code can be modified to support other types.
* Currently we demonstrate 1 type of change operation, which is increment. However the code is structured so that other change types can be added.
* We assume that the SpaceId will not be autogenerated.

