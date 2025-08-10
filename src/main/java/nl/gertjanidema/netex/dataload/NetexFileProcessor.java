package nl.gertjanidema.netex.dataload;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import org.rutebanken.netex.model.CompositeFrame;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.Line_VersionStructure;
import org.rutebanken.netex.model.LinkSequence_VersionStructure;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.ResourceFrame;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.ScheduledStopPoint;
import org.rutebanken.netex.model.ServiceFrame;
import org.rutebanken.netex.model.SiteFrame;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.TypeOfProductCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import nl.gertjanidema.netex.dataload.dto.NetexFileInfo;
import nl.gertjanidema.netex.dataload.dto.StNetexDelivery;
import nl.gertjanidema.netex.dataload.dto.StNetexLineRepository;
import nl.gertjanidema.netex.dataload.dto.StNetexPointOnJourneyRepository;
import nl.gertjanidema.netex.dataload.dto.StNetexPointOnRouteRepository;
import nl.gertjanidema.netex.dataload.dto.StNetexProductCategoryRepository;
import nl.gertjanidema.netex.dataload.dto.StNetexQuayRepository;
import nl.gertjanidema.netex.dataload.dto.StNetexResponsibleAreaRepository;
import nl.gertjanidema.netex.dataload.dto.StNetexRouteRepository;
import nl.gertjanidema.netex.dataload.dto.StNetexScheduledStopPointRepository;
import nl.gertjanidema.netex.dataload.dto.StNetexStopPlace;
import nl.gertjanidema.netex.dataload.dto.StNetexStopPlaceRepository;
import nl.gertjanidema.netex.dataload.processors.NetexDeliveryProcesser;
import nl.gertjanidema.netex.dataload.processors.NetexLineProcessor;
import nl.gertjanidema.netex.dataload.processors.NetexPointOnRouteProcessor;
import nl.gertjanidema.netex.dataload.processors.NetexProductCategoryProcessor;
import nl.gertjanidema.netex.dataload.processors.NetexQuayProcessor;
import nl.gertjanidema.netex.dataload.processors.NetexRouteProcessor;
import nl.gertjanidema.netex.dataload.processors.NetexScheduledStopPointProcessor;
import nl.gertjanidema.netex.dataload.processors.NetexStopPlaceProcessor;

@Component
public class NetexFileProcessor {
    private static Logger LOG = LoggerFactory.getLogger(NetexFileProcessor.class);

    private PublicationDeliveryStructure delivery;
    private StNetexDelivery stDelivery;

    @Inject
    StNetexQuayRepository quayRepository;
    
    @Inject
    StNetexStopPlaceRepository stopPlaceRepository;
    
    @Inject
    StNetexLineRepository lineRepository;
    
    @Inject
    StNetexScheduledStopPointRepository scheduledStopPointRepository;
    
    @Inject
    StNetexRouteRepository routeRepository;
    
    @Inject
    StNetexPointOnRouteRepository pointOnRouteRepository;
    
    @Inject
    StNetexPointOnJourneyRepository pointOnJourneyRepository;
    
    @Inject
    StNetexResponsibleAreaRepository responsibleAreaRepository;
    
    @Inject
    StNetexProductCategoryRepository productCategoryRepository;

    public StNetexDelivery processHeader(NetexFileInfo fileInfo) {
        delivery = readFile(fileInfo.getCachedFile());
        stDelivery = NetexDeliveryProcesser.process(delivery, fileInfo);
        return stDelivery;
    }
    
    public void processData() {
        lineRepository.deleteByFileSetId(stDelivery.getFileSetId());
        delivery.getDataObjects().getCompositeFrameOrCommonFrame().forEach(frameStructure -> {
            if (frameStructure.getDeclaredType().equals(CompositeFrame.class)) {
                processCompositeFrame((CompositeFrame) frameStructure.getValue());
            }
        });
    }

    private void processCompositeFrame(CompositeFrame compositeFrame) {
        compositeFrame.getFrames().getCommonFrame().forEach(commonFrame -> {
            if (commonFrame.getDeclaredType().equals(ResourceFrame.class)) {
                processResourceFrame((ResourceFrame)commonFrame.getValue());
            }
            else if (commonFrame.getDeclaredType().equals(ServiceFrame.class)) {
                processServiceFrame((ServiceFrame)commonFrame.getValue());
            }
            else if (commonFrame.getDeclaredType().equals(SiteFrame.class)) {
                processSiteFrame((SiteFrame)commonFrame.getValue());
            }
        });
    }

    private void processResourceFrame(ResourceFrame frame) {
        if (frame.getTypesOfValue() != null) {
            frame.getTypesOfValue().getValueSetOrTypeOfValue().forEach(element -> {
                if (element.getDeclaredType().equals(TypeOfProductCategory.class)) {
                    processProductCategory((TypeOfProductCategory)element.getValue());
                }
            });
        }
    }

    private void processProductCategory(TypeOfProductCategory productCategory) {
        try {
            var netexProductCategory = NetexProductCategoryProcessor.process(productCategory);
            netexProductCategory.setFileSetId(stDelivery.getFileSetId());
            productCategoryRepository.save(netexProductCategory);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void processServiceFrame(ServiceFrame frame) {
        frame.getLines().getLine_().stream().map(JAXBElement::getValue).map(Line_VersionStructure.class::cast)
            .forEach(line -> {
                if (line instanceof Line) {
                    processLine((Line) line);
                }
                else LOG.info("Unprocessed line type: {}", line.getClass().getName());
            });
        frame.getScheduledStopPoints().getScheduledStopPoint().stream().forEach(this::processScheduledStopPoint);
        frame.getRoutes().getRoute_().stream().map(JAXBElement::getValue).map(LinkSequence_VersionStructure.class::cast)
            .forEach(linkSequence -> {
                if (linkSequence instanceof Route) {
                    processRoute((Route) linkSequence);
                }
                else LOG.info("Unprocessed route type: {}", linkSequence.getClass().getName());
            });
    }

    private void processScheduledStopPoint(ScheduledStopPoint stopPoint) {
        try {
            var netexStopPoint = NetexScheduledStopPointProcessor.process(stopPoint);
            netexStopPoint.setFileSetId(stDelivery.getFileSetId());
            scheduledStopPointRepository.save(netexStopPoint);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private void processRoute(Route route) {
        try {
            var netexRoute = NetexRouteProcessor.process(route);
            netexRoute.setFileSetId(stDelivery.getFileSetId());
            routeRepository.save(netexRoute);
            var pointsOnRoute = NetexPointOnRouteProcessor.process(route);
            pointsOnRoute.forEach(point->point.setFileSetId(stDelivery.getFileSetId()));
            pointOnRouteRepository.saveAll(pointsOnRoute);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private void processLine(Line line) {
        try {
            var netexLine = NetexLineProcessor.process(line);
            netexLine.setFileSetId(stDelivery.getFileSetId());
            lineRepository.save(netexLine);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private void processSiteFrame(SiteFrame frame) {
        frame.getStopPlaces().getStopPlace_().stream()
            .map(JAXBElement::getValue)
            .map(StopPlace.class::cast)
            .forEach(this::processStopPlace);
    }
    
    private void processQuay(Quay quay, StNetexStopPlace netexStopPlace) {
        try {
            var netexQuay = NetexQuayProcessor.process(quay, netexStopPlace);
            netexQuay.setFileSetId(stDelivery.getFileSetId());
            quayRepository.save(netexQuay);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private void processStopPlace(StopPlace stopPlace) {
        try {
            var netexStopPlace = NetexStopPlaceProcessor.process(stopPlace);
            netexStopPlace.setFileSetId(stDelivery.getFileSetId());
            stopPlaceRepository.save(netexStopPlace);
            stopPlace.getQuays().getQuayRefOrQuay().forEach(quayRefOrQuay -> {
                if (quayRefOrQuay.getDeclaredType() == Quay.class) {
                    processQuay((Quay) quayRefOrQuay.getValue(), netexStopPlace);
                }
            });
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private static PublicationDeliveryStructure readFile(File netexFile) {
        try (
            var is = new FileInputStream(netexFile);
            var streamReader = new GZIPInputStream(is);
        ) {
            var context = JAXBContext.newInstance(PublicationDeliveryStructure.class);
            @SuppressWarnings("unchecked")
            var delivery = ((JAXBElement<PublicationDeliveryStructure>)context.createUnmarshaller()
                    .unmarshal(streamReader))
                    .getValue();
            return delivery;
        } catch (JAXBException | IOException e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
    }
    
    public StNetexDelivery getStDelivery() {
        return stDelivery;
    }
}
