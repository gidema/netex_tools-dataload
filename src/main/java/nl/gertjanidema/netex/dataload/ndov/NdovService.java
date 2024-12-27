package nl.gertjanidema.netex.dataload.ndov;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;
import org.springframework.beans.factory.annotation.Value;

import nl.gertjanidema.netex.dataload.dto.NetexFileInfo;

public class NdovService {
    
    private static Pattern fileNamePattern = Pattern.compile("(NeTEx_)?(.+?)_(20\\d{6})(.+)");

//    private static Logger LOG = LoggerFactory.getLogger(NdovService.class);

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

    private Path netexTempPath;

    private Path chbTempPath;

    public Path getNetexTempPath() {
        if (netexTempPath == null) {
            this.netexTempPath = tempPath.resolve("netex");
        }
        return netexTempPath;
    }

    public Path getChbTempPath() {
        if (chbTempPath == null) {
            this.chbTempPath = tempPath.resolve("chb");
        }
        return chbTempPath;
    }
    
   /**
     * Initialize the NeTeX context. Create temporary folders if necessary and clear
     * any old temporary files.
     */
    public void initializeNetex() {
        var folder = getNetexTempPath();
        if (!folder.toFile().exists()) {
            folder.toFile().mkdir();
        }
        else {
            for (File file : folder.toFile().listFiles()) {
                if (file.isFile()) file.delete();
            }
        }
    }
    
    /**
     * Initialize the CHB context. Create temporary folders if necessary and clear
     * any old temporary files.
     */
    public void initializeChb() {
        var folder = getChbTempPath();
        if (!folder.toFile().exists()) {
            folder.toFile().mkdir();
        }
        else {
            folder.forEach(p -> {
                var file = p.toFile();
                if (file.isFile()) file.delete();
            });
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

    private static List<String> getNetexAgencyFolders(FTPClient ftpClient) {
        List<String> agencyFolders;  
        try {
            ftpClient.changeWorkingDirectory("netex");
            ftpClient.enterLocalPassiveMode();
            FTPFile[] folders = ftpClient.listDirectories();
            agencyFolders = new ArrayList<>(folders.length);
            for (FTPFile folder : folders) {
                agencyFolders.add(folder.getName());
            }
        } catch (@SuppressWarnings("unused") IOException e) {
            return Collections.emptyList();
        }
        return agencyFolders;
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
    
    private static List<NetexFileInfo> getFileInfo(FTPClient ftpClient, String folder) throws IOException {
        String agencyId = folder.toUpperCase();
        FTPFile[] ftpFiles = ftpClient.listFiles(folder);
        var fileInfoList = new ArrayList<NetexFileInfo>(ftpFiles.length);
        for (FTPFile ftpFile : ftpFiles) {
            var fileInfo = new NetexFileInfo();
            fileInfo.setAgencyId(agencyId);
            fileInfo.setFileName(ftpFile.getName());
            fileInfo.setLastModified(ftpFile.getTimestamp());
            fileInfo.setSize(ftpFile.getSize());
            Matcher m = fileNamePattern.matcher(ftpFile.getName());
            if (m.matches()) {
                var result = m.toMatchResult();
                fileInfo.setFileGroup(result.group(2));
            }
            fileInfoList.add(fileInfo);
        }
        return fileInfoList;    
    }
    
    public void downloadNetexFiles(Collection<NetexFileInfo> netexFiles) {
        initializeNetex();
        netexFiles.forEach(fileInfo -> {
            try {
                downloadNetexFile(fileInfo);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
    }

    public void downloadNetexFile(NetexFileInfo fileInfo) throws IOException {
        File sourceFile = new File(String.format("/netex/%s/%s", fileInfo.getAgencyId().toLowerCase(), fileInfo.getFileName()));
        downloadFile(sourceFile , getNetexTempPath());
    }

    public void downloadFile(File sourceFile, Path targetPath) throws IOException {
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

    public List<NetexFileInfo> getNetexFileInfo() throws IOException {
        var ftpClient = connect();
        var fileInfoList = new LinkedList<NetexFileInfo>();
        var folderIterator = getNetexAgencyFolders(ftpClient).iterator();
        while (folderIterator.hasNext()) {
            var folder = folderIterator.next();
            var fileInfoIterator = getFileInfo(ftpClient, folder).iterator();
            while (fileInfoIterator.hasNext()) {
                fileInfoList.add(fileInfoIterator.next());
            }
        }
        return fileInfoList;
    }
}
