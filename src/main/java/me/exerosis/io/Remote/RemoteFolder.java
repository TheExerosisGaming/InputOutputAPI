package me.exerosis.io.Remote;

import me.exerosis.io.util.ArchiveUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class RemoteFolder {
    private final File _file;
    private final URL _url;

    public RemoteFolder(File file, String url) throws MalformedURLException {
        this(file, new URL(url));
    }

    public RemoteFolder(File file, URL url) {
        _file = file;
        _url = url;
        if (file.mkdirs())
            System.err.println("Failed to find that folder, created directory '" + file.getPath() + "'!");
    }

    public void sync() {
        if (RemoteUtils.isUpdated(_file, _url) != 1)
            return;
        try {
            FileUtils.cleanDirectory(_file);
        } catch (IOException e) {
            System.err.println("Failed to clean directory before download!");
        }
        ArchiveUtils.downloadFileInto(_url.toString(), _file);
        if (!_file.setLastModified(System.currentTimeMillis()))
            System.err.println("Failed to set the last modified date!");
    }

    public File getFile() {
        return _file;
    }

    public URL getUrl() {
        return _url;
    }
}