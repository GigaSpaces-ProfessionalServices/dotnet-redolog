# ReadRedoLogContentsTest

## Introduction

This is a .NET C# console application project that can be used to verify objects that have been written to the space.

## Overview

There are 3 sub-modules in this .NET project:

1. Common - Contains class definitions of the test objects.
2. Feeder - It writes objects to the space and can validate the objects were written correctly.
3. Space - An example configuration for a space with the correct configurations for writing the replication redo log to disk.

## Running the Feeder Program

The Feeder program uses an app.config where you will find the following settings:

```
	<appSettings>
		<add key="spaceName" value="dataExampleSpace" />
		<add key="lookupLocators" value="EC2AMAZ-PUUQMQH" />
		<add key="lookupGroups" value="xap-16.2.1" />
		<add key="validateOnlyEnabled" value="false"/>
	</appSettings>
```

 * `spaceName` - the name of the space to connect to
 * `lookupLocators` - the lookup hosts to connect to
 * `lookupGroups` - the lookup groups that identify the GigaSpaces cluster
 * `validateOnlyEnabled` - do the validation only, don't write or modify objects in the space.

## Deploying a space

1. After the project is built, zip the contents of the build output directory for example, `cd ReadRedoLogContentsTest\Space\bin\Debug\net4.8\; zip -r space.zip *`. The pu.config should be located at the root of the zip archive.
2. Deployment - one way is through the CLI, for example, 
```cd C:\GigaSpaces\smart-cache.net-16.2.1-x64\NET v4.0\Bin;
   
   gs.exe pu deploy --partitions=1 <name given to pu deployment> <path to zip file>
```
Note: Make sure you have Grid Service Containers available before calling deploy.
