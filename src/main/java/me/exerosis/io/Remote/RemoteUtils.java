package me.exerosis.io.Remote;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Date;

public class RemoteUtils {
    private static final String CREATION_TIME = "basic:creationTime";

    private RemoteUtils() {
    }

    public static int isUpdated(File file, URL url) {
        Date serverDate = getLastModified(url);
        if (serverDate == null || !file.exists())
            return -1;
        return serverDate.after(getDateCreated(file)) ? 1 : 0;
    }

    public static Date getLastModified(URL url) {
        try {
            long date = url.openConnection().getLastModified();
            if (date != 0)
                return new Date(date);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void setDateCreated(File file, Date date) {
        Path path = Paths.get(file.getPath());
        try {
            Files.setAttribute(path, CREATION_TIME, FileTime.fromMillis(date.getTime()), LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            if (!file.setLastModified(date.getTime()))
                System.out.println("Failed to update the date modified!");
        }

    }

    public static Date getDateCreated(File file) {
        return new Date(file.lastModified());
    }
}