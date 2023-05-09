using GigaSpaces.Core;
using YamlDotNet.Serialization.NamingConventions;
using YamlDotNet.Serialization;


/*
 * Notes: YamlDotNet.13.1.0
 *            
            var rec = new Record
            {
                Opr = "write",
                Type = "com.gs.demo.Person",
                Uid = "12345",
            };

            var serializer = new SerializerBuilder()
                .WithNamingConvention(CamelCaseNamingConvention.Instance)
                .Build();
            var yaml = serializer.Serialize(rec);
            System.Console.WriteLine(yaml);
            var p = deserializer.Deserialize<Record>(yaml);
            Console.WriteLine(p);
*/
namespace ReadRedoLogContents
{
    public class Program
    {
        private const string INDENT = "  ";

        private static readonly NLog.Logger Logger = NLog.LogManager.GetCurrentClassLogger();

        // program arguments
        private string? spaceName;
        private string? lookupLocators;
        private string? lookupGroups;
        private string? redoLogFileName;
        private string? assemblyFileName;

        private ISpaceProxy? spaceProxy;
        private SpaceReplay? spaceReplay;

        static void Main(string[] args)
        {
            Program program = new Program();
            program.processArgs(args);
            program.checkParameters();

            program.Initialize();
            // See https://aka.ms/new-console-template for more information
            Console.WriteLine("Done.");
        }
        private void processArgs(string[] args)
        {
            try
            {
                int i = 0;
                while (i < args.Length)
                {
                    string s = args[i];
                    string sUpper = s.ToUpper();
                    Console.WriteLine("Processing {0}", s);

                    if (sUpper.StartsWith("--help".ToUpper()))
                    {
                        printUsage();
                        Environment.Exit(0);
                    }
                    else if (sUpper.StartsWith("--spaceName".ToUpper()))
                    {
                        string[] sArray = s.Split('=');
                        spaceName = sArray[1];
                    }
                    else if (sUpper.StartsWith("--lookupLocators".ToUpper()))
                    {
                        string[] sArray = s.Split('=');
                        lookupLocators = sArray[1];
                    }
                    else if (sUpper.StartsWith("--lookupGroups".ToUpper()))
                    {
                        string[] sArray = s.Split('=');
                        lookupGroups = sArray[1];
                    }
                    else if (sUpper.StartsWith("--redoLogYaml".ToUpper()))
                    {
                        string[] sArray = s.Split('=');
                        redoLogFileName = sArray[1];
                    }
                    else if (sUpper.StartsWith("--assemblyFileName".ToUpper()))
                    {
                        string[] sArray = s.Split('=');
                        assemblyFileName = sArray[1];
                    }
                    else
                    {
                        Console.WriteLine("Please enter valid arguments.");
                        printUsage();
                        Environment.Exit(-1);
                    }
                    i++;
                }
            }
            catch (Exception e)
            {
                Console.WriteLine(e.ToString());
                Console.WriteLine(System.Environment.StackTrace);
                printUsage();
                Environment.Exit(-1);
            }

        }
        private static void printUsage()
        {
            Console.WriteLine("This program reads the deserialized contents of the GigaSpaces redo log in yaml format.");
            Console.WriteLine("It will convert the yaml into records which can be used replay the redo log operations into the space.");
            Console.WriteLine($"Usage: {Environment.NewLine}");
            Console.WriteLine("The following parameters are accepted:");
            Console.WriteLine("{0}--spaceName=<space name>", INDENT);
            Console.WriteLine("{0}{1}The name of the space to connect to. This argument is required.", INDENT, INDENT);
            Console.WriteLine("{0}--lookupLocators=<lookup locators>", INDENT);
            Console.WriteLine("{0}{1}The lookup locators used to connect to the space. This argument is required.", INDENT, INDENT);
            Console.WriteLine("{0}--lookupGroups=<lookup groups>", INDENT);
            Console.WriteLine("{0}{1}The lookup groups used to connect to the space. This argument is required.", INDENT, INDENT);
            Console.WriteLine("{0}--redoLogYaml=<redolog.yaml>", INDENT);
            Console.WriteLine("{0}{1}The filename containing the redo log contents. This argument is required.", INDENT, INDENT);
            Console.WriteLine(@"{0}--assembyFileName=<\path\to\assemby.dll>", INDENT);
            Console.WriteLine("{0}{1}The assembly filename that contains the POCO class definitions. This argument is required.", INDENT, INDENT);
            Console.WriteLine("{0}--help", INDENT);
            Console.WriteLine("{0}{1}Display this help message and exit.", INDENT, INDENT);
            Console.WriteLine();
            Console.WriteLine($"Example: {Environment.NewLine}");
            Console.WriteLine(@"ReadRedoLogContents.exe --spaceName=dataExampleSpace --lookupLocators=EC2AMAZ-PUUQMQH --lookupGroups=xap-16.2.1 --redoLogYaml=C:\Users\Administrator\Documents\redologcontents.yaml --assemblyFileName=C:\Users\Administrator\source\repos\ProcessingUnitWithCSharp\Release\GigaSpaces.Examples.ProcessingUnit.Common.dll");
        }
        private void checkParameters()
        {
            if( spaceName == null ||
                lookupLocators == null || 
                lookupGroups == null ||
                redoLogFileName == null ||
                assemblyFileName == null)
            {
                Console.WriteLine("Please enter required arguments.");
                printUsage();
                Environment.Exit(-1);
            }
            if ( ! File.Exists(redoLogFileName) ||
                 ! File.Exists(assemblyFileName))
            {
                Console.WriteLine("The redoLogFileName or assemblyFileName must exist.");
                printUsage();
                Environment.Exit(-1);
            }
        }
        private void Initialize()
        {
            
            SpaceProxyFactory factory = new SpaceProxyFactory(spaceName);
            factory.LookupLocators = lookupLocators;
            factory.LookupGroups = lookupGroups;

            spaceProxy = factory.Create();

            spaceReplay = new SpaceReplay(spaceProxy, assemblyFileName);

            var deserializer = new DeserializerBuilder()
                    .WithNamingConvention(CamelCaseNamingConvention.Instance)
                    .Build();
            
            var p = deserializer.Deserialize <List<Record>>(File.OpenText(redoLogFileName));

            int count = 0;
            int writeCount = 0;
            int removeCount = 0;
            int changeCount = 0;
            foreach (var record in p)
            {
                string operand = record.Opr;

                if (string.Equals(operand, "write", StringComparison.OrdinalIgnoreCase)) {
                    Logger.Info("Processing write...");
                    Logger.Info("record is: " + record);
                    spaceReplay.write(record);
                    writeCount++;
                }
                else if (string.Equals(operand, "remove", StringComparison.OrdinalIgnoreCase))
                {
                    Logger.Info("Processing remove...");
                    Logger.Info("record is: " + record);
                    spaceReplay.remove(record);
                    removeCount++;
                }
                else if (string.Equals(operand, "change", StringComparison.OrdinalIgnoreCase))
                {
                    Logger.Info("Processing change...");
                    Logger.Info("record is: " + record);
                    spaceReplay.change(record);
                    changeCount++;
                }
                else
                {
                    Logger.Error("Unsupported redo log operand: {0}", operand);
                }

                count++;
                if (count % 10 == 0)
                {
                    Logger.Info("The number of records processed is: " + count);
                }
            }
            Logger.Info("Done.");
            Logger.Info("Printing summary information:");
            Logger.Info("Total number of records processed: {0}", count);
            Logger.Info("{0}Total number of writes processed: {1}", INDENT, writeCount);
            Logger.Info("{0}Total number of removes processed: {1}", INDENT, removeCount);
            Logger.Info("{0}Total number of changes processed: {1}", INDENT, changeCount);
        }
    }

}