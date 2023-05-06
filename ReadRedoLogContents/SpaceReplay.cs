using GigaSpaces.Core;
using GigaSpaces.Core.Metadata;
using System.Reflection;
using System.Text.RegularExpressions;

namespace ReadRedoLogContents
{
    internal class SpaceReplay
    {
        private ISpaceProxy _proxy;

        private Assembly? _assembly;

        public SpaceReplay(ISpaceProxy proxy) { _proxy = proxy; }
        /*
        public static T ChangeType<T>(object value)
        {
            var t = typeof(T);

            if (t.IsGenericType && t.GetGenericTypeDefinition().Equals(typeof(Nullable<>)))
            {
                if (value == null)
                {
                    return default(T);
                }

                t = Nullable.GetUnderlyingType(t);
            }

            return (T)Convert.ChangeType(value, t);
        }

        public static object ChangeType(object value, Type conversion)
        {
            var t = conversion;

            Console.WriteLine("t is: " + t);

            if (t.IsGenericType && t.GetGenericTypeDefinition().Equals(typeof(Nullable<>)))
            {
                if (value == null)
                {
                    return null;
                }

                t = Nullable.GetUnderlyingType(t);
            }

            return Convert.ChangeType(value, t);
        }
        */

        private void initAssembly()
        {
            //string assemblyFilename = @"C:\GigaSpaces\smart-cache.net-16.2.1-x64\NET v4.0\Examples\ProcessingUnit\Common\obj\x64\Debug\GigaSpaces.Examples.ProcessingUnit.Common.dll";
            string assemblyFilename = @"C:\Users\Administrator\source\repos\ProcessingUnitWithCSharp\Release\GigaSpaces.Examples.ProcessingUnit.Common.dll";

            _assembly = Assembly.LoadFrom(assemblyFilename);

        }
        public void write(Record record) {
            string sType = record.Type;
            Console.WriteLine("sType is: " + sType);

            if ( _assembly == null )
            {
                initAssembly();
            }

            var entry = _assembly.CreateInstance(sType);
            Type type = _assembly.GetType(sType);

            // TODO - cache sorted field names
            PropertyInfo[] propertyInfo = type.GetProperties();

            string[] propertyNames = new string[propertyInfo.Length];

            for (int i = 0; i < propertyInfo.Length; i++)
            {
                propertyNames[i] = propertyInfo[i].Name;
            }

            Array.Sort(propertyNames);

            for (int i = 0; i < record.FixedProps.Count; i++)
            {
                object value = record.FixedProps[i];
                string propertyName = propertyNames[i];

                Console.WriteLine("i is: " + i);
                Console.WriteLine("propertyName is: " + propertyName);
                Console.WriteLine("value is: " + value);

                if (value != null)
                {

                    PropertyInfo property = type.GetProperty(propertyNames[i]);

                    Console.WriteLine("property.Name is: {0}, property.PropertyType() is: {1} ", property.Name, property.PropertyType);
                    Console.WriteLine("value type is: " + value.GetType());
                    property.SetValue(entry, convertStringToObject(property.PropertyType, value));
                }
            }
            _proxy.Write(entry);
        }
        public void remove(Record record)
        {

            // register the type
            // ask gs to what the id property is
            // use this to call takeById
            string sType = record.Type;
            Console.WriteLine("sType is: " + sType);

            if (_assembly == null)
            {
                initAssembly();
            }
            var entry = _assembly.CreateInstance(sType);
            Type entryType = _assembly.GetType(sType);

            ISpaceTypeDescriptor spaceTypeDescriptor = registerAndGetTypeDescriptor(entryType);

            // create the idQuery
            string idPropertyName = spaceTypeDescriptor.IdPropertyName;
            PropertyInfo idProperty = entryType.GetProperty(idPropertyName);
            Type idPropertyType = idProperty.PropertyType;

            string sId = parseIdStringFromUid(record.Uid);

            //var idObject = parseStringToObject(idPropertyType, sId);
            IdQuery<object> idQuery = new GigaSpaces.Core.IdQuery<object>(sType, sId);

            var returnValue = _proxy.TakeById(idQuery);
            //idProperty.SetValue(entry, convertStringToObject(idProperty.PropertyType, record.Uid));

            //var returnValue = _proxy.Take(entry);
            //SqlQuery<GigaSpaces.Examples.ProcessingUnit.Common.Data> query = new SqlQuery<Data>("Id = ?");

            if (returnValue == null)
            {
                Console.WriteLine("An attempt to remove record: {0} was made, but the item was not found.", record.ToString());
            }
            else
            {
                Console.WriteLine("The call to TakeById was successful");
                Console.WriteLine("returnValue is: " + returnValue.ToString());
            }
            // TODO - verify
        }

