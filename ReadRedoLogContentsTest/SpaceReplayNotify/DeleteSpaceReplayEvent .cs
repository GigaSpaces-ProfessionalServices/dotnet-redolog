using GigaSpaces.Core;
using System.Configuration;
using ReadRedoLogContentsTest.Common;

namespace ReadRedoLogContentsTest
{
    public class DeleteSpaceReplayEvent
    {
        string? spaceName;
        string? lookupLocators;
        string? lookupGroups;
        ISpaceProxy? spaceProxy;

        public void Configure() {
            spaceName = ConfigurationManager.AppSettings.Get("spaceName");
            lookupLocators = ConfigurationManager.AppSettings.Get("locators");
            lookupGroups = ConfigurationManager.AppSettings.Get("groups");

            SpaceProxyFactory factory = new SpaceProxyFactory(spaceName);
            factory.LookupLocators = lookupLocators;
            factory.LookupGroups = lookupGroups;

            spaceProxy = factory.Create();

        }
        public void DeleteEvent() {
            SpaceReplayEvent template = new SpaceReplayEvent();
            spaceProxy.Take(template);
        }
        public static void Main(string[] args)
        {
            DeleteSpaceReplayEvent deleteSpaceReplayEvent = new DeleteSpaceReplayEvent();
            deleteSpaceReplayEvent.Configure();
            deleteSpaceReplayEvent.DeleteEvent();
        }
    }
}