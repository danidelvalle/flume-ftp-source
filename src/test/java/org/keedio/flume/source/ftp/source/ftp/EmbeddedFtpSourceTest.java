package org.keedio.flume.source.ftp.source.ftp;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import javax.annotation.concurrent.NotThreadSafe;

import org.apache.flume.EventDeliveryException;
import org.apache.flume.PollableSource;
import org.keedio.flume.source.ftp.source.TestFileUtils;
import org.keedio.flume.source.ftp.source.ftp.server.EmbeddedFTPServer;

import org.keedio.flume.source.ftp.source.utils.FTPSourceEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;



/**
 * Basic integration tests for the Keedios' Flume FTP Source,
 *
 */
@NotThreadSafe
public class EmbeddedFtpSourceTest extends AbstractFtpSourceTest {

    private static Logger logger = LoggerFactory.getLogger(EmbeddedFtpSourceTest.class);

    static {
        logger.info("homeDir: " + EmbeddedFTPServer.homeDirectory.toFile().getAbsolutePath());
    }
    
    @Test
    public void testIgnoreSuffixFile() {
    	String filename1 = EmbeddedFTPServer.homeDirectory + "/test_a.txt";
    	Path file1 = Paths.get(filename1);
    	String filename2 = EmbeddedFTPServer.homeDirectory 
     			+ "/test_b.txt"
     			+ ftpSource.getKeedioSource().getRenamedSuffix();
        Path file2 = Paths.get(filename2);
        
        try {
        	// Create a file using the renamed suffix
        	
        	PrintWriter writer = new PrintWriter(filename1,"UTF-8");
            writer.println("The first line");
            writer.println("The second line");
            writer.close();
            
            // Create a file using the renamed suffix
        	writer = new PrintWriter(filename2, "UTF-8");
            writer.println("The first line");
            writer.println("The second line");
            writer.println("The third line");
            writer.close();
        	
            PollableSource.Status proc = ftpSource.process();
            Assert.assertEquals(PollableSource.Status.READY, proc);
            Assert.assertEquals(ftpSourceCounter.getFilesCount(), 2);
            Assert.assertEquals(ftpSourceCounter.getFilesProcCount(), 2);
            Assert.assertEquals(ftpSourceCounter.getFilesProcCountError(), 0);
        } catch (EventDeliveryException e) {
            Assert.fail();
        } catch (FileNotFoundException e) {
        	Assert.fail();
		} catch (UnsupportedEncodingException e) {
			Assert.fail();
		}finally {
            cleanup( file1 );
            cleanup( file2 );
        }
    }

    @Test(dependsOnMethods = "testIgnoreSuffixFile")
    public void testProcessNoFile() {
        try {
            PollableSource.Status proc = ftpSource.process();
            Assert.assertEquals(PollableSource.Status.READY, proc);
            Assert.assertEquals(ftpSourceCounter.getFilesCount(), 0);
            Assert.assertEquals(ftpSourceCounter.getFilesProcCount(), 0);
            Assert.assertEquals(ftpSourceCounter.getFilesProcCountError(), 0);
        } catch (EventDeliveryException e) {
            Assert.fail();
        }
    }

    @Test(dependsOnMethods = "testProcessNoFile")
    public void testProcessNewFile() {
        Path tmpFile = null;
        try {
            tmpFile = TestFileUtils.createTmpFile(EmbeddedFTPServer.homeDirectory);
            PollableSource.Status proc = ftpSource.process();
            Assert.assertEquals(PollableSource.Status.READY, proc);
            Assert.assertEquals(ftpSourceCounter.getFilesCount(), 1);
            Assert.assertEquals(ftpSourceCounter.getFilesProcCount(), 1);
            Assert.assertEquals(ftpSourceCounter.getFilesProcCountError(), 0);
        } catch (IOException|EventDeliveryException e) {
            Assert.fail();
        } finally {
            cleanup( tmpFile );
        }
    }

    @Test(dependsOnMethods = "testProcessNewFile")
    public void testProcessNewFileInNewFolder() {
        Path tmpDir = null;
        Path tmpFile = null;
        try {
            tmpDir = TestFileUtils.createTmpDir(EmbeddedFTPServer.homeDirectory);
            tmpFile = TestFileUtils.createTmpFile(tmpDir);

            PollableSource.Status proc = ftpSource.process();
            Assert.assertEquals(PollableSource.Status.READY, proc);
            Assert.assertEquals(ftpSourceCounter.getFilesCount(), 1);
            Assert.assertEquals(ftpSourceCounter.getFilesProcCount(), 1);
            Assert.assertEquals(ftpSourceCounter.getFilesProcCountError(), 0);
        } catch (IOException|EventDeliveryException e) {
            Assert.fail();
        } finally {
            cleanup(Arrays.asList(tmpFile, tmpDir));
        }

    }

