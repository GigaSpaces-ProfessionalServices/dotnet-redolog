using GigaSpaces.Core;
using GigaSpaces.Core.Document;
using GigaSpaces.Core.Metadata;
using NLog;
using System.Reflection;

namespace ReadRedoLogContents
{
    /*
     * This class takes redo log record information, reconstructs the objects and makes GigaSpaces API calls to re-run the operation
     */
    internal class SpaceReplay
    {
        static object lockObject = new object();

        private static readonly NLog.Logger Logger = NLog.LogManager.GetCurrentClassLogger();

        private ISpaceProxy _proxy;

        private string? _assemblyFileName;
        private Assembly? _assembly;

        Dictionary<string, Type> typeNameTypePairs = new Dictionary<string, Type>();
        Dictionary<string, string[]> typeNameSortedPropertyNamesPairs = new Dictionary<string, string[]>();

        public SpaceReplay(ISpaceProxy proxy, string assemblyFileName)
        {
            _proxy = proxy;
            _assemblyFileName = assemblyFileName;
        }

        private void initAssembly()
        {
            _assembly = Assembly.LoadFrom(_assemblyFileName);
        }
        private Type getTypeFromTypeName(string typeName, Assembly assembly)
        {
            lock (lockObject)
            {
                if (typeNameTypePairs.ContainsKey(typeName))
                {
                    return typeNameTypePairs[typeName];
                }
                else
                {
                    Type type = assembly.GetType(typeName);
                    typeNameTypePairs.Add(typeName, type);
                    return type;
                }
            }
        }
        private string[] getSortedPropertyNamesFromTypeName(string typeName, Type entryType)
        {
            lock (lockObject)
            {
                if (typeNameSortedPropertyNamesPairs.ContainsKey(typeName))
                {
                    return typeNameSortedPropertyNamesPairs[typeName];
                }
                else
                {
                    PropertyInfo[] propertyInfo = entryType.GetProperties();

                    string[] propertyNames = new string[propertyInfo.Length];

                    for (int i = 0; i < propertyInfo.Length; i++)
                    {
                        propertyNames[i] = propertyInfo[i].Name;
                    }

                    Array.Sort(propertyNames);

                    typeNameSortedPropertyNamesPairs.Add(typeName, propertyNames);
                    return propertyNames;
                }
            }
        }
        private ISpaceTypeDescriptor registerAndGetTypeDescriptor(Type entryType)
        {
            _proxy.TypeManager.RegisterTypeDescriptor(entryType);
            return _proxy.TypeManager.GetTypeDescriptor(entryType);
        }
        public static Type getPropertyType(Type entryType, string propertyName)
        {
            PropertyInfo propertyInfo = entryType.GetProperty(propertyName);
            return propertyInfo.PropertyType;
        }

        public void write(Record record) {
            string sType = record.Type;
            Logger.Info("sType is: " + sType);

            if ( _assembly == null )
            {
                initAssembly();
            }

            var entry = _assembly.CreateInstance(sType);
            Type entryType = getTypeFromTypeName(sType, _assembly);

            string[] propertyNames = getSortedPropertyNamesFromTypeName(sType, entryType);

            // set the property values for each property in the entry
            for (int i = 0; i < record.FixedProps.Count; i++)
            {
                object value = record.FixedProps[i];
                string propertyName = propertyNames[i];

                Logger.Debug("i is: " + i);
                Logger.Debug("propertyName is: " + propertyName);
                Logger.Debug("value is: " + value);

                if (value != null)
                {
                    PropertyInfo property = entryType.GetProperty(propertyNames[i]);

                    Logger.Debug("property.Name is: {0}, property.PropertyType() is: {1} ", property.Name, property.PropertyType);
                    Logger.Debug("value type is: " + value.GetType());
                    property.SetValue(entry, convertStringToObject(property.PropertyType, value));
                }
            }
            _proxy.Write(entry);
        }
        public void remove(Record record)
        {
            // get the id
            // create an idQuery and set the id
            // call Take
            string sType = record.Type;
            Logger.Info("sType is: " + sType);

            if (_assembly == null)
            {
                initAssembly();
            }

            Type entryType = getTypeFromTypeName(sType, _assembly);

            string sId = parseIdStringFromUid(record.Uid);
            //object templateQuery = createIdTemplate(sType, sId);
            IQuery<SpaceDocument> idQuery = getIdQuery(sType, sId);

            var returnValue = _proxy.Take(idQuery);

            if (returnValue == null)
            {
                Logger.Error("An attempt to remove record: {0} was made, but the item was not found.", record.ToString());
            }
            else
            {
                Logger.Info("The call to Take was successful");
                Logger.Debug("returnValue is: " + returnValue.ToString());
            }
        }

