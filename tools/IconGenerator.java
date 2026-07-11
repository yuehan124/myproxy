import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class IconGenerator {

    private static final int[] SIZES = {16, 32, 48, 256};
    private static final Color ICON_COLOR = new Color(220, 53, 69);
    private static final Path OUTPUT_DIRECTORY = Path.of("src", "main", "resources", "icons");

    private IconGenerator() {
    }

    public static void main(String[] args) throws IOException {
        Files.createDirectories(OUTPUT_DIRECTORY);

        byte[][] pngImages = new byte[SIZES.length][];
        for (int i = 0; i < SIZES.length; i++) {
            BufferedImage image = createIcon(SIZES[i]);
            Path pngPath = OUTPUT_DIRECTORY.resolve("myproxy-" + SIZES[i] + ".png");
            ImageIO.write(image, "png", pngPath.toFile());

            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                ImageIO.write(image, "png", output);
                pngImages[i] = output.toByteArray();
            }
        }

        writeIco(OUTPUT_DIRECTORY.resolve("MyProxy.ico"), pngImages);
        System.out.println("Generated MyProxy icons in " + OUTPUT_DIRECTORY.toAbsolutePath());
    }

    private static BufferedImage createIcon(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(ICON_COLOR);
            int stroke = Math.max(2, size / 8);
            int margin = size / 8;
            graphics.setStroke(new BasicStroke(stroke));
            graphics.drawOval(margin, margin, size - 2 * margin - 1, size - 2 * margin - 1);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static void writeIco(Path path, byte[][] images) throws IOException {
        try (DataOutputStream output = new DataOutputStream(Files.newOutputStream(path))) {
            writeShort(output, 0);
            writeShort(output, 1);
            writeShort(output, images.length);

            int offset = 6 + 16 * images.length;
            for (int i = 0; i < images.length; i++) {
                output.writeByte(SIZES[i] == 256 ? 0 : SIZES[i]);
                output.writeByte(SIZES[i] == 256 ? 0 : SIZES[i]);
                output.writeByte(0);
                output.writeByte(0);
                writeShort(output, 1);
                writeShort(output, 32);
                writeInt(output, images[i].length);
                writeInt(output, offset);
                offset += images[i].length;
            }
            for (byte[] image : images) {
                output.write(image);
            }
        }
    }

    private static void writeShort(DataOutputStream output, int value) throws IOException {
        output.writeByte(value & 0xff);
        output.writeByte((value >>> 8) & 0xff);
    }

    private static void writeInt(DataOutputStream output, int value) throws IOException {
        output.writeByte(value & 0xff);
        output.writeByte((value >>> 8) & 0xff);
        output.writeByte((value >>> 16) & 0xff);
        output.writeByte((value >>> 24) & 0xff);
    }
}
