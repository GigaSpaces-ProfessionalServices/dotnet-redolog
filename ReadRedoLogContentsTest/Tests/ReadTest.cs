using GigaSpaces.Core;

using ReadRedoLogContentsTest.Common;
using System.Configuration;

namespace ReadRedoLogContentsTest
{
    public class ReadTest
    {

        string? spaceName;
        string? lookupLocators;
        string? lookupGroups;
        ISpaceProxy? spaceProxy;


        public void Configure()
        {
            spaceName = ConfigurationManager.AppSettings.Get("spaceName");
            lookupLocators = ConfigurationManager.AppSettings.Get("locators");
            lookupGroups = ConfigurationManager.AppSettings.Get("groups");
            string sValidationOnlyEnabled = ConfigurationManager.AppSettings.Get("validateOnlyEnabled");

            SpaceProxyFactory factory = new SpaceProxyFactory(spaceName);
            factory.LookupLocators = lookupLocators;
            factory.LookupGroups = lookupGroups;

            spaceProxy = factory.Create();
        }
        private void readTest()
        {
            Data template = new Data();
            template.Id = 1;
            Data data = spaceProxy.Read(template);
            Console.WriteLine("verifying data.. data.Date: {0}, in ticks {1}", data.Date, data.Date.GetValueOrDefault().Ticks);
        }


        public static void Main(string[] args)
        {
            ReadTest test = new ReadTest();
            test.Configure();
            test.readTest();
        }
    }
}