package eu.amadeos.fractals.domain;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** File types for saving pictures. */
public enum ImageFormat {
    PNG("png", List.of("png")),
    JPEG("jpg", List.of("jpg", "jpeg"));

    private final String writerName;
    private final List<String> extensions;

    ImageFormat(String writerName, List<String> extensions) {
        this.writerName = writerName;
        this.extensions = extensions;
    }

    /** Name for {@link javax.imageio.ImageIO#write}. */
    public String writerName() {
        return writerName;
    }

    public List<String> extensions() {
        return extensions;
    }

    public String defaultExtension() {
        return extensions.get(0);
    }

    /** The type a file name asks for, if its extension is one of ours. */
    public static Optional<ImageFormat> forPath(Path path) {
        String extension = extension(path);
        for (ImageFormat format : values()) {
            if (format.extensions.contains(extension)) return Optional.of(format);
        }
        return Optional.empty();
    }

    /** The path with this type's extension instead of an image extension it may have. */
    public Path withExtension(Path path) {
        String name = path.getFileName().toString();
        if (forPath(path).isPresent()) name = name.substring(0, name.lastIndexOf('.'));
        return path.resolveSibling(name + "." + defaultExtension());
    }

    private static String extension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
