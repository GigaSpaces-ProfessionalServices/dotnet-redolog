using GigaSpaces.Core;

using ReadRedoLogContentsTest.Common;
using System.Configuration;

namespace ReadRedoLogContentsTest
{
    public class WriteTest
    {

        string? spaceName;
        string? lookupLocators;
        string? lookupGroups;
        bool validateOnlyEnabled = false;
        ISpaceProxy? spaceProxy;

        private static DateTime testStartDate = DateTime.Now;

        private Data testData1 = createTestData1();
        private Data testData2 = createTestData2();

        /********** set up sample test objects **********/
        private static Data createTestData1()
        {
            Data data = new Data();
            data.Id = 1;
            data.Info = "testData1";
            data.Type = 1;
            data.Date = testStartDate;
            data.Processed = false;
            return data;
        }
        // for take test
        private static Data createTestData2()
        {
            Data data = new Data();
            data.Id = 2;
            data.Info = "testData2";
            data.Type = 2;
            data.Date = DateTime.Now;
            data.Processed = false;
            return data;
        }

        /********** write or take *********/
        private void write()
        {
            spaceProxy.Write(testData1);
            spaceProxy.Write(testData2);
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
            take();
        }



        /********** validate **********/
        // a Tick is one hundred nanoseconds or one ten-millionth of a second (100 * 1/10^9)

        // java date contains a long and the time unit is milliseconds from epoch which is 1970-01-01T00:00:00Z
        bool dataEquals(Data? original, Data? newData)
        {
            
            DateTime originalDt = original.Date.GetValueOrDefault();
            DateTime newDataDt = newData.Date.GetValueOrDefault();

            Console.WriteLine("original.Date: {0}, Ticks: {1}", original.Date, originalDt.Ticks);

            Console.WriteLine("newData.Date:  {0}, Ticks: {1}", newData.Date, newDataDt.Ticks);

            long truncOriginalTicks = originalDt.Ticks / 10000;

            long truncNewDataTicks = newDataDt.Ticks / 10000;

            return
                original.Id == newData.Id &&
                original.Info.Equals(newData.Info) &&
                truncOriginalTicks == truncNewDataTicks &&
                original.Processed == newData.Processed &&
                original.Type == newData.Type;
        }

        public void Configure()
        {
            spaceName = ConfigurationManager.AppSettings.Get("spaceName");
            lookupLocators = ConfigurationManager.AppSettings.Get("locators");
            lookupGroups = ConfigurationManager.AppSettings.Get("groups");
            string sValidationOnlyEnabled = ConfigurationManager.AppSettings.Get("validateOnlyEnabled");
            validateOnlyEnabled = bool.Parse(sValidationOnlyEnabled);

            SpaceProxyFactory factory = new SpaceProxyFactory(spaceName);
            factory.LookupLocators = lookupLocators;
            factory.LookupGroups = lookupGroups;

            spaceProxy = factory.Create();
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

        private void dateTest()
        {
            DateTime minDate = DateTime.MinValue;
            DateTime maxDate = DateTime.MaxValue;
            long minValueTicks = DateTime.MinValue.Ticks;
            long maxValueTicks = DateTime.MaxValue.Ticks;

            Console.WriteLine(".NET DateTime.MinValue is: {0}, expressed in Ticks: {1}", minDate, minValueTicks);
            Console.WriteLine(".NET DateTime.MaxValue is: {0}, expressed in Ticks: {1}", maxDate, maxValueTicks);


            // converting long in millis originally from Java
            // long in millis from Java epoch 1970-01-01
            // sample date, 6/29/2023 3:36:57 PM
            long longInMillis = 1688053017180L;
            
            TimeSpan timeSpan = TimeSpan.FromMilliseconds(longInMillis);
            string answer = string.Format("days: {0}, hours: {1:D2}, minutes: {2:D2}, seconds: {3:D2}, millis: {4:D3}",
                    timeSpan.Days,
                    timeSpan.Hours,
                    timeSpan.Minutes,
                    timeSpan.Seconds,
                    timeSpan.Milliseconds);
            Console.WriteLine("Timespan is: {0}, pretty printed is: {1}", timeSpan, answer);

            DateTime javaEpoch = new DateTime(1970, 01, 01);
            //DateTime(int year, int month, int day, int hour, int minute, int second, int millisecond);

            DateTime convertedDt = javaEpoch.Add(timeSpan);
            
            Console.WriteLine("Converted date: {0}, in ticks: {1}", convertedDt, convertedDt.Ticks);
        }

        public static void Main(string[] args)
        {
            WriteTest test = new WriteTest();
            test.dateTest();
            //test.Configure();
            //test.makeUpdates();
            //test.verifyTestData1();
        }
    }
}