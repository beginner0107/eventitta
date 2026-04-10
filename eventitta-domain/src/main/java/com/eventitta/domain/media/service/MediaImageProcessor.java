package com.eventitta.domain.media.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.eventitta.domain.file.exception.FileStorageErrorCode;
import com.eventitta.domain.media.domain.MediaCategory;
import com.eventitta.domain.media.domain.MediaVariantType;
import com.eventitta.domain.media.policy.MediaPolicyProvider;
import com.eventitta.domain.media.policy.MediaVariantPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

@Component
@RequiredArgsConstructor
public class MediaImageProcessor {

    private static final String WEBP_FORMAT = "webp";

    private final MediaPolicyProvider mediaPolicyProvider;

    public MediaRenderedVariant render(MediaCategory category, MediaVariantType variantType, byte[] originalBytes) {
        BufferedImage source = decodeWithOrientation(originalBytes);
        BufferedImage transformed = switch (category) {
            case POST_IMAGE -> renderPostVariant(source, variantType);
            case PROFILE_IMAGE -> renderProfileVariant(source, variantType);
        };
        byte[] webpBytes = encodeWebp(transformed);
        return new MediaRenderedVariant(webpBytes, transformed.getWidth(), transformed.getHeight(), WEBP_FORMAT);
    }

    private BufferedImage renderPostVariant(BufferedImage source, MediaVariantType variantType) {
        MediaVariantPolicy variantPolicy = mediaPolicyProvider.getVariantPolicy();
        return switch (variantType) {
            case THUMB -> resizeToLongEdge(source, variantPolicy.postThumbLongEdge());
            case DETAIL -> resizeToLongEdge(source, variantPolicy.postDetailLongEdge());
            default -> throw FileStorageErrorCode.UNSUPPORTED_FILE_TYPE.defaultException();
        };
    }

    private BufferedImage renderProfileVariant(BufferedImage source, MediaVariantType variantType) {
        if (variantType != MediaVariantType.AVATAR) {
            throw FileStorageErrorCode.UNSUPPORTED_FILE_TYPE.defaultException();
        }
        return centerCropAndResize(source, mediaPolicyProvider.getVariantPolicy().profileAvatarSize());
    }

    private BufferedImage decodeWithOrientation(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw FileStorageErrorCode.UNSUPPORTED_FILE_TYPE.defaultException();
            }
            int orientation = readOrientation(bytes);
            return applyOrientation(image, orientation);
        } catch (IOException e) {
            throw FileStorageErrorCode.FILE_LOAD_FAIL.defaultException(e);
        }
    }

    private int readOrientation(byte[] bytes) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(bytes));
            ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            return directory != null ? directory.getInt(ExifIFD0Directory.TAG_ORIENTATION) : 1;
        } catch (Exception ignored) {
            return 1;
        }
    }

    private BufferedImage applyOrientation(BufferedImage image, int orientation) {
        return switch (orientation) {
            case 3 -> rotate(image, 180);
            case 6 -> rotate(image, 90);
            case 8 -> rotate(image, 270);
            default -> toRgb(image);
        };
    }

    private BufferedImage rotate(BufferedImage source, int degrees) {
        double radians = Math.toRadians(degrees);
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));
        int newWidth = (int) Math.floor(source.getWidth() * cos + source.getHeight() * sin);
        int newHeight = (int) Math.floor(source.getHeight() * cos + source.getWidth() * sin);

        BufferedImage rotated = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rotated.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.translate((newWidth - source.getWidth()) / 2.0, (newHeight - source.getHeight()) / 2.0);
        graphics.rotate(radians, source.getWidth() / 2.0, source.getHeight() / 2.0);
        graphics.drawRenderedImage(toRgb(source), null);
        graphics.dispose();
        return rotated;
    }

    private BufferedImage resizeToLongEdge(BufferedImage source, int maxLongEdge) {
        BufferedImage normalized = toRgb(source);
        int width = normalized.getWidth();
        int height = normalized.getHeight();
        int longEdge = Math.max(width, height);
        if (longEdge <= maxLongEdge) {
            return normalized;
        }

        double scale = maxLongEdge / (double) longEdge;
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));
        return resize(normalized, targetWidth, targetHeight);
    }

    private BufferedImage centerCropAndResize(BufferedImage source, int size) {
        BufferedImage normalized = toRgb(source);
        int squareSize = Math.min(normalized.getWidth(), normalized.getHeight());
        int x = (normalized.getWidth() - squareSize) / 2;
        int y = (normalized.getHeight() - squareSize) / 2;
        BufferedImage cropped = normalized.getSubimage(x, y, squareSize, squareSize);
        return resize(cropped, size, size);
    }

    private BufferedImage resize(BufferedImage source, int targetWidth, int targetHeight) {
        Image scaled = source.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        BufferedImage output = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = output.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(scaled, 0, 0, null);
        graphics.dispose();
        return output;
    }

    private BufferedImage toRgb(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_RGB) {
            return source;
        }
        BufferedImage normalized = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = normalized.createGraphics();
        graphics.setComposite(AlphaComposite.SrcOver);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, normalized.getWidth(), normalized.getHeight());
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return normalized;
    }

    private byte[] encodeWebp(BufferedImage image) {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(WEBP_FORMAT);
        if (!writers.hasNext()) {
            throw FileStorageErrorCode.FILE_SAVE_FAIL.defaultException();
        }

        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ImageOutputStream ios = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(ios);
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                String[] compressionTypes = writeParam.getCompressionTypes();
                if (compressionTypes != null && compressionTypes.length > 0) {
                    writeParam.setCompressionType(compressionTypes[0]);
                }
                writeParam.setCompressionQuality(mediaPolicyProvider.getVariantPolicy().webpQuality());
            }
            writer.write(null, new IIOImage(image, null, null), writeParam);
            writer.dispose();
            return output.toByteArray();
        } catch (IOException e) {
            writer.dispose();
            throw FileStorageErrorCode.FILE_SAVE_FAIL.defaultException(e);
        }
    }

    public record MediaRenderedVariant(byte[] content, int width, int height, String format) {
    }
}
