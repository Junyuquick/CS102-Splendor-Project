package ui.swing;

import config.Config;
import model.DevelopmentCard;
import model.GemColor;
import model.NobleTile;

import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Loads and scales artwork used by the Swing interface.
 *
 * The loader searches configured asset directories first and then falls back to a few
 * project-local media locations.
 */
final class SwingAssetLoader {
    private final Config config;

    /**
     * Creates an asset loader that reads locations from the supplied configuration.
     *
     * @param config application configuration
     */
    SwingAssetLoader(Config config) {
        this.config = config;
    }

    /**
     * Loads the icon used for a token button.
     *
     * @param color gem color to load
     * @return token icon, or null if none could be found
     */
    ImageIcon loadTokenIcon(GemColor color) {
        String filename = tokenFilename(color);

        Path[] candidates = new Path[]{
                config.getTokenImageDir().resolve(filename),
                Path.of("assets", "tokens", filename),
                Path.of("resources", "tokens", filename),
                Path.of("src", "assets", "tokens", filename),
                Path.of("media", "tokens", filename)
        };

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return loadCircularIcon(candidate, SwingUiTheme.TOKEN_ICON_SIZE);
            }
        }

        try (var stream = Files.list(Path.of("media", "tokens"))) {
            List<Path> pngs = stream
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".png"))
                    .sorted()
                    .toList();

            if (!pngs.isEmpty()) {
                int idx = fallbackTokenIndex(color);
                if (idx >= 0 && idx < pngs.size()) {
                    return loadCircularIcon(pngs.get(idx), SwingUiTheme.TOKEN_ICON_SIZE);
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private String tokenFilename(GemColor color) {
        if (color == GemColor.WHITE) {
            return "white.png";
        }
        if (color == GemColor.BLUE) {
            return "blue.png";
        }
        if (color == GemColor.GREEN) {
            return "green.png";
        }
        if (color == GemColor.RED) {
            return "red.png";
        }
        if (color == GemColor.BLACK) {
            return "black.png";
        }
        return "gold.png";
    }

    private int fallbackTokenIndex(GemColor color) {
        if (color == GemColor.GREEN) {
            return 0;
        }
        if (color == GemColor.BLACK) {
            return 1;
        }
        if (color == GemColor.WHITE) {
            return 2;
        }
        if (color == GemColor.RED) {
            return 3;
        }
        if (color == GemColor.BLUE) {
            return 4;
        }
        return 5;
    }

    /**
     * Loads the artwork for a development card.
     *
     * @param card card whose artwork should be loaded
     * @return card icon, or null if no image is available
     */
    ImageIcon loadCardIcon(DevelopmentCard card) {
        String filename = "card_" + card.getId() + ".png";
        Path[] candidates = new Path[]{
                config.getCardImageDir().resolve("devLevel" + card.getLevel()).resolve(filename),
                config.getCardImageDir().resolve(filename),
                Path.of("media", "devLevel" + card.getLevel(), filename)
        };
        for (Path path : candidates) {
            if (Files.exists(path)) {
                return loadScaledIcon(path, SwingUiTheme.DEV_CARD_ICON_WIDTH, SwingUiTheme.DEV_CARD_ICON_HEIGHT);
            }
        }
        return null;
    }

    /**
     * Loads the artwork for a noble tile.
     *
     * @param noble noble whose artwork should be loaded
     * @return noble icon, or null if no image is available
     */
    ImageIcon loadNobleIcon(NobleTile noble) {
        String filename = "card_" + noble.getId() + ".png";
        Path[] candidates = new Path[]{
                config.getNobleImageDir().resolve(filename),
                Path.of("media", "nobles", filename)
        };
        for (Path path : candidates) {
            if (Files.exists(path)) {
                return loadScaledIcon(path, SwingUiTheme.NOBLE_ICON_SIZE, SwingUiTheme.NOBLE_ICON_SIZE);
            }
        }
        return null;
    }

    /**
     * Loads an image and scales it to fit within the requested bounds while preserving aspect ratio.
     *
     * @param imagePath source image path
     * @param targetW target width in pixels
     * @param targetH target height in pixels
     * @return scaled icon centered in the target canvas
     */
    // Partially generated by ChatGPT-5.3.
    private ImageIcon loadScaledIcon(Path imagePath, int targetW, int targetH) {
        ImageIcon icon = new ImageIcon(imagePath.toString()); // Swing icon wrapper that can load from file path.
        Image src = icon.getImage(); // Underlying AWT image data for manual scaling.
        int imgW = Math.max(1, src.getWidth(null));
        int imgH = Math.max(1, src.getHeight(null));
        double scale = Math.min((double) targetW / imgW, (double) targetH / imgH);
        int newW = (int) (imgW * scale);
        int newH = (int) (imgH * scale);
        int x = (targetW - newW) / 2;
        int y = (targetH - newH) / 2;

        BufferedImage out = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics(); // Create a drawing context for the buffered image.
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC); // Smooth scaling.
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setBackground(new Color(0, 0, 0, 0));
        g2.clearRect(0, 0, targetW, targetH); // Clear canvas using current background.
        g2.drawImage(src, x, y, newW, newH, null); // Paint scaled image onto target canvas.
        g2.dispose(); // Release Graphics2D resources.
        return new ImageIcon(out); // Wrap result as Swing-compatible icon.
    }

    /**
     * Loads an image, scales it to cover a circle, and clips the result to a round token icon.
     *
     * @param imagePath source image path
     * @param diameter target icon diameter in pixels
     * @return circular icon
     */
    // Partially generated by ChatGPT-5.3 for the structure for efficiency
    private ImageIcon loadCircularIcon(Path imagePath, int diameter) {
        ImageIcon icon = new ImageIcon(imagePath.toString()); // Load source image as Swing icon.
        Image src = icon.getImage();
        int imgW = Math.max(1, src.getWidth(null));
        int imgH = Math.max(1, src.getHeight(null));
        double scale = Math.max((double) diameter / imgW, (double) diameter / imgH);
        int newW = (int) Math.ceil(imgW * scale);
        int newH = (int) Math.ceil(imgH * scale);
        int x = (diameter - newW) / 2;
        int y = (diameter - newH) / 2;

        BufferedImage out = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics(); // Drawing context for circular token output.
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC); // Smooth scaling.
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setClip(new Ellipse2D.Double(0, 0, diameter, diameter)); // Clip drawing to a circular mask.
        g2.drawImage(src, x, y, newW, newH, null);
        g2.dispose();
        return new ImageIcon(out); // Convert final raster into a Swing icon.
    }
}