        public void change(Record record)
        {

            // register the type
            // ask gs to what the id property is
            // use this to call change
            string sType = record.Type;
            Console.WriteLine("sType is: " + sType);
            Console.WriteLine("record.Changes is: " + record.Changes);

            if (_assembly == null)
            {
                initAssembly();
            }

            var entry = _assembly.CreateInstance(sType);
            Type entryType = _assembly.GetType(sType);

            ISpaceTypeDescriptor spaceTypeDescriptor = registerAndGetTypeDescriptor(entryType);
            
            // create the idQuery
            string idPropertyName = spaceTypeDescriptor.IdPropertyName;
            PropertyInfo idProperty = entryType.GetProperty(idPropertyName);
            Type idPropertyType = idProperty.PropertyType;

            string sId = parseIdStringFromUid(record.Uid);
            
            //var idObject = parseStringToObject(idPropertyType, sId);
            IdQuery<object> idQuery = new GigaSpaces.Core.IdQuery<object>(sType, sId);

            ChangeSet changeSet = ChangeContentParser.parse(record.Changes, entryType);

            _proxy.Change<object>(idQuery, changeSet);

        }
        private ISpaceTypeDescriptor registerAndGetTypeDescriptor(Type entryType)
        {
            _proxy.TypeManager.RegisterTypeDescriptor(entryType);
            return _proxy.TypeManager.GetTypeDescriptor(entryType);
        }
        private static string parseIdStringFromUid(string uid)
        {
            // TODO: need logic to check if SpaceId is autoGenerate=true  
            string[] uidTokens = uid.Split('^');
            return uidTokens[2];
        }
        public static object convertStringToObject(Type toType, object fromValue)
        {
            // handle Nullable type            
            if (Nullable.GetUnderlyingType(toType) != null)
            {
                // TODO: test other nullable
                // it's nullable
                //if (Nullable.GetUnderlyingType(toType) == typeof(string))
                //{
                // handle and test nullable string
                //}    
                if (Nullable.GetUnderlyingType(toType) == typeof(bool))
                {
                    return parseStringToObject(typeof(bool), fromValue);
                }
                else if (Nullable.GetUnderlyingType(toType) == typeof(byte))
                {
                    return parseStringToObject(typeof(byte), fromValue);
                }

                else if (Nullable.GetUnderlyingType(toType) == typeof(int))
                {
                    return parseStringToObject(typeof(int), fromValue);
                }
                else if (Nullable.GetUnderlyingType(toType) == typeof(long))
                {
                    return parseStringToObject(typeof(long), fromValue);
                }
                else if (Nullable.GetUnderlyingType(toType) == typeof(double))
                {
                    return parseStringToObject(typeof(double), fromValue);
                }
                else if (Nullable.GetUnderlyingType(toType) == typeof(DateTime))
                {
                    return parseStringToObject(typeof(DateTime), fromValue);
                }
            } else // handle regular type
            {
                return parseStringToObject(toType, fromValue);
            }
            return fromValue;
        }
        public static object parseStringToObject(Type toType, object fromValue)
        {
            // TODO: I think this is always string because it is deserialized from yaml, need to test
            // TODO: Test other types
            Type fromValueType = fromValue.GetType();

