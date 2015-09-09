package me.exerosis.io.util;

import org.apache.commons.io.IOUtils;

import java.io.*;

public class StreamUtil {

    private StreamUtil() {
    }

    public static void write(CharSequence path, InputStream stream, boolean close) {
        write(new File(path.toString()), stream, close);
    }

    public static void write(File file, InputStream stream, boolean close) {
        file.getParentFile().mkdirs();
        if (file.exists())
            file.delete();

        OutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            fileOutputStream = new BufferedOutputStream(fileOutputStream);
            IOUtils.copy(stream, fileOutputStream);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            StreamUtil.closeQuietly(fileOutputStream);
            if (close)
                StreamUtil.closeQuietly(stream);
        }
    }

    public static void writeBuffer(File file, InputStream inputStream, boolean close) {
        OutputStream outputStream = null;
        try {
            inputStream = new BufferedInputStream(inputStream);

            outputStream = new FileOutputStream(file);
            outputStream = new BufferedOutputStream(outputStream);

            IOUtils.copy(inputStream, outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            StreamUtil.closeQuietly(outputStream);
            if (close)
                StreamUtil.closeQuietly(inputStream);
        }
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null)
            try {
                if (closeable.getClass().isAssignableFrom(OutputStream.class))
                    ((OutputStream) closeable).flush();
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }
}