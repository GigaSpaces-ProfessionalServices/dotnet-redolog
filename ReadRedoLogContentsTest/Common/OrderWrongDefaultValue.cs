using System;
using GigaSpaces.Core.Metadata;

namespace ReadRedoLogContentsTest.Common
{
    public class OrderWrongDefaultValue
    {
        // simulate not having the proper default value for the primitive set
        // code should not have to rely on template matching
        private long _id = 0L;
        private string _info;
        private long _calCumQty = 0L;
        private Nullable<Double> _calExecValue;
        
        
        public OrderWrongDefaultValue()
        {
        }

        [SpaceID(AutoGenerate = false)]
        [SpaceProperty(NullValue = -1)]
        public long Id
        {
            get { return _id; }
            set { _id = value; }
        }
        public string Info
        {
            get { return _info; }
            set { _info = value; }
        }
        [SpaceProperty(NullValue = -1)]
        public long CalCumQty
        {
            get { return _calCumQty; }
            set { _calCumQty = value; }
        }
        public double? CalExecValue
        {
            get { return _calExecValue; }
            set { _calExecValue = value; }
        }
    }
}