            if (toType == typeof(bool))
            {
                if (fromValueType == typeof(string))
                {
                    return bool.Parse((String)fromValue);
                }
            }
            else if (toType == typeof(byte))
            {
                if (fromValueType == typeof(string))
                {
                    return byte.Parse((String)fromValue);
                }
            }

            else if (toType == typeof(int))
            {
                if (fromValueType == typeof(string))
                {
                    return int.Parse((String)fromValue);
                }
            }
            else if (toType == typeof(long))
            {
                if (fromValueType == typeof(string))
                {
                    return long.Parse((String)fromValue);
                }
            }
            else if (toType == typeof(double))
            {
                if (fromValueType == typeof(string))
                {
                    return double.Parse((String)fromValue);
                }
            }
            else if (toType == typeof(DateTime))
            {
                if (fromValueType == typeof(string))
                {
                    long longValue = long.Parse((String)fromValue);
                    return new DateTime(longValue);
                }
            }
            // it's either a string type or type not handled above 
            return fromValue;
        }
        public static Type getPropertyType(Type entryType, string propertyName)
        {
            PropertyInfo propertyInfo = entryType.GetProperty(propertyName);
            return propertyInfo.PropertyType;
        }
    
    }

    public class ChangeContentParser
    {
        private const string expression = @"^([a-zA-Z]+)\[(.+?)\]";
        //                                     ^ captures ChangeSet type
        //                                                 ^ captures path and change value
        
        public static ChangeSet parse(string content, Type entryType)
        {
            ChangeSet changeSet = new ChangeSet();

            if (content.StartsWith("[") && content.EndsWith("]"))
            {
                content = content.Substring(1, content.Length - 1);
                Regex regex = new Regex(expression, RegexOptions.Compiled);
                Match m = regex.Match(content);
                while (m.Success)
                {
                    GroupCollection groups = m.Groups;
                    string changeType = groups[1].Value;
                    string changeDetails = groups[2].Value;
                    string item = string.Format("{0}[{1}]", changeType, changeDetails);
                    Console.WriteLine("item is: \"{0}\"", item);
                    Console.WriteLine("changeType is: {0}, changeDetails is: {1}", changeType, changeDetails);
                    content = content.Substring(item.Length);
                    addToChangeSet(changeSet, changeType, changeDetails, entryType);

                    if (content.StartsWith(", "))
                    {
                        // remove the comma separating ChangeSet
                        content = content.Substring(", ".Length);
                        Console.WriteLine("remainder is: \"{0}\"", content);

                        m = regex.Match(content);
                    } else
                    {
                        break;
                    }
                }
                return changeSet;
            }
            else
            {
                return changeSet;
            }
        }

        private static void addToChangeSet(ChangeSet changeSet, string changeType, string changeDetails, Type entryType)
        {
            string[] changeDetailsTokens = changeDetails.Split(',');
            
            string[] pathTokens = changeDetailsTokens[0].Split('=');
            string path = pathTokens[1];

            string[] valueTokens = changeDetailsTokens[1].Split('=');
            string value = valueTokens[1];

            // TODO; Test other change operations
            if (changeType.Equals("IncrementSpaceEntryMutator"))
            {
                Type toType = SpaceReplay.getPropertyType(entryType, path);
                var objValue = SpaceReplay.parseStringToObject(toType, value);

                if (toType == typeof(byte)) {
                    changeSet.Increment(path, (byte) objValue);
                    return;
                }
                else if (toType == typeof(int))
                {
                    changeSet.Increment(path, (int)objValue);
                    return;
                }
                else if (toType == typeof(long))
                {
                    changeSet.Increment(path, (long)objValue);
                    return;
                }
                else if (toType == typeof(double))
                {
                    changeSet.Increment(path, (double)objValue);
                    return;
                }
            }
            string sError = string.Format("The change type: {0} is not supported.", changeType);
            new InvalidOperationException(sError);
        }        
    }
}
