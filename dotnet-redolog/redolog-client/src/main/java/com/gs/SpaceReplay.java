package com.gs;

import com.gigaspaces.client.ChangeModifiers;
import com.gigaspaces.client.ChangeSet;
import com.gigaspaces.client.TakeModifiers;
import com.gigaspaces.client.mutators.SpaceEntryMutator;
import com.gigaspaces.internal.client.QueryResultTypeInternal;
import com.gigaspaces.internal.metadata.EntryType;
import com.gigaspaces.internal.metadata.ITypeDesc;
import com.gigaspaces.internal.metadata.PojoIntrospector;
import com.gigaspaces.internal.server.space.SpaceUidFactory;
import com.gigaspaces.internal.server.storage.IEntryData;
import com.gigaspaces.internal.transport.EntryPacket;
import com.gigaspaces.internal.transport.EntryPacketFactory;
import com.gigaspaces.internal.transport.IEntryPacket;
import com.gigaspaces.metadata.SpaceTypeDescriptor;
import com.gigaspaces.query.IdQuery;
import com.j_spaces.core.OperationID;
import com.mycompany.app.model.Person;
import com.mycompany.app.model.Product;
import net.jini.core.entry.Entry;
import net.jini.core.entry.UnusableEntryException;
import net.jini.core.transaction.TransactionException;
import org.openspaces.core.GigaSpace;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Iterator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import com.gigaspaces.metadata.SpacePropertyDescriptor;



public class SpaceReplay {
    GigaSpace gs;

    public SpaceReplay(GigaSpace gs) {
        this.gs = gs;
        //ToDo - Assuming type exists in target space otherwise register all types before start
        gs.getTypeManager().registerTypeDescriptor(Product.class);
        gs.getTypeManager().registerTypeDescriptor(Person.class);
    }


    public void write(String type, IEntryData entry, OperationID operationID, String uid){
        SpaceTypeDescriptor typeDescriptor = gs.getTypeManager().getTypeDescriptor(type);
        PojoIntrospector pojoIntrospector = new PojoIntrospector<>((ITypeDesc) typeDescriptor);
        Object target = pojoIntrospector.newInstance();
        pojoIntrospector.setValues(target, entry.getFixedPropertiesValues());
        pojoIntrospector.setUID(target, uid);
        gs.write(target);

    }

    public void remove(String type, String uid) throws UnusableEntryException, TransactionException, RemoteException, InterruptedException {
        gs.getSpace().getDirectProxy().takeByUid(uid, null, TakeModifiers.NONE.getCode(), QueryResultTypeInternal.OBJECT_JAVA, false);
    }

    public void change(String type, String uid, Collection<SpaceEntryMutator> mutators) throws TransactionException, RemoteException {
        ChangeSet changeSet = new ChangeSet();
        SpaceTypeDescriptor typeDescriptor = gs.getTypeManager().getTypeDescriptor(type);
        PojoIntrospector pojoIntrospector = new PojoIntrospector<>((ITypeDesc) typeDescriptor);
        Object target = pojoIntrospector.newInstance();
        pojoIntrospector.setUID(target, uid);
        Iterator<SpaceEntryMutator> iterator = mutators.iterator();
        while (iterator.hasNext()) {
            SpaceEntryMutator mutator = iterator.next();
            changeSet.custom(mutator);
        }

        String id = SpaceUidFactory.getIdStringFromUID(SpaceUidFactory.generateTypePrefix(type), uid);
        System.out.println("About to change object type: "+ type + " uid: " +uid +"id: "+ id);
        //ToDo some limitaion on id type if id type is not supported need special care of it
        IdQuery<Object> idQuery = new IdQuery(pojoIntrospector.getType(),createIdFromIdString(id, typeDescriptor));
        Object result = gs.change(idQuery, changeSet);
        gs.change(idQuery, changeSet);
    }

    protected Object createIdFromIdString(String id, SpaceTypeDescriptor typeDescriptor){
        List<String> idPropertiesNames = typeDescriptor.getIdPropertiesNames();
        if (idPropertiesNames.size() > 1)
            throw new IllegalArgumentException("Only single SpaceId is currently supported by redolog processor");

        String idPropertyName = idPropertiesNames.get(0);
        SpacePropertyDescriptor idProperty = typeDescriptor.getFixedProperty(idPropertyName);
        return convertIdStringToObject(id, idProperty.getType(), idPropertyName);
    }



    public  Object convertIdStringToObject(String object, Class type, String propKey) {
        if (type.equals(Long.class) || type.equals(Long.TYPE))
            return Long.valueOf(object);

        if (type.equals(Boolean.class) || type.equals(Boolean.TYPE))
            return Boolean.valueOf(object);

        if (type.equals(Integer.class) || type.equals(Integer.TYPE))
            return Integer.valueOf(object);

        if (type.equals(Byte.class) || type.equals(Byte.TYPE))
            return Byte.valueOf(object);

        if (type.equals(Short.class) || type.equals(Short.TYPE))
            return Short.valueOf(object);

        if (type.equals(Float.class) || type.equals(Float.TYPE))
            return Float.valueOf(object);

        if (type.equals(Double.class) || type.equals(Double.TYPE))
            return Double.valueOf(object);

        if (type.isEnum())
            return Enum.valueOf(type, object);

        if (type.equals(String.class) || type.equals(Object.class))
            return String.valueOf(object);

        if (type.equals(java.util.Date.class)) {
            String dateformat = "yyyy-MM-dd HH:mm:ss";
            try {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat();
                return simpleDateFormat.parse(object);
            } catch (ParseException e) {
                throw new IllegalArgumentException("Change replay- Unable to parse date [" + object + "]. Make sure it matches the format: " + dateformat);
            }
        }

        //unknown type
        throw new IllegalArgumentException("Change replay- Non primitive type when converting property [" + propKey + "]:" + type);
    }

}
