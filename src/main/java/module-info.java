module nl.gertjanidema.netex.dataload {
    requires transitive chbhaltebestand;
    requires spring.data.jpa;
    requires org.locationtech.jts;
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
    requires lombok;
    requires spring.data.commons;
    requires org.entur.netex.java.model;
    requires jakarta.xml.bind;
}