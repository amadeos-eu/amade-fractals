package org.datazuul.amadeos.fractals.repository;

import org.datazuul.amadeos.fractals.domain.ImageFormat;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;

/**
 * Writes files so that a failure never leaves half a file behind: into a temporary file next to the
 * target, then moved over it.
 */
public class ImageFiles {
    public static final float JPEG_QUALITY = 0.92f;

    /** Writes the stream's content atomically. */
    @FunctionalInterface
    public interface Content {
        void writeTo(OutputStream out) throws IOException;
    }

    public void write(BufferedImage image, Path path, ImageFormat format) throws IOException {
        writeAtomically(path, out -> encode(image, format, out));
    }

    public void writeAtomically(Path path, Content content) throws IOException {
        Path target = path.toAbsolutePath();
        Path directory = target.getParent();
        Files.createDirectories(directory);
        Path temp = Files.createTempFile(directory, "." + target.getFileName(), ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(temp)) {
                content.writeTo(out);
            }
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static void encode(BufferedImage image, ImageFormat format, OutputStream out) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format.writerName());
        if (!writers.hasNext()) throw new IOException("No writer for " + format);
        ImageWriter writer = writers.next();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(stream);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (format == ImageFormat.JPEG) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(JPEG_QUALITY);
            }
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }
}
