package me.exerosis.io;

import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.Validate;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

public class DataBaseUtils {
    private static final String CREATION_TIME = "basic:creationTime";

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public static boolean createFile(File output, URL downloadURL, File dataBaseSaveFile) {
        Validate.notNull(output, "Output file can not be null!");
        if (output.exists()) {
            return true;
        }

        try {
            return (downloadAndUnzip(downloadURL, output) || copyMostUpToDateVersion(dataBaseSaveFile, output) || copyResource(output.getName(), output));
        } catch (FileExistsException e) {
            e.printStackTrace();
        }
        return false;

    }

    public static boolean copyMostUpToDateVersion(File dataBaseSaveFile, File output) throws FileExistsException {
        if (dataBaseSaveFile == null)
            return false;
        if (output.exists())
            throw new FileExistsException(output);
        File mostUpToDateVersion = getMostUpToDateVersion(dataBaseSaveFile, output.getName());
        if (mostUpToDateVersion == null)
            return false;

        try {
            com.google.common.io.Files.copy(mostUpToDateVersion, output);
            setDateCreated(output, getDateCreated(mostUpToDateVersion));
            System.out.println("Copyied file " + mostUpToDateVersion.getName() + " to " + output.getName());
            return true;
        } catch (IOException e) {
            System.err.println("Failed to copy file " + mostUpToDateVersion.getName() + " to " + output.getName());
            e.printStackTrace();
            if (output.delete())
                System.out.println("Deleted " + output.getName());
        }
        return false;
    }

    public static File getMostUpToDateVersion(File dataBaseSaveFile, String name) {
        if (dataBaseSaveFile == null)
            return null;
        File mostUpToDateVersion = null;
        Date mostUpToDateTimeStamp = null;
        for (File file : dataBaseSaveFile.listFiles()) {
            String fileName = file.getName();
            Date fileDate = getDateCreated(file);

            if (fileDate != null && fileName.contains(removeAllExtensions(name)) && fileName.endsWith(getExtension(name))
                    && (mostUpToDateTimeStamp == null || fileDate.after(mostUpToDateTimeStamp))) {
                mostUpToDateVersion = file;
                mostUpToDateTimeStamp = fileDate;
            }
        }
        return mostUpToDateVersion;
    }

    public static boolean saveToDataBaseSaveFile(File dataBaseSaveFile, File file) {
        if (dataBaseSaveFile == null)
            return false;
        File moveTo = exist(new File(dataBaseSaveFile, timeStamp(file)));
        file.renameTo(moveTo);
        return true;
    }

    public static boolean download(URL downloadURL, File outputFile) throws FileExistsException {
        if (downloadURL == null)
            return false;
        if (outputFile.exists())
            throw new FileExistsException(outputFile);
        try {
            System.out.println("Querying server...");

            ReadableByteChannel rbc = Channels.newChannel(downloadURL.openStream());
            FileOutputStream fos = new FileOutputStream(outputFile);
            System.out.println("Starting download...");
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

            System.out.println("Download complete!");

            fos.flush();
            fos.close();

            Date date = lastModified(downloadURL);
            if (date == null)
                date = new Date();
            setDateCreated(outputFile, date);
            return true;
        } catch (UnknownHostException e) {
            System.err.println("Unknown Host: " + e.getMessage());
        } catch (FileNotFoundException e) {
            System.err.println("File not found at: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean downloadAndUnzip(URL downloadURL, File output) throws FileExistsException {
        if (downloadURL == null)
            return false;
        if (output.exists())
            throw new FileExistsException(output);

        File downloadFile = exist(new File(downloadURL.toString().substring(downloadURL.toString().lastIndexOf("/") + 1)));

        if (download(downloadURL, downloadFile)) {
            if (unarchive(downloadFile, output) || downloadFile.renameTo(output)) {
                setDateCreated(output, getDateCreated(downloadFile));
                downloadFile.delete();
                return true;
            }

            System.err.println("A Error has occurred!");

            if (downloadFile.delete())
                System.out.println("Deleted " + downloadFile.getName());

            if (output.delete())
                System.out.println("Deleted " + output.getName());
        }
        return false;
    }

    public static boolean unarchive(File archivedFile, File outputFile) {
        return unarchiveGZ(archivedFile, outputFile) || unarchiveZip(archivedFile, outputFile);
    }

    public static boolean unarchiveGZ(File gzFile, File outputFile) {
        if (gzFile.getName().endsWith(".gz")) {
            try {
                return unarchiveGZ(new FileInputStream(gzFile), new FileOutputStream(outputFile));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                System.err.println("A Error has occurred while creating gz input/output streams!");
            }
        }
        return false;
    }

    public static boolean unarchiveGZ(FileInputStream inputStream, FileOutputStream outputStream) {
        try {
            System.out.println("Unzipping...");
            GZIPInputStream gzis = new GZIPInputStream(inputStream);
            FileOutputStream out = outputStream;
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzis.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            gzis.close();
            out.flush();
            out.close();
            System.out.println("Unziped!");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("A Error has occurred");
        }
        return false;
    }

    public static boolean unarchiveZip(File zipFile, File outputFile) {
        if (zipFile.getName().endsWith(".zip")) {
            try {
                return unarchiveGZ(new FileInputStream(zipFile), new FileOutputStream(outputFile));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                System.err.println("A Error has occurred while creating zip input/output streams!");
            }
        }
        return false;
    }

    public static boolean unarchiveZip(FileInputStream inputStream, FileOutputStream outputStream) {
        try {
            System.out.println("Unzipping...");

            ZipInputStream zis = new ZipInputStream(inputStream);
            FileOutputStream out = outputStream;

            byte[] buffer = new byte[1024];

            int len;
            while ((len = zis.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }

            zis.close();
            out.flush();
            out.close();

            System.out.println("Unziped!");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("A Error has occurred!");
        }
        return false;
    }

    public static boolean hasResource(String resourceName) {
        URL u = DataBaseUtils.class.getClassLoader().getResource("resourceName");
        return (u != null);
    }

    public static boolean copyResource(String resourceName, File output) throws FileExistsException {
        if (output.exists())
            throw new FileExistsException(output);
        try {
            URL inputUrl = DataBaseUtils.class.getClassLoader().getResource(resourceName);

            FileUtils.copyURLToFile(inputUrl, output);

            setDateCreated(output, new Date(0));
            System.out.println("Copyied Resource " + resourceName + " to " + output.getName());
            return true;

        } catch (FileNotFoundException e) {
            // e.printStackTrace();
        } catch (IOException e) {
            // e.printStackTrace();
        }
        return false;
    }

    /**
     * Gets the name of the data-base file with the timestamp.
     *
     * @param date
     * @return the name of the data-base file with the timestamp.
     */
    private static String timeStamp(File file) {

        StringBuilder sb = new StringBuilder(file.getName());

        sb.insert(sb.indexOf("."), dateFormat.format(getDateCreated(file)));

        return sb.toString();
    }

    public static File exist(File file) {
        if (!file.exists())
            return file;

        String fileName = file.getName();

        String newName = file.getName();

        int add = 0;

        while (new File(file.getParent(), newName).exists()) {
            add++;
            newName = removeAllExtensions(fileName) + "(" + add + ")" + getAllExtension(fileName);
        }

        return new File(file.getParent(), newName);
    }

    public static void main(String[] args) throws IOException {
        // "C:/Users/Exerosis/Documents/Jars/AaronsChatBot.jar"
        File file = new File("C:/Users/Exerosis/Desktop/GeoLiteCity.dat");

        URL url = new URL("http://geolite.maxmind.com/download/geoip/database/GeoLiteCity.dat.gz");

        // setDateCreated(file, new Date(0));

        // System.out.println(checkUpdate(file, url));

        // System.out.println(createFile(file, url, null));

        System.out.println(update(file, url, null));

    }

    public static String getAllExtension(String fileName) {
        return fileName.substring(fileName.indexOf('.'));
    }

    public static String getExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf("."));
    }

    public static String removeAllExtensions(String fileName) {
        return fileName.substring(0, fileName.indexOf("."));
    }

    public static String removeExtension(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf("."));
    }

