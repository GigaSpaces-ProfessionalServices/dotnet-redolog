using GigaSpaces.Core;
using NLog;
using System.Text.RegularExpressions;


namespace ReadRedoLogContents
{
    public class ChangeContentParser
    {
        private static readonly NLog.Logger Logger = NLog.LogManager.GetCurrentClassLogger();

        private const string CHANGE_EXPRESSION = @"^([a-zA-Z]+)\[(.+?)\]";
        //                                           ^ captures ChangeSet type
        //                                                       ^ captures path and change value

        private const string PATH_DELTA_EXPRESSION = @"^path=([a-zA-Z]{1}[a-zA-Z0-9_]*),delta=(.+)";
        //                                                   ^ captures path, a valid property name starts with a letter and can include numbers and underscore after
        //                                                                                    ^ captures delta

        public static ChangeSet parse(string content, Type entryType)
        {
            ChangeSet changeSet = new ChangeSet();

            if (content.StartsWith("[") && content.EndsWith("]"))
            {
                // remove enclosing [] (brackets)
                content = content.Substring(1, content.Length - 1);
                Regex regex = new Regex(CHANGE_EXPRESSION, RegexOptions.Compiled);
                Match m = regex.Match(content);

                // loop through changes
                // this may be simpler to read and maintain than a complicated regular expression
                while (m.Success)
                {
                    GroupCollection groups = m.Groups;
                    string changeType = groups[1].Value;
                    string changeDetails = groups[2].Value;
                    string item = m.Groups[0].Value; // the whole match
                    Logger.Debug("item is: \"{0}\"", item);
                    Logger.Debug("changeType is: {0}, changeDetails is: {1}", changeType, changeDetails);
                    content = content.Substring(item.Length);
                    addToChangeSet(changeSet, changeType, changeDetails, entryType);

                    if (content.StartsWith(", "))
                    {
                        // remove the comma separating ChangeSet
                        content = content.Substring(", ".Length);
                        // prepare to process the next changeset
                        Logger.Debug("remainder is: \"{0}\"", content);

                        m = regex.Match(content);
                    }
                    else
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

        private static List<string> parseIncrementPathAndDelta(string changeDetails)
        {
            List<string> result = new List<string>();

            Regex regex = new Regex(PATH_DELTA_EXPRESSION, RegexOptions.Compiled);
            Match m = regex.Match(changeDetails);

            if (m.Success)
            {
                result.Add(m.Groups[1].Value);
                result.Add(m.Groups[2].Value);
            }
            return result;
        }
        private static void addToChangeSet(ChangeSet changeSet, string changeType, string changeDetails, Type entryType)
        {

            // TODO: Test other change operations
            if (changeType.Equals("IncrementSpaceEntryMutator"))
            {
                List<string> list = parseIncrementPathAndDelta(changeDetails);

                string path = list[0];
                string delta = list[1];
                Logger.Debug("path is: {0}", path);
                Logger.Debug("delta is: {0}", delta);
                Type toType = SpaceReplay.getPropertyType(entryType, path);
                var objValue = SpaceReplay.parseStringToObject(toType, delta);

                if (toType == typeof(byte))
                {
                    changeSet.Increment(path, (byte)objValue);
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
