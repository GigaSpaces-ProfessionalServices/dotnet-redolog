using GigaSpaces.Core.Metadata;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReadRedoLogContentsTest.Common
{
    public class SpaceReplayEvent
    {
        private DateTime? _timeStamp;

        private long?_id;
        private string? _guid;

        public DateTime? TimeStamp
        {
            get { return _timeStamp; }
            set { _timeStamp = value; }
        }

        [SpaceID(AutoGenerate = false)]
        public long? Id
        {
            get { return _id; }
            set { _id = value; }
        }

        public string? Guid
        {
            get { return _guid; }
            set { _guid = value; }
        }

    }
}