    /**
     * Creates a new empty file in the ftp root,
     * creates a new directory in the ftp root and an empty file inside of it.
     */
    @Test(dependsOnMethods = "testProcessNewFileInNewFolder")
    public void testProcessMultipleFiles0() {
        Path tmpDir = null;
        Path tmpFile0 = null;
        Path tmpFile1 = null;
        try {
            tmpDir = TestFileUtils.createTmpDir(EmbeddedFTPServer.homeDirectory);
            tmpFile0 = TestFileUtils.createTmpFile(EmbeddedFTPServer.homeDirectory);
            tmpFile1 = TestFileUtils.createTmpFile(tmpDir);

            PollableSource.Status proc = ftpSource.process();
            Assert.assertEquals(PollableSource.Status.READY, proc);
            Assert.assertEquals(ftpSourceCounter.getFilesCount(), 2);
            Assert.assertEquals(ftpSourceCounter.getFilesProcCount(), 2);
            Assert.assertEquals(ftpSourceCounter.getFilesProcCountError(), 0);
        } catch (IOException|EventDeliveryException e) {
            Assert.fail();
        } finally {
            cleanup(Arrays.asList(tmpFile0, tmpFile1, tmpDir));
        }
    }

    /**
     * Tries to access a file without permissions
     */
    @Test(dependsOnMethods = "testProcessMultipleFiles0")
    public void testProcessNoPermission() {
    	// Only if the OS is POSIX compliant
    	if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
    		return;
    	}
    		
        Path tmpFile0 = null;
        
