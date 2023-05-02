package com.gs;

import java.util.List;
import java.util.Map;

public class Record implements java.io.Serializable {

    private static final long serialVersionUID = 0L;

    public enum Operation {write, remove, change};

    public static String FIELD_SEPARATOR = "#";
    Operation opr;
    String type;
    List<Object> fixedProps;
    Map<String, Object> dynamicProps;
    String uid;
    String changes;


    public Record() {
    }

    public Record(Operation opr, String type, String uid) {
        this.opr = opr;
        this.type = type;
        this.uid = uid;
    }

    public Operation getOpr() {
        return opr;
    }

    public void setOpr(Operation opr) {
        this.opr = opr;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Object> getFixedProps() {
        return fixedProps;
    }

    public void setFixedProps(List<Object> fixedProps) {
        this.fixedProps = fixedProps;
    }

    public Map<String,Object> getDynamicProps() {
        return dynamicProps;
    }

    public void setDynamicProps(Map<String,Object> dynamicProps) {
        this.dynamicProps = dynamicProps;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getChanges() {
        return changes;
    }

    public void setChanges(String changes) {
        this.changes = changes;
    }


    public StringBuffer toStringBuffer() {
        StringBuffer stringBuffer = new StringBuffer(200);
        stringBuffer.append(opr);
        stringBuffer.append(FIELD_SEPARATOR);
        stringBuffer.append(type);
        stringBuffer.append(FIELD_SEPARATOR);
        stringBuffer.append(fixedProps);
        stringBuffer.append(FIELD_SEPARATOR);
        stringBuffer.append(dynamicProps);
        stringBuffer.append(FIELD_SEPARATOR);
        stringBuffer.append(uid);
        stringBuffer.append(FIELD_SEPARATOR);
        stringBuffer.append(changes);
        return stringBuffer;
    }
}
