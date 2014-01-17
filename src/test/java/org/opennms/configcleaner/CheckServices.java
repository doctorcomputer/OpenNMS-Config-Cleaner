package org.opennms.configcleaner;

import com.google.common.collect.Sets;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opennms.netmgt.config.PollerConfigFactory;
import org.opennms.netmgt.config.poller.Monitor;
import org.opennms.netmgt.config.poller.Package;
import org.opennms.netmgt.config.poller.PollerConfiguration;
import org.opennms.netmgt.config.poller.Service;
import org.opennms.netmgt.provision.persist.foreignsource.ForeignSource;
import org.opennms.netmgt.provision.persist.foreignsource.PluginConfig;
import org.opennms.netmgt.provision.persist.requisition.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class CheckServices {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckServices.class);
    private static final Object CONFIG_FOLDER = "/tmp/opennms/";
    private static final Map<RequisitionCategory, Integer> categoriesInRequisition = new TreeMap<>();
    private static final Map<RequisitionMonitoredService, Integer> servicesInRequisition = new HashMap<>();
    private static final Set<String> allServicesInRequisition = new TreeSet<>();
    private static final Set<String> detectingServices = new TreeSet<>();

    private PollerConfiguration pollerConfiguration;

    private List<Service> allNotMonitoredServices = new ArrayList<>();
    private List<Service> allMonitoredServices = new ArrayList<>();
    private Set<String> allDefinedServiceNames= new TreeSet<>();

    private List<Service> allDefinedServices = new ArrayList<>();
    private List<org.opennms.netmgt.config.poller.Package> packageCollection = new ArrayList<>();
    private List<Monitor> allDefinedMonitors = new ArrayList<>();

    @Before
    public void before() throws MarshalException, IOException, ValidationException, JAXBException {
        readRequisitions();
        readPollerConfiguration();
    }

    public void readRequisitions() throws JAXBException {
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
                if (categoriesInRequisition.containsKey(category)) {
                    categoriesInRequisition.put(category, categoriesInRequisition.get(category) + 1);
                } else {
                    categoriesInRequisition.put(category, 1);
                }
            }

            for (RequisitionInterface requisitionInterface : node.getInterfaces()) {
                for (RequisitionMonitoredService service : requisitionInterface.getMonitoredServices()) {
                    if (servicesInRequisition.containsKey(service)) {
                        servicesInRequisition.put(service, servicesInRequisition.get(service) +1);
                    } else {
                        servicesInRequisition.put(service, 1);
                    }
                    allServicesInRequisition.add(service.getServiceName());
                }
            }
        }
    }

    @Test
    public void allDefinedButNotInRequisitionsServices() {
        Sets.SetView<String> allDefinedButNotUsedServices = Sets.difference(allDefinedServiceNames, allServicesInRequisition);
        Sets.SetView<String> allUsedButNotDefinedServices = Sets.difference(allServicesInRequisition, allDefinedServiceNames);

        if (!allDefinedButNotUsedServices.isEmpty()) {
        LOGGER.error("This {} services are defined in pollerconfiguration but not used by the provisioning at all:", allDefinedButNotUsedServices.size());
            for (String service : allDefinedButNotUsedServices) {
                LOGGER.error("\t{}", service);
            }
        }

        if(!allUsedButNotDefinedServices.isEmpty()) {
            LOGGER.error("This {} services are used in Requisitions but are not defined in pollerconfiguration at all:", allUsedButNotDefinedServices.size());
            for (String service : allUsedButNotDefinedServices) {
                LOGGER.error("\t{}", service);
            }
        }
    }

    @Test
    public void everyCategoryUsedInRequisition() {
        LOGGER.info("All {} used Categories:", categoriesInRequisition.size());
        for (Map.Entry<RequisitionCategory, Integer> entry : categoriesInRequisition.entrySet()) {
            LOGGER.info("\t{} {}", entry.getValue(), entry.getKey().getName());
        }

        LOGGER.info("All {} used Services:", servicesInRequisition.size());
        for (Map.Entry<RequisitionMonitoredService, Integer> entry: servicesInRequisition.entrySet()) {
            LOGGER.info("\t{} {}",entry.getValue(), entry.getKey().getServiceName());
        }

        LOGGER.info("All {} detecting Services:", detectingServices.size());
        for (String service : detectingServices) {
            LOGGER.info("\t{}", service);
        }
    }

    public void readPollerConfiguration() throws MarshalException, IOException, ValidationException {
        PollerConfigFactory.init();
        PollerConfigFactory pollerConfigFactory = new PollerConfigFactory(0, new FileInputStream(new File(CONFIG_FOLDER + "poller-configuration.xml")),"LocalServer?",false);
        pollerConfiguration = pollerConfigFactory.getConfiguration();

        buildListsFromPollerConfiguraion();
    }

    private void buildListsFromPollerConfiguraion() {
        Map<String, Monitor> monitorsByServiceName = new HashMap<>();
        for (Monitor monitor : pollerConfiguration.getMonitorCollection()) {
            monitorsByServiceName.put(monitor.getService(), monitor);
        }

        packageCollection = pollerConfiguration.getPackageCollection();
        for (Package pollerPackage : packageCollection) {
            allDefinedServices.addAll(pollerPackage.getServiceCollection());
        }

        for (Service service : allDefinedServices) {
            allDefinedServiceNames.add(service.getName());
        }

        for (Service service : allDefinedServices) {
            if (monitorsByServiceName.containsKey(service.getName())) {
                allMonitoredServices.add(service);
            } else {
                allNotMonitoredServices.add(service);
            }
        }

        allDefinedMonitors = pollerConfiguration.getMonitorCollection();
    }

    @Test
    public void everyServiceHasAMonitor() {
        if (!allNotMonitoredServices.isEmpty()) {
            LOGGER.error("This Services have no Monitors associated");
            for (Service service : allNotMonitoredServices) {
                LOGGER.error("\t{}", service.getName());
            }
        }
        Assert.assertTrue(allNotMonitoredServices.isEmpty());
    }

    @Test
    public void everyMonitorHasAService() {
        List<Monitor> allMonitorsWithoutServices = new ArrayList<>();

        for (Monitor monitor : allDefinedMonitors) {
            if (!allDefinedServiceNames.contains(monitor.getService())) {
                allMonitorsWithoutServices.add(monitor);
            }
        }
        if (!allMonitorsWithoutServices.isEmpty()) {
            LOGGER.error("This Monitors have no Service associated");
            for (Monitor monitor : allMonitorsWithoutServices) {
                LOGGER.error("\t{} \t{}", monitor.getService(), monitor.getClassName());
            }
        }
        Assert.assertTrue(allMonitorsWithoutServices.isEmpty());
    }

}