        public void change(Record record)
        {

            string sType = record.Type;
            Logger.Info("sType is: " + sType);
            Logger.Debug("record.Changes is: " + record.Changes);

            if (_assembly == null)
            {
                initAssembly();
            }

            Type entryType = getTypeFromTypeName(sType, _assembly);

            string sId = parseIdStringFromUid(record.Uid);
            object templateQuery = createIdTemplate(sType, sId);
            
            // create the changeSet
            ChangeSet changeSet = ChangeContentParser.parse(record.Changes, entryType);

            IQuery<SpaceDocument> idQuery = getIdQuery(sType, sId);
            IChangeResult<object> changeResult = _proxy.Change<object>(idQuery, changeSet);

            //IChangeResult<object> changeResult = _proxy.Change<object>(templateQuery, changeSet);
            if (changeResult == null)
            {
                Logger.Info("changeResult is null.");
            }
            else
            {
                Logger.Debug("number of changed entries is for {0}, id {1} is: {2}", sType, sId, changeResult.NumberOfChangedEntries);
            }

        }
        
        private IQuery<SpaceDocument> getIdQuery(string sType, string sId)
        {
            if (_assembly == null)
            {
                initAssembly();
            }
            var entry = _assembly.CreateInstance(sType);
            Type entryType = getTypeFromTypeName(sType, _assembly);

            ISpaceTypeDescriptor spaceTypeDescriptor = registerAndGetTypeDescriptor(entryType);

            string idPropertyName = spaceTypeDescriptor.IdPropertyName;
            PropertyInfo idProperty = entryType.GetProperty(idPropertyName);
            object idPropertyValue = convertStringToObject(idProperty.PropertyType, sId);

            return new IdQuery<SpaceDocument>(sType, idPropertyValue);
        }
        private object createIdTemplate(string sType, string sId)
        {
            if (_assembly == null)
            {
                initAssembly();
            }
            var entry = _assembly.CreateInstance(sType);
            Type entryType = getTypeFromTypeName(sType, _assembly);

            ISpaceTypeDescriptor spaceTypeDescriptor = registerAndGetTypeDescriptor(entryType);

            string idPropertyName = spaceTypeDescriptor.IdPropertyName;
            PropertyInfo idProperty = entryType.GetProperty(idPropertyName);
            Type idPropertyType = idProperty.PropertyType;
            idProperty.SetValue(entry, convertStringToObject(idProperty.PropertyType, sId));
            return entry;
        }
        private static string parseIdStringFromUid(string uid)
        {
            // TODO: need logic to check if SpaceId is autoGenerate=true
            // See: github.com/xap/xap SpaceUidFactory.java
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
                    // long in millis from Java epoch 1970-01-01
                    long longInMillis = long.Parse((String)fromValue);

                    TimeSpan timeSpan = TimeSpan.FromMilliseconds(longInMillis);

                    DateTime javaEpoch = new DateTime(1970, 01, 01);

                    return javaEpoch.Add(timeSpan);
                }
            }
            // it's either a string type or type not handled above 
            return fromValue;
        }
    }
}
