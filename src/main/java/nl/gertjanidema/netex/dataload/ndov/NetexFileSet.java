package nl.gertjanidema.netex.dataload.ndov;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import nl.gertjanidema.netex.dataload.dto.NetexFileInfo;

public class NetexFileSet {
    private String ndovSourceId;
    private String fileSetId;
    private List<NetexFileInfo> fileInfo = new ArrayList<>();
    
    public NetexFileSet(String ndovSourceId, String fileSetId) {
        super();
        this.ndovSourceId = ndovSourceId;
        this.fileSetId = fileSetId;
    }

    public String getFileSetId() {
        return fileSetId;
    }

    public List<NetexFileInfo> getFileInfo() {
        return fileInfo;
    }

    /**
     * Get the newest file in this fileSet
     * 
     * @return
     */
    public NetexFileInfo getNewest() {
        return fileInfo.stream().max(new NewestFileComparator()).get();
    }
    
    public void addFile(NetexFileInfo info) {
        fileInfo.add(info);
    }
    
    private class NewestFileComparator implements Comparator<NetexFileInfo> {
        @Override
        public int compare(NetexFileInfo nfi1, NetexFileInfo nfi2) {
            return nfi1.getFileName().compareTo(nfi2.getFileName());
        }
    }
}
