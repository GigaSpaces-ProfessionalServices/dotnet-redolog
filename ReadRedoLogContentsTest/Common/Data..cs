using System;
using GigaSpaces.Core.Metadata;

namespace ReadRedoLogContentsTest.Common
{
    /// <summary>
    /// Represnts a data object
    /// </summary>	
    public class Data
    {
        private long _id;
        private string _info;
        private Nullable<int> _type;
        private bool _processed;

        public Data()
        {
        }

        public Data(string info, Nullable<int> type)
        {
            _info = info;
            _type = type;
            _processed = false;
        }

        [SpaceID(AutoGenerate = false)]
        public long Id
        {
            get { return _id; }
            set { _id = value; }
        }

        /// <summary>
        /// Gets the data type, used as the routing index inside the cluster
        /// </summary>
        [SpaceRouting]
        public Nullable<int> Type
        {
            get { return _type; }
            set { _type = value; }
        }
        /// <summary>
        /// Gets the data info
        /// </summary>
        public string Info
        {
            get { return _info; }
            set { _info = value; }
        }
        /// <summary>
        /// Gets or sets the data processed state
        /// </summary>
        public bool Processed
        {
            get { return _processed; }
            set { _processed = value; }
        }

    }
}