        try {
            tmpFile0 = TestFileUtils.createTmpFile(EmbeddedFTPServer.homeDirectory);
            Files.setPosixFilePermissions(tmpFile0,
                    new HashSet<PosixFilePermission>(Arrays.asList(PosixFilePermission.OWNER_WRITE)));

            PollableSource.Status proc = ftpSource.process();
            Assert.assertEquals(PollableSource.Status.READY, proc);
            Assert.assertEquals(ftpSourceCounter.getFilesCount(), 1);
            Assert.assertEquals(ftpSourceCounter.getFilesProcCount(), 0);
            Assert.assertEquals(ftpSourceCounter.getFilesProcCountError(), 1);
        	
        } catch (IOException|EventDeliveryException e) {
            Assert.fail();
        } finally {
            cleanup(tmpFile0);
        }

    }

    
    @Test(dependsOnMethods = "testProcessNoPermission")
    public void testProcessNotEmptyFile() {
        Path tmpFile0 = null;
        try {
            tmpFile0 = TestFileUtils.createTmpFile(EmbeddedFTPServer.homeDirectory);
            TestFileUtils.appendASCIIGarbageToFile(tmpFile0);
            PollableSource.Status proc = ftpSource.process();
            Assert.assertEquals(PollableSource.Status.READY, proc);
            Assert.assertEquals(ftpSourceCounter.getFilesCount(), 1);
            Assert.assertEquals(ftpSourceCounter.getFilesProcCount(), 1);
            Assert.assertEquals(ftpSourceCounter.getFilesProcCountError(), 0);

           // Map<String, Long> map = ftpSource.loadMap(this.getAbsoutePath);
            Map<String, Long> map = ftpSource.getKeedioSource().loadMap(this.getAbsoutePath);
            String filename = "//"+tmpFile0.toFile().getName();

            Assert.assertEquals( Long.valueOf(map.get(filename)), Long.valueOf(82L * 100L));
        } catch (IOException|ClassNotFoundException|EventDeliveryException e) {
            logger.error("",e);
            Assert.fail();
            
        } finally {
            cleanup(tmpFile0);
        }
    }

    @Test(dependsOnMethods = "testProcessNotEmptyFile")
    public void testProcessModifiedFile() {
        Path tmpFile0 = null;
        try {
            tmpFile0 = TestFileUtils.createTmpFile(EmbeddedFTPServer.homeDirectory);
            TestFileUtils.appendASCIIGarbageToFile(tmpFile0);
            PollableSource.Status proc0 = ftpSource.process();
            Assert.assertEquals(PollableSource.Status.READY, proc0);
            Assert.assertEquals(ftpSourceCounter.getFilesCount(), 1);
            Assert.assertEquals(ftpSourceCounter.getFilesProcCount(), 1);
            Assert.assertEquals(ftpSourceCounter.getFilesProcCountError(), 0);

            TestFileUtils.appendASCIIGarbageToFile(tmpFile0, 1000, 100);

            PollableSource.Status proc1 = ftpSource.process();
            Assert.assertEquals(PollableSource.Status.READY, proc1);
            Assert.assertEquals(ftpSourceCounter.getFilesCount(), 1);
            Assert.assertEquals(ftpSourceCounter.getFilesProcCount(), 1);
            Assert.assertEquals(ftpSourceCounter.getFilesProcCountError(), 0);

            Map<String, Long> map = ftpSource.getKeedioSource().loadMap(this.getAbsoutePath);

            String filename = "//"+tmpFile0.toFile().getName();

            Assert.assertEquals(Long.valueOf(map.get(filename)), Long.valueOf(82L * 100L + 1000L * 102L));

        } catch (IOException|EventDeliveryException|ClassNotFoundException e) {
            Assert.fail();
        } finally {
            cleanup(tmpFile0);
        }
    }

    /**
     * Creates N temporary non-empty files in the
     * FTP root dir and process it using the FTP source.
     */
    @Test(dependsOnMethods = "testProcessModifiedFile")
    public void testProcessMultipleFiles1() {
        int totFiles = 100;
        List<Path> files = new LinkedList<>();

        try {
            for (int i = 1; i <= totFiles; i++) {
                Path tmpFile0 = TestFileUtils.createTmpFile(EmbeddedFTPServer.homeDirectory);
                TestFileUtils.appendASCIIGarbageToFile(tmpFile0);

              	// Only if the OS is POSIX compliant
            	if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix") && (i == 8) ) {	
                    Files.setPosixFilePermissions(tmpFile0,
                            new HashSet<>(Arrays.asList(PosixFilePermission.OWNER_WRITE)));
            	}

                files.add(tmpFile0);
            }

            PollableSource.Status proc0 = ftpSource.process();
            Assert.assertEquals(PollableSource.Status.READY, proc0);
            Assert.assertEquals(ftpSourceCounter.getFilesCount(), totFiles);
            if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            	Assert.assertEquals(ftpSourceCounter.getFilesProcCount(), totFiles - 1);
            	Assert.assertEquals(ftpSourceCounter.getFilesProcCountError(), 1);
            }
            else {
            	Assert.assertEquals(ftpSourceCounter.getFilesProcCount(), totFiles);
            	Assert.assertEquals(ftpSourceCounter.getFilesProcCountError(), 0);
            }
            
        } catch (IOException|EventDeliveryException e) {
            Assert.fail();
        } finally {
            cleanup(files);
        }
    }
    
   @Test(dependsOnMethods = "testProcessMultipleFiles1")
    public void testFtpFailure() throws IOException {
        class MyEventListener extends FTPSourceEventListener {
            @Override
            public void fileStreamRetrieved()  {
                logger.info("Stopping server");
                EmbeddedFTPServer.ftpServer.stop();

                while (!EmbeddedFTPServer.ftpServer.isStopped()){
                    try {
                        Thread.currentThread().wait(100);
                    } catch (InterruptedException e) {
                        logger.error("",e);
                    }
                }
            }
        }
        //ftpSource.getKeedioSource().getClientSource().setFileType(FTP.BINARY_FILE_TYPE);
        ftpSource.setListener(new MyEventListener());

        String[] directories = EmbeddedFTPServer.homeDirectory.toFile().list();

        logger.info("Found files: ");

        for (String directory : directories) {
            logger.info(directory);
        }

        Path tmpFile0 = null;
        try {
            tmpFile0 = TestFileUtils.createTmpFile(EmbeddedFTPServer.homeDirectory);
            TestFileUtils.appendASCIIGarbageToFile(tmpFile0, 100000, 1000);
          // TestFileUtils.appendASCIIGarbageToFile(tmpFile0, 100, 10);
            Assert.assertEquals(ftpSourceCounter.getFilesCount(), 0);

            PollableSource.Status proc0 = ftpSource.process();
            Assert.assertEquals(PollableSource.Status.READY, proc0);
            Assert.assertEquals(ftpSourceCounter.getFilesCount(), 1);
//            Assert.assertEquals(ftpSourceCounter.getFilesProcCount(), 0);
//            Assert.assertEquals(ftpSourceCounter.getFilesProcCountError(), 1);
        } catch (IOException|EventDeliveryException e) {
            Assert.fail();
        } finally {
            cleanup(tmpFile0);
        }
    }
}
