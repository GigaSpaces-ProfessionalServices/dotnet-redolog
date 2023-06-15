using GigaSpaces.Core;
using GigaSpaces.Core.Document;
using ReadRedoLogContentsTest.Common;
using System.Configuration;
using System.Security.Cryptography;

namespace ReadRedoLogContentsTest
{
    public class Feeder
    {

        string? spaceName;
        string? lookupLocators;
        string? lookupGroups;
        bool validateOnlyEnabled = false;
        ISpaceProxy? spaceProxy;

        private Data testData1 = createTestData1();
        private Data testData2 = createTestData2();

        private Order testOrder1 = createTestOrder1();
        private OrderWrongDefaultValue testOrderWrongDefaultValue1 = createTestOrderWrongDefaultValue1();

        private static Order createTestOrder1()
        {
            Order order = new Order();
            order.Id = 1;
            order.Info = "testOrder1";
            order.CalCumQty = 8;
            order.CalExecValue = 4;
            return order;
        }
        private static OrderWrongDefaultValue createTestOrderWrongDefaultValue1()
        {
            OrderWrongDefaultValue order = new OrderWrongDefaultValue();
            order.Id = 1;
            order.Info = "testOrderWrongDefaultValue1";
            order.CalCumQty = 8;
            order.CalExecValue = 4;
            return order;
        }

        private static Data createTestData1()
        {
            Data data = new Data();
            data.Id = 1;
            data.Info = "testData1";
            data.Type = 1;
            data.Processed = false;
            return data;
        }
        private static Data createTestData2()
        {
            Data data = new Data();
            data.Id = 2;
            data.Info = "testData2";
            data.Type = 2;
            data.Processed = false;
            return data;
        }

        bool dataEquals(Data? original, Data? newData)
        {
            return
                original.Id == newData.Id &&
                original.Info.Equals(newData.Info) &&
                original.Processed == newData.Processed &&
                original.Type == newData.Type;
        }

        private bool orderAfterChangeEquals(Order original, Order newOrder)
        {
            
            double calExecValue = (original.CalExecValue.HasValue ? original.CalExecValue.Value : 0);

            calExecValue += 15.0;
            long calCumQty = original.CalCumQty + 10;

            return
                original.Id == newOrder.Id &&
                original.Info.Equals(newOrder.Info) &&
                calCumQty == newOrder.CalCumQty &&
                calExecValue == (newOrder.CalExecValue.HasValue? newOrder.CalExecValue: 0);
                
        }
        // the difference between this and the OrderAfterChangeEquals is that it doesn't rely on template matching and uses idQuery
        // both in the SpaceReplay and in this test.
        private bool orderWrongDefaultValueAfterChangeEquals(OrderWrongDefaultValue original, OrderWrongDefaultValue newOrder)
        {

            double calExecValue = (original.CalExecValue.HasValue ? original.CalExecValue.Value : 0);

            calExecValue += 15.0;
            long calCumQty = original.CalCumQty + 10;

            return
                original.Id == newOrder.Id &&
                original.Info.Equals(newOrder.Info) &&
                calCumQty == newOrder.CalCumQty &&
                calExecValue == (newOrder.CalExecValue.HasValue ? newOrder.CalExecValue : 0);

        }

