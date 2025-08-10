module nl.gertjanidema.netex.dataload {
    requires spring.data.jpa;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires nl.gertjanidema.netex.core;
    requires org.slf4j;
    requires spring.batch.core;
    requires spring.beans;
    requires spring.boot;
    requires spring.batch.infrastructure;
    requires spring.oxm;
    requires spring.tx;
    requires jakarta.inject;
    requires jakarta.persistence;
    requires jakarta.annotation;
    requires lombok;
    requires spring.data.commons;
    requires org.entur.netex.java.model;
    requires jakarta.xml.bind;
    requires commons.net;
    
    exports nl.gertjanidema.netex.dataload.cli;
    
    opens nl.gertjanidema.netex.dataload to spring.core, spring.beans, spring.context;
    opens nl.gertjanidema.netex.dataload.cli to spring.core, spring.beans, spring.context;
    opens nl.gertjanidema.netex.dataload.ndov to spring.core, spring.beans, spring.context;
    opens nl.gertjanidema.netex.dataload.jobs to spring.core, spring.beans, spring.context;
    opens nl.gertjanidema.netex.dataload.dto;
}