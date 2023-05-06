using GigaSpaces.Core;
using GigaSpaces.Core.Metadata;
using YamlDotNet.Serialization.NamingConventions;
using YamlDotNet.Serialization;
using System.Net;


/*
 * Notes: YamlDotNet.13.1.0
 *             var rec = new Record
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

 */
namespace ReadRedoLogContents
{
    public class Program
    {
        private ISpaceProxy? spaceProxy;
        private SpaceReplay? spaceReplay;

        static void Main(string[] args)
        {
            Program program = new Program();
            program.Initialize();
            // See https://aka.ms/new-console-template for more information
            Console.WriteLine("Done.");
        }
        
        private void Initialize()
        {
            
            SpaceProxyFactory factory = new SpaceProxyFactory("dataExampleSpace");
            factory.LookupLocators = "172.31.8.136";
            factory.LookupGroups = "xap-16.2.1";

            spaceProxy = factory.Create();

            spaceReplay = new SpaceReplay(spaceProxy);

            var deserializer = new DeserializerBuilder()
                    .WithNamingConvention(CamelCaseNamingConvention.Instance)
                    .Build();
            
            var p = deserializer.Deserialize <List<Record>>(File.OpenText("C:\\Users\\Administrator\\Documents\\redologcontents.yaml"));

            int count = 0;
            int writeCount = 0;
            int removeCount = 0;
            int changeCount = 0;
            foreach (var record in p)
            {
                string operand = record.Opr;
                Console.WriteLine(operand);
                if (string.Equals(operand, "write", StringComparison.OrdinalIgnoreCase)) {
                    Console.WriteLine("Processing write...");
                    Console.WriteLine("record is: " + record);
                    spaceReplay.write(record);
                    writeCount++;
                }
                else if (string.Equals(operand, "remove", StringComparison.OrdinalIgnoreCase))
                {
                    Console.WriteLine("Processing remove...");
                    Console.WriteLine("record is: " + record);
                    spaceReplay.remove(record);
                    removeCount++;
                }
                else if (string.Equals(operand, "change", StringComparison.OrdinalIgnoreCase))
                {
                    Console.WriteLine("Processing change...");
                    Console.WriteLine("record is: " + record);
                    spaceReplay.change(record);
                    changeCount++;
                }
                else
                {
                    Console.WriteLine("Unsupported redo log operand: {0}", operand);
                }

                count++;
                if (count % 10 == 0)
                {
                    Console.WriteLine("The number of records processed is: " + count);
                }
            }
            Console.WriteLine("Done.");
            Console.WriteLine("Printing summary information:");
            Console.WriteLine("Total number of records processed: {0}", count);
            Console.WriteLine("  Total number of writes processed: {0}", writeCount);
            Console.WriteLine("  Total number of removes processed: {0}", removeCount);
            Console.WriteLine("  Total number of changes processed: {0}", changeCount);
            /*
            var p = deserializer.Deserialize<Record>(yaml);
            Console.WriteLine(p);
             */
        }
    }

}