package nl.gertjanidema.netex.dataload.processors;

import org.rutebanken.netex.model.LimitationStatusEnumeration;
import org.rutebanken.netex.model.Line;
import org.springframework.batch.item.ItemProcessor;

import nl.gertjanidema.netex.dataload.dto.StNetexLine;

public class NetexLineProcessor implements ItemProcessor<Line, StNetexLine> {
 
    @Override
    public StNetexLine process(Line line) throws Exception {
        line.getResponsibilitySetRef();
        var netexLine = new StNetexLine();
        netexLine.setId(line.getId());
        netexLine.setResponsibilitySetRef(line.getResponsibilitySetRef());
        netexLine.setName(line.getName() != null ? line.getName().getValue() : null);
        netexLine.setBrandingRef(getBrandingRef(line));
        netexLine.setTransportMode(getTransportMode(line));
        netexLine.setPublicCode(line.getPublicCode());
        netexLine.setPrivateCode(getPrivateCode(line));
        var presentation = line.getPresentation();
        if (presentation != null) {
            netexLine.setColour(colourToString(presentation.getColour()));
            netexLine.setTextColour(colourToString(presentation.getTextColour()));
        }
        netexLine.setMobilityImpairedAccess(getMobilityImpairedAccess(line));
        var productCategoryRef = line.getTypeOfProductCategoryRef();
        if (productCategoryRef != null) {
            netexLine.setProductCategoryRef(line.getTypeOfProductCategoryRef().getRef());
        }
        return netexLine;
    }
    
    private static String getBrandingRef(Line line) {
        if (line.getBrandingRef() == null) return null;
        return line.getBrandingRef().getRef();
    }

    private static String getTransportMode(Line line) {
        if (line.getTransportMode() == null) return null;
        return line.getTransportMode().value();
    }

    private static String getPrivateCode(Line line) {
        if (line.getPrivateCode() == null) return null;
        return line.getPrivateCode().getValue();
    }
    
    private static Boolean getMobilityImpairedAccess(Line line) {
        if (line.getAccessibilityAssessment() == null) return null;
        if (line.getAccessibilityAssessment().getMobilityImpairedAccess() == null) return null;
        return line.getAccessibilityAssessment().getMobilityImpairedAccess().equals(LimitationStatusEnumeration.TRUE);
    }
    
    private static String colourToString(byte[] colour) {
        if (colour == null) return null;
        var sb = new StringBuilder();
        for (byte b : colour) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}