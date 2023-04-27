package com.gs;

import com.gigaspaces.client.ChangeSet;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.SpaceProxyConfigurer;
import com.mycompany.app.model.Product;
import com.mycompany.app.model.Person;
import com.mycompany.app.model.MultiplyIntegerChangeOperation;
import org.openspaces.core.transaction.manager.DistributedJiniTxManagerConfigurer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.ArrayList;

public class Feeder {
    static final int amount = 10;
    GigaSpace gs;
    public static void main(String[] args) throws Exception {

        String spaceName = "redolog";
        if (args.length >= 1) {
            spaceName = args[0];
        }
        PlatformTransactionManager ptm = new DistributedJiniTxManagerConfigurer().transactionManager();
        GigaSpace gs = new GigaSpaceConfigurer(new SpaceProxyConfigurer(spaceName)).transactionManager(ptm).gigaSpace();
        Feeder feeder = new Feeder(gs);
        feeder.writeTransaction(gs,ptm);
        feeder.writeData(gs);
        feeder.writeTransaction2(gs,ptm);
        //feeder.changeData(gs); //- custom change issue
        System.out.println("end writing");
    }

    public Feeder(GigaSpace gs) {
        this.gs = gs;
    }

    public void writeTransaction(GigaSpace gs,PlatformTransactionManager ptm){
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        definition.setPropagationBehavior(Propagation.REQUIRES_NEW.ordinal());
        TransactionStatus status = ptm.getTransaction(definition);
        try {
            gs.write(new Person(1,"Lev", "Ron",20));
            gs.write(new Person(2,"Levi", "Dan",40));
        }
        catch (Throwable e) {
            e.printStackTrace();
            ptm.rollback(status);
            throw e;
        }
        ptm.commit(status);

    }

    public void writeTransaction2(GigaSpace gs,PlatformTransactionManager ptm){
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        definition.setPropagationBehavior(Propagation.REQUIRES_NEW.ordinal());
        TransactionStatus status = ptm.getTransaction(definition);
        try {
            gs.write(new Product(7, "coffee-machine-2", 77));
            gs.takeById(Product.class, 2);
        }
        catch (Throwable e) {
            e.printStackTrace();
            ptm.rollback(status);
            throw e;
        }
        ptm.commit(status);
    }

    public void writeData(GigaSpace gs) {
        ArrayList<Product> products = new ArrayList<>(amount);

        products.add(new Product(1, "Train", 1));
        products.add(new Product(2, "Doll", 2));
        products.add(new Product(3, "Chocolate", 3));
        products.add(new Product(4, "Kandy", 4));
        products.add(new Product(5, "Basketball", 5));
        products.add(new Product(6, "Football", 6));
        products.add(new Product(7, "coffee-machine", 7));
        products.add(new Product(8, "Ice-maker", 8));
        products.add(new Product(1, "Train", 11));

        products.forEach(p -> gs.write(p));
        Product changeTemplate = new Product();
        changeTemplate.setId(1);
        gs.change(changeTemplate, new ChangeSet().increment("price", 2));
        gs.takeById(Product.class, 5);

    }

    public void changeData(GigaSpace gs){
        Product changeTemplate2 = new Product();
        changeTemplate2.setId(3);
        gs.change(changeTemplate2, new ChangeSet().custom(new MultiplyIntegerChangeOperation("price", 2)));;
    }
}