    /**
     * Checks to see if there is a file at that url.
     *
     * @param url
     * @return true if it finds a file at the URL, false otherwise
     */
    public static boolean fileExists(URL url) {
        try {
            HttpURLConnection.setFollowRedirects(false);
            // note : you may also need
            // HttpURLConnection.setInstanceFollowRedirects(false)
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("HEAD");

            if (con.getResponseCode() != HttpURLConnection.HTTP_OK)
                throw new IOException();
            return true;
        } catch (UnknownHostException e) {
            System.err.println("Unable to connect to " + url);
            System.err.println("(This may be because you are not connected to the internet)");
            return false;
        } catch (IOException e) {
            System.err.println("No file found at  " + url);
            return false;
        }
    }

    /**
     * Gets last time the file was modified at the specified url.
     *
     * @param url
     * @return The date the file at the specified url was last modified, null
     * otherwise.
     */
    public static Date lastModified(URL url) {

        try {
            HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
            long date = httpCon.getLastModified();
            if (date != 0)
                return new Date(date);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Date getDateCreated(File file) {
        Path path = Paths.get(file.getPath());
        long date;
        try {
            date = ((FileTime) Files.getAttribute(path, CREATION_TIME)).toMillis();
        } catch (IOException e) {
            date = Long.MAX_VALUE;
        }
        return new Date(Math.min(date, file.lastModified()));
    }

    public static void setDateCreated(File file, Date date) {
        Path path = Paths.get(file.getPath());
        try {
            Files.setAttribute(path, CREATION_TIME, FileTime.fromMillis(date.getTime()), LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            file.setLastModified(date.getTime());
        }
    }

    /**
     * Checks to see if there is a newer file at the url then the file.
     *
     * @param file
     * @param url
     * @return the value greater than 0 if a update is found; the value 0 if
     * no update is found; and a value less than 0 if it's unknown.
     */
    public static int checkUpdate(File file, URL url) {
        Date serverDate = lastModified(url);
        if (serverDate == null)
            return -1;
        if (!file.exists())
            return 1;
        Date myDate = getDateCreated(file);
        if (serverDate.after(myDate))
            return 1;
        return 0;
    }

    public static boolean update(File output, URL downloadURL, File dataBaseSaveFile) {

        if (checkUpdate(output, downloadURL) == 0)
            return false;

        return forceUpdate(output, downloadURL, dataBaseSaveFile);
    }

    public static boolean forceUpdate(File output, URL downloadURL, File dataBaseSaveFile) {

        if (!saveToDataBaseSaveFile(dataBaseSaveFile, output))
            output.delete();
        return createFile(output, downloadURL, dataBaseSaveFile);
    }

}