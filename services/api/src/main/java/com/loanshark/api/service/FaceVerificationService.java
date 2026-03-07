package com.loanshark.api.service;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

@Service
public class FaceVerificationService {

    public FaceVerificationResult compare(Path pdfPath, Path selfiePath) {
        try (PDDocument document = Loader.loadPDF(Files.readAllBytes(pdfPath))) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage idImage = renderer.renderImageWithDPI(0, 140);
            BufferedImage selfieImage = javax.imageio.ImageIO.read(selfiePath.toFile());
            if (selfieImage == null) {
                return new FaceVerificationResult(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), false);
            }

            long firstHash = averageHash(cropCenter(idImage));
            long secondHash = averageHash(cropCenter(selfieImage));
            int distance = Long.bitCount(firstHash ^ secondHash);
            double similarity = ((64.0 - distance) / 64.0) * 100.0;
            BigDecimal score = BigDecimal.valueOf(Math.max(0.0, similarity)).setScale(2, RoundingMode.HALF_UP);
            return new FaceVerificationResult(score, score.compareTo(new BigDecimal("78.00")) >= 0);
        } catch (IOException exception) {
            return new FaceVerificationResult(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), false);
        }
    }

    private BufferedImage cropCenter(BufferedImage source) {
        int size = Math.min(source.getWidth(), source.getHeight());
        int x = Math.max(0, (source.getWidth() - size) / 2);
        int y = Math.max(0, (source.getHeight() - size) / 2);
        BufferedImage cropped = source.getSubimage(x, y, size, size);
        Image scaled = cropped.getScaledInstance(32, 32, Image.SCALE_SMOOTH);
        BufferedImage gray = new BufferedImage(32, 32, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = gray.createGraphics();
        graphics.drawImage(scaled, 0, 0, null);
        graphics.dispose();
        return new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null).filter(gray, null);
    }

    private long averageHash(BufferedImage image) {
        long total = 0;
        int[] pixels = new int[32 * 32];
        image.getRaster().getPixels(0, 0, 32, 32, pixels);
        for (int pixel : pixels) {
            total += pixel;
        }
        long average = total / pixels.length;
        long hash = 0L;
        for (int i = 0; i < 64; i++) {
            if (pixels[i] >= average) {
                hash |= (1L << i);
            }
        }
        return hash;
    }

    public record FaceVerificationResult(
        BigDecimal score,
        boolean matched
    ) {
    }
}
