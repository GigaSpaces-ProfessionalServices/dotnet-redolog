using GigaSpaces.Core;
using GigaSpaces.XAP.Events;
using GigaSpaces.XAP.Events.Notify;
using ReadRedoLogContentsTest.Common;
using System.Configuration;
using GigaSpaces.Core.Admin.ServiceGrid;
using GigaSpaces.Core.Admin.ServiceGrid.Space;

namespace ReadRedoLogContentsTest
{

    public class Program
    {
        private string spaceName;
        private string lookupLocators;
        private string lookupGroups;
        private ISpaceProxy spaceProxy;
        private IServiceGridAdmin admin;

        public Program()
        {
        }
        public void Configure()
        {
            spaceName = ConfigurationManager.AppSettings.Get("spaceName");
            lookupLocators = ConfigurationManager.AppSettings.Get("locators");
            lookupGroups = ConfigurationManager.AppSettings.Get("groups");

            SpaceProxyFactory factory = new SpaceProxyFactory(spaceName);
            factory.LookupLocators = lookupLocators;
            factory.LookupGroups = lookupGroups;

            spaceProxy = factory.Create();

            ServiceGridAdminBuilder adminBuilder = new ServiceGridAdminBuilder();
            adminBuilder.Locators.Add(lookupLocators);
            adminBuilder.Groups.Add(lookupGroups);
            
            admin = adminBuilder.CreateAdmin();
        }
        public void CreateNotifyContainer()
        {

            NotifyEventListenerContainer<SpaceReplayEvent> notifyEventListenerContainer = new NotifyEventListenerContainer<SpaceReplayEvent>(spaceProxy);

            notifyEventListenerContainer.Template = new SpaceReplayEvent();
           
            notifyEventListenerContainer.DataEventArrived += new DelegateDataEventArrivedAdapter<SpaceReplayEvent, SpaceReplayEvent>(EventListener).WriteBackDataEventHandler;
        }
        public SpaceReplayEvent EventListener(IEventListenerContainer<SpaceReplayEvent> sender, DataEventArgs<SpaceReplayEvent> e)
        {
            // some action needed here
            Console.WriteLine("Notify listener received a SpaceReplayEvent.");
            return null;
        }
        public void CreateSpaceModeEventListener()
        {
            admin.Spaces.SpaceModeChanged += HandleSpaceModeChanged;
        }
        void HandleSpaceModeChanged(object sender, SpaceModeChangedEventArgs e)
        {
            Console.WriteLine("Space [" + e.SpaceInstance.Space.Name + "] " +
                        "Instance [" + e.SpaceInstance.InstanceId + "/" + e.SpaceInstance.BackupId + "] " +
                        "changed mode from [" + e.PreviousMode + "] to [" + e.NewMode + "]");

            if (e.PreviousMode == GigaSpaces.Core.Admin.SpaceMode.None && 
                e.NewMode == GigaSpaces.Core.Admin.SpaceMode.Primary)
            {
                Console.WriteLine("Instance {0} has changed from SpaceMode.None to SpaceMode.Primary.", e.SpaceInstance.ToString());
                WriteEvent();
            }
        }
        public void WriteEvent()
        {
            ReadRedoLogContentsTest.Common.SpaceReplayEvent data = new ReadRedoLogContentsTest.Common.SpaceReplayEvent();
            data.Id = 1L;
            data.Guid = Guid.NewGuid().ToString();
            data.TimeStamp = DateTime.Now;
            spaceProxy.Write(data);
        }

        public static void Main(string[] args)
        {
            Program program = new Program();
            program.Configure();
            //program.CreateNotifyContainer();
            program.CreateSpaceModeEventListener();
        }
    } 
}
