package org.opennms.configcleaner;

import java.util.Map;
import java.util.TreeMap;
import org.opennms.configcleaner.model.ServiceCompound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Starter {

    private final static Logger LOGGER = LoggerFactory.getLogger(Starter.class);
    private final static String CONFIG_FOLDER = "/tmp/opennms/";

    public static void main(String[] args) throws Exception {
        LOGGER.debug("Hello World");
        
        Map<String, ServiceCompound> serviceCompounds = new TreeMap<>();
        
        PollerChecker pollerChecker = new PollerChecker(CONFIG_FOLDER);
        CollectionChecker collectionChecker = new CollectionChecker(CONFIG_FOLDER);
        RequisitionChecker requisitionChecker = new RequisitionChecker(CONFIG_FOLDER);
        
        serviceCompounds = pollerChecker.updateServiceCompounds(serviceCompounds);
        serviceCompounds = collectionChecker.updateServiceCompounds(serviceCompounds);
        serviceCompounds = requisitionChecker.updateServiceCompounds(serviceCompounds);
        
        System.out.println("------------------------- COPY ME -------------------------");
        System.out.println(ServiceCompound.getTABEL_HEADER());
        for (Map.Entry<String, ServiceCompound> entry : serviceCompounds.entrySet()) {
            System.out.println(entry.getValue().toFormatedString());
        }
        System.out.println("------------------------- All "+serviceCompounds.size()+" ServiceCompounds DONE -------------------------");
    }

}
