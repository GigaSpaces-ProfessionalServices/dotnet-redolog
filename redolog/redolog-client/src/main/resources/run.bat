
set GIGA=C:\GigaSpaces\smart-cache.net-16.2.1-x64\Runtime

set PROJ_DIR=C:\Users\Administrator\Downloads\dotnet-redolog\redolog

set CLASSES_DIR=%PROJ_DIR%\redolog-client\target\classes

set MODEL_DIR=%PROJ_DIR%\my-app-model\target\classes

set CLASSPATH=%GIGA%\lib\required\*

set CLASSPATH=%CLASSPATH%;%GIGA%\lib\platform\zookeeper\*

set CLASSPATH=%CLASSPATH%;%GIGA%\lib\platform\service-grid\*

set CLASSPATH=%CLASSPATH%;%GIGA%\lib\optional\tiered-storage\sqlite\*

set CLASSPATH=%CLASSPATH%;%MODEL_DIR%

set CLASSPATH=%CLASSPATH%;%CLASSES_DIR%

set GS_LOOKUP_LOCATORS=localhost
set GS_LOOKUP_GROUPS=xap-16.2.1

rem %JAVA_HOME%\bin\java -Xms1g -Xmx1g -classpath %CLASSPATH% com.gs.Feeder dataExampleSpace
%JAVA_HOME%\bin\java -Xms1g -Xmx1g -classpath %CLASSPATH% com.gs.FlushRedoLogToDisk dataExampleSpace