        public void Configure()
        {
            spaceName      = ConfigurationManager.AppSettings.Get("spaceName");
            lookupLocators = ConfigurationManager.AppSettings.Get("locators");
            lookupGroups   = ConfigurationManager.AppSettings.Get("groups");
            string sValidationOnlyEnabled = ConfigurationManager.AppSettings.Get("validateOnlyEnabled");
            validateOnlyEnabled = bool.Parse(sValidationOnlyEnabled);

            SpaceProxyFactory factory = new SpaceProxyFactory(spaceName);
            factory.LookupLocators = lookupLocators;
            factory.LookupGroups = lookupGroups;

            spaceProxy = factory.Create();
        }
        private void write()
        {
            spaceProxy.Write(testData1);
            spaceProxy.Write(testData2);
            spaceProxy.Write(testOrder1);
            spaceProxy.Write(testOrderWrongDefaultValue1);
        }
        private void change()
        {
            //IdQuery<object> idQuery = new GigaSpaces.Core.IdQuery<object>("ReadRedoLogContentsTest.Common.Order", "1");
            //IdQuery<Order> idQuery = new IdQuery<Order>(1L);

            object template = new Order();
            ((Order)template).Id = 1L;

            ChangeSet orderChange = new ChangeSet();
            orderChange.Increment("CalCumQty", 10L);
            orderChange.Increment("CalExecValue", 15.0d);            
            IChangeResult<object> orderChangeResults = spaceProxy.Change<object>(template, orderChange);
            if (orderChangeResults == null)
            {
                Console.WriteLine("orderChangeResults is null.");
            }
            else
            {
                Console.WriteLine("orderChangeResults.NumberOfChangedEntries is: {0}", orderChangeResults.NumberOfChangedEntries );
            }
        }
        private void changeWrongDefaultValue()
        {
            IdQuery<OrderWrongDefaultValue> idQuery = new IdQuery<OrderWrongDefaultValue>(1L);


            ChangeSet orderChange = new ChangeSet();
            orderChange.Increment("CalCumQty", 10L);
            orderChange.Increment("CalExecValue", 15.0d);
            IChangeResult<object> orderChangeResults = spaceProxy.Change<object>(idQuery, orderChange);
            if (orderChangeResults == null)
            {
                Console.WriteLine("orderChangeResults for wrong default value test is null.");
            }
            else
            {
                Console.WriteLine("orderChangeResults.NumberOfChangedEntries for wrong default value test is: {0}", orderChangeResults.NumberOfChangedEntries);
            }
        }
        private void take()
        {
            Data template = new Data();
            template.Id = 2;

            Data returnValue = spaceProxy.Take<Data>(template);
            if (returnValue == null)
            {
                Console.WriteLine("in take(), returnValue is null");
            }
            else
            {
                Console.WriteLine("in take(), returned item's id: {0}", returnValue.Id);
            }

        }
        private void makeUpdates()
        {
            if (validateOnlyEnabled == true)
            {
                Console.WriteLine("Configuration has been set to validate only. Skipping update");
                return;
            }
            write();
            change();
            changeWrongDefaultValue();
            take();
        }
        private void verifyTestData1()
        {
            Data template = new Data();
            template.Id = 1;
            Data data = spaceProxy.Read(template);
            bool same = false;
            if (data != null)
            {
                same = dataEquals(testData1, data);
            }
            Console.WriteLine("verifying with testData1. Return value is same: " + same);
        }
        private void verifyChange()
        {
            Order template = new Order();
            template.Id = 1L;
            
            Order order = spaceProxy.Read(template);
            bool same = false;
            if (order != null)
            {
                same = orderAfterChangeEquals(testOrder1, order);
            }
            else
            {
                Console.WriteLine("Order read was null.");
            }
            Console.WriteLine("verifying with testOrder1 after change. Return value is same: " + same);

        }

        private void verifyChangeWrongDefaultValue()
        {
            IdQuery<OrderWrongDefaultValue> idQuery = new IdQuery<OrderWrongDefaultValue>(1L);


            OrderWrongDefaultValue order = spaceProxy.ReadById(idQuery);
            bool same = false;
            if (order != null)
            {
                same = orderWrongDefaultValueAfterChangeEquals(testOrderWrongDefaultValue1, order);
            }
            else
            {
                Console.WriteLine("OrderWrongDefaultValue read was null.");
            }
            Console.WriteLine("verifying with testOrderWrongDefaultValue1 after change. Return value is same: " + same);

        }


        public static void Main(string[] args)
        {
            Feeder feeder = new Feeder();
            feeder.Configure();
            feeder.makeUpdates();
            feeder.verifyTestData1();
            feeder.verifyChange();
            feeder.verifyChangeWrongDefaultValue();

        }
    }
}