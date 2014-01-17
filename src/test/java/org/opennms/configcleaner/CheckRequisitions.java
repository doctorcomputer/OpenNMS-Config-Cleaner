package org.opennms.configcleaner;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opennms.netmgt.provision.persist.foreignsource.ForeignSource;
import org.opennms.netmgt.provision.persist.foreignsource.PluginConfig;
import org.opennms.netmgt.provision.persist.requisition.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.*;

public class CheckRequisitions {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckRequisitions.class);
    private static final Object CONFIG_FOLDER = "/home/tak/Desktop/Leitner/B-opennms/";
    private static final Map<RequisitionCategory, Integer> categories = new TreeMap<>();
    private static final Map<RequisitionMonitoredService, Integer> services = new HashMap<>();
    private static final Set<String> detectingServices = new TreeSet<>();

    @Before
    public void before() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(Requisition.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

        File importsFolder = new File(CONFIG_FOLDER + "imports/");

        for(File file : importsFolder.listFiles()) {
            if (file.isFile()) {
                LOGGER.info("Reading file {}", file.getName());
                Requisition requisition = (Requisition) jaxbUnmarshaller.unmarshal(file);
                buildListsFromRequisition(requisition);
            }
        }

        jaxbContext = JAXBContext.newInstance(ForeignSource.class);
        jaxbUnmarshaller = jaxbContext.createUnmarshaller();

        importsFolder = new File(CONFIG_FOLDER + "foreign-sources/");

        for(File file : importsFolder.listFiles()) {
            if (file.isFile()) {
                LOGGER.info("Reading file {}", file.getName());
                ForeignSource foreignSource = (ForeignSource) jaxbUnmarshaller.unmarshal(file);
                buildListsFromForeignSource(foreignSource);
            }
        }
    }

    private void buildListsFromForeignSource(ForeignSource foreignSource) {
        for (PluginConfig detector : foreignSource.getDetectors()) {
            detectingServices.add(detector.getName() + "\t" + detector.getPluginClass());
        }
    }

    public void buildListsFromRequisition(Requisition requisition) {
        for (RequisitionNode node : requisition.getNodes()) {
            for (RequisitionCategory category : node.getCategories()) {
                if (categories.containsKey(category)) {
                    categories.put(category, categories.get(category) + 1);
                } else {
                    categories.put(category, 1);
                }
            }

            for (RequisitionInterface requisitionInterface : node.getInterfaces()) {
                for (RequisitionMonitoredService service : requisitionInterface.getMonitoredServices()) {
                    if (services.containsKey(service)) {
                        services.put(service, services.get(service) +1);
                    } else {
                        services.put(service, 1);
                    }
                }
            }
        }
    }

    @Test
    public void everyCategoryUsedInRequisition() {
        LOGGER.info("All {} used Categories:", categories.size());
        for (Map.Entry<RequisitionCategory, Integer> entry : categories.entrySet()) {
            LOGGER.info("\t{} {}", entry.getValue(), entry.getKey().getName());
        }

        LOGGER.info("All {} used Services:", services.size());
        for (Map.Entry<RequisitionMonitoredService, Integer> entry: services.entrySet()) {
            LOGGER.info("\t{} {}",entry.getValue(), entry.getKey().getServiceName());
        }

        LOGGER.info("All {} detecting Services:", detectingServices.size());
        for (String service : detectingServices) {
            LOGGER.info("\t{}", service);
        }
        Assert.assertTrue(false);
    }
}
