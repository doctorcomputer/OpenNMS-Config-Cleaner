package org.opennms.configcleaner;

import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opennms.netmgt.config.PollerConfigFactory;
import org.opennms.netmgt.config.poller.*;
import org.opennms.netmgt.config.poller.Package;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class CheckPollerConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckPollerConfiguration.class);
    private static final Object CONFIG_FOLDER = "/home/tak/Desktop/Leitner/C-opennms/";
    private PollerConfiguration pollerConfiguration;

    private List<Service> allNotMonitoredServices = new ArrayList<>();
    private List<Service> allMonitoredServices = new ArrayList<>();
    private Set<String> allDefinedServiceNames= new HashSet<>();

    private List<Service> allDefinedServices = new ArrayList<>();
    private List<Package> packageCollection = new ArrayList<>();
    private List<Monitor> allDefinedMonitors = new ArrayList<>();

    @Before
    public void before() throws MarshalException, IOException, ValidationException {
        PollerConfigFactory.init();
        PollerConfigFactory pollerConfigFactory = new PollerConfigFactory(0, new FileInputStream(new File(CONFIG_FOLDER + "poller-configuration.xml")),"LocalServer?",false);
        pollerConfiguration = pollerConfigFactory.getConfiguration();

        buildLists();
    }

    private void buildLists() {
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
