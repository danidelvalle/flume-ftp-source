/*
 * KEEDIO
 */
package org.keedio.flume.source.ftp.client.factory;

import org.apache.flume.Context;
import org.keedio.flume.source.ftp.client.KeedioSource;
import org.keedio.flume.source.ftp.client.sources.FTPSSource;
import org.keedio.flume.source.ftp.client.sources.FTPSource;
import org.keedio.flume.source.ftp.client.sources.SFTPSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multiset.Entry;

/**
 *
 * @author Luis Lázaro lalazaro@keedio.com Keedio
 */
public class SourceFactory {

    private KeedioSource keedioSource;
    private static final Logger LOGGER = LoggerFactory.getLogger(KeedioSource.class);
    private static final Integer DISCOVER_DELAY = 10000;
    private static final boolean FLUSHLINE_DEFAULT = true;
    private static final String FOLDER_DEFAULT = System.getProperty("java.io.tmpdir");
    private static final Integer CHUNKSIZE_DEFAULT = 1024;
    private static final String FILENAME_DEFAULT = "default_file_track_status.ser";
    private static final boolean CONTROL_DELELED_FILES_DEFAULT = true;
    private static final boolean WORKING_RESTRICTED_DEFAULT = false;

    /**
     * Create KeedioSource
     *
     * @param context
     * @return KeedioSource
     */
    public KeedioSource createKeedioSource(Context context) {
        keedioSource = null;
        initSource(context);
        return keedioSource;
    }

    /**
     * Initiate attributes of KeediosSource according to context
     *
     * @param context of the source
     * @return KeedioSource
     */
    public KeedioSource initSource(Context context) {
        switch (context.getString("client.source")) {
            case "ftp":
                keedioSource = new FTPSource();
                initCommonParam(context);
                break;
            case "sftp":
                keedioSource = new SFTPSource();
                SFTPSource sftpSource = new SFTPSource(context.getString("knownHosts"));
                keedioSource = sftpSource;
                initCommonParam(context);
                break;
            case "ftps":
                keedioSource = new FTPSSource();
                FTPSSource ftpsSource = new FTPSSource(
                        context.getBoolean("security.enabled"),
                        context.getString("security.cipher"),
                        context.getBoolean("security.certificate.enabled"),
                        context.getString("path.keystore", FOLDER_DEFAULT),
                        context.getString("store.pass")
                );
                keedioSource = ftpsSource;
                initCommonParam(context);
                break;
            default:
                LOGGER.error("Source not found in context");
        }
        return keedioSource;
    }

    /**
     * initialize common parameters for all sources.
     * @param context of source
     */
    public void initCommonParam(Context context) {
    	
    	/*for (java.util.Map.Entry<String, String> entry : context.getParameters().entrySet())
    	{
    	    System.out.println(entry.getKey() + "/" + entry.getValue());
    	}
    	*/
    	/*
    	System.out.println(context.getBoolean("working.restricted"));
    	System.out.println(context.getBoolean("working.restricted", WORKING_RESTRICTED_DEFAULT));
    	
    	System.out.println(context.getBoolean("control_deleted_files"));
    	System.out.println(context.getBoolean("control_deleted_files", CONTROL_DELELED_FILES_DEFAULT));
    	*/
    	
        keedioSource.setBufferSize(context.getInteger("buffer.size"));
        keedioSource.setServer(context.getString("name.server"));
        keedioSource.setUser(context.getString("user"));
        keedioSource.setPassword(context.getString("password"));
        keedioSource.setRunDiscoverDelay(context.getInteger("run.discover.delay", DISCOVER_DELAY));
        keedioSource.setWorkingDirectory(context.getString("working.directory"));
        keedioSource.setWorkingRestricted(context.getBoolean("working.restricted", WORKING_RESTRICTED_DEFAULT));
        keedioSource.setPort(context.getInteger("port"));
        keedioSource.setFolder(context.getString("folder", FOLDER_DEFAULT));
        keedioSource.setFileName(context.getString("file.name", FILENAME_DEFAULT));
        keedioSource.setFlushLines(context.getBoolean("flushlines", FLUSHLINE_DEFAULT));
        keedioSource.setChunkSize(context.getInteger("chunk.size", CHUNKSIZE_DEFAULT));
        keedioSource.setRenamedSuffix(context.getString("renamed.suffix"));
        keedioSource.setControlDeletedFiles(context.getBoolean("control_deleted_files", CONTROL_DELELED_FILES_DEFAULT));
    }

   
}
