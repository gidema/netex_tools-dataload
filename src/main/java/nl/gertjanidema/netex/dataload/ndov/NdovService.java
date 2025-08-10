package nl.gertjanidema.netex.dataload.ndov;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.function.Predicate.not;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import nl.gertjanidema.netex.dataload.dto.NetexFileInfo;
import nl.gertjanidema.netex.dataload.dto.StNetexDelivery;
import nl.gertjanidema.netex.dataload.dto.StNetexDeliveryRepository;

@Component
public class NdovService {
    
    private static Pattern fileNamePattern = Pattern.compile("(NeTEx_)?(.+?)_(20\\d{6})(.+)");

    private static Logger LOG = LoggerFactory.getLogger(NdovService.class);

    @Value("${ndov.server.ftp}")
    private String FTP_SERVER;

    @Value("${ndov.server.usesftp}")
    private boolean useSftp = true;
    
    @Value("${ndov.username}")
    private String username;

    @Value("${ndov.password}")
    private String password;
    
    @Value("${osm_netex.path.temp}")
    private Path tempPath;
    
    @Inject
    StNetexDeliveryRepository deliveryRepository;

    private Map<String, NdovSource> sources = new HashMap<>(32);

    public Path getNetexTempPath() {
        return tempPath.resolve("netex");
    }

    public Path getChbTempPath() {
        return tempPath.resolve("chb");
    }
    
    @PostConstruct
    public void initialize() {
        this.initializeNetexFolders();
        this.initializeFileInfo();
    }

/**
     * Initialize the NeTeX context. Create temporary folders if necessary and clear
     * any old temporary files.
     */
    private void initializeNetexFolders() {
        var folder = getNetexTempPath().toFile();
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }
    
    /**
     * Initialize the CHB context. Create temporary folders if necessary and clear
     * any old temporary files.
     */
    public void initializeChb() {
        var folder = getChbTempPath().toFile();
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    public FTPClient connect() throws IOException {
        FTPClient ftpClient;
        if (useSftp) {
            ftpClient = new FTPSClient();
        }
        else {
            ftpClient = new FTPClient();
        }
        ftpClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
        ftpClient.connect(InetAddress.getByName(FTP_SERVER));
        ftpClient.login(username, password);
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        return ftpClient;
    }

    private static List<String> getNetexNdovSourceFolders(FTPClient ftpClient) {
        List<String> ndovSourceFolders;  
        try {
            ftpClient.changeWorkingDirectory("netex");
            ftpClient.enterLocalPassiveMode();
            FTPFile[] folders = ftpClient.listDirectories();
            ndovSourceFolders = new ArrayList<>(folders.length);
            for (FTPFile folder : folders) {
                ndovSourceFolders.add(folder.getName());
            }
        } catch (@SuppressWarnings("unused") IOException e) {
            return Collections.emptyList();
        }
        return ndovSourceFolders;
    }

    public List<String> getChbFiles() {
        List<String> files;
        FTPClient ftpClient = null;
        try {
            ftpClient = connect();
            ftpClient.changeWorkingDirectory("haltes");
            FTPFile[] ftpFiles = ftpClient.listFiles();
            files = new ArrayList<>(ftpFiles.length);
            for (FTPFile file : ftpFiles) {
                files.add(file.getName());
            }
        } catch (@SuppressWarnings("unused") IOException e) {
            return Collections.emptyList();
        } finally {
            close(ftpClient);
        }
        return files;
    }
    
    private static void initializeFileSets(FTPClient ftpClient, NdovSource source) throws IOException {
        for (FTPFile ftpFile :  ftpClient.listFiles(source.getNdovFolder())) {
            // Get the fileSetId from the filename
            Matcher m = fileNamePattern.matcher(ftpFile.getName());
            if (!m.matches()) {
                LOG.warn("Unexpected filename: %s", ftpFile.getName());
                continue;
            }
            var fileSetId = m.toMatchResult().group(2);
            var fileSet = source.getFileSet(fileSetId);
            var fileInfo = new NetexFileInfo();
            fileInfo.setNdovSourceId(source.getId());
            fileInfo.setFileSetId(fileSetId);
            fileInfo.setFileName(ftpFile.getName());
            fileInfo.setLastModified(ftpFile.getTimestamp());
            fileInfo.setSize(ftpFile.getSize());
            fileSet.addFile(fileInfo);
        }
        return;
    }
    
    public Collection<NetexFileInfo> downloadNetexFiles(Collection<NetexFileInfo> netexFiles) {
        return downloadNetexFiles(netexFiles, true);
    }
    
    /**
     * Download the requested Netex files and save them to the cache folder.
     * @param netexFiles
     * @param useCache If true, always download the file. Overwrite the cached file if it exists.
     */
    public Collection<NetexFileInfo> downloadNetexFiles(Collection<NetexFileInfo> netexFiles, boolean useCache) {
        netexFiles.forEach(fileInfo -> {
            try {
                var cachedFile = new File(getNetexTempPath().toFile(), fileInfo.getFileName());
                fileInfo.setCachedFile(cachedFile);
                if (!cachedFile.exists() || !useCache) {
                    File sourceFile = new File(String.format("/netex/%s/%s", fileInfo.getNdovSourceId().toLowerCase(), fileInfo.getFileName()));
                    downloadFile(sourceFile , getNetexTempPath());
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
        return netexFiles;
    }

    private void downloadFile(File sourceFile, Path targetPath) throws IOException {
        var ftpClient = connect();
        ftpClient.enterLocalPassiveMode();
        var targetFile = new File(targetPath.toFile(), sourceFile.getName());
        try (FileOutputStream outputStream = new FileOutputStream(targetFile)) {
            var succes  = ftpClient.retrieveFile(sourceFile.getAbsolutePath(), outputStream);
            if (succes) {
                outputStream.close();
            }
            else {
                throw new FileNotFoundException(sourceFile.getName());
            }
        }
        finally {
            close(ftpClient);
        }
    }

    private static void close(FTPClient ftpClient) {
        if (ftpClient != null) {
            try {
                ftpClient.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public List<NetexFileInfo> checkForNewNetexFiles() throws IOException {
        var existingFilenames = getExistingFileNames();
        return getNewestNetexFiles().stream()
            .filter(fileInfo -> !existingFilenames.contains(fileInfo.getFileName()))
            .toList();
    }
    
    private Set<String> getExistingFileNames() {
        return StreamSupport.stream(deliveryRepository.findAll().spliterator(), false)
            .map(StNetexDelivery::getFilename)
            .collect(Collectors.toSet());
    }

    /**
     * Retrieve a list of fileinfo about the newest available netex files per fileset on the Ndov server.
     * 
     * @return The list.
     * @throws IOException
     */
    public List<NetexFileInfo> getNewestNetexFiles() throws IOException {
        return sources.values().stream().flatMap(source -> source.getNewestFiles().stream()).toList();
    }
    
    /**
     * Retrieve a list of fileinfo about the available netex files on the Ndov server.
     * 
     * @return The list.
     * @throws IOException
     */
    public List<NetexFileInfo> getAvailableNetexFiles() throws IOException {
        return sources.values().stream().flatMap(source -> source.getAvailableFiles().stream()).toList();
    }
    
    private void initializeFileInfo() { 
        try {
            var ftpClient = connect();
            for (var folder : getNetexNdovSourceFolders(ftpClient)) {
                var ndovSource = new NdovSource(folder);
                initializeFileSets(ftpClient, ndovSource);
                this.sources.put(ndovSource.getId(), ndovSource);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return;
    }
}
