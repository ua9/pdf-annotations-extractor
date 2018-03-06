import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import static org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationSquareCircle.SUB_TYPE_SQUARE;
import static org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT;

class AnnotationRenderer {

    private static final int POINTS_IN_INCH = 72;

    private final PDDocument document;
    private final PDFRenderer renderer;
    private final int resolutionDotPerInch;

    private AnnotationRenderer(PDDocument document, int resolutionDotPerInch) {
        this.document = document;
        this.renderer = new PDFRenderer(document);
        this.resolutionDotPerInch = resolutionDotPerInch;
    }

    private RenderedImage getAnnotationImage(PDPage page, PDAnnotation annotation, int pageIndex) throws IOException {
        PDRectangle annotationRectangle = annotation.getRectangle();
        BufferedImage image = createImage(annotationRectangle);
        Graphics2D graphics = createGraphics(image);
        PDRectangle origPageCropBox = page.getCropBox();
        page.setCropBox(annotationRectangle);
//            boolean oldHidden = annotation.isHidden();
//            annotation.setHidden(true);
        renderer.renderPageToGraphics(pageIndex, graphics);
        page.setCropBox(origPageCropBox);
//            annotation.setHidden(oldHidden);
        graphics.dispose();
        return image;
    }

    private BufferedImage createImage(PDRectangle rect) {
        int scale = resolutionDotPerInch / POINTS_IN_INCH;
        int bitmapWidth  = Math.round(rect.getWidth()  * scale);
        int bitmapHeight = Math.round(rect.getHeight() * scale);
        return new BufferedImage(bitmapWidth, bitmapHeight, BufferedImage.TYPE_INT_RGB);
    }

    private Graphics2D createGraphics(BufferedImage image) {
        double scale = resolutionDotPerInch / POINTS_IN_INCH;
        AffineTransform transform = AffineTransform.getScaleInstance(scale, scale);
        Graphics2D graphics = image.createGraphics();
        graphics.setBackground(Color.WHITE);
        graphics.setTransform(transform);
        return graphics;
    }

    public void saveAnnotationsAsImages(String path) throws IOException {
        for (int pageIndex = 0, pagesCount = document.getPages().getCount(); pageIndex < pagesCount; pageIndex++) {
            for (int annotationIndex = 0, annotationsCount = document.getPage(pageIndex).getAnnotations().size();
                 annotationIndex < annotationsCount; annotationIndex++) {
                PDPage page = document.getPage(pageIndex);
                PDAnnotation annotation = page.getAnnotations().get(annotationIndex);
                if (annotation.getSubtype().equals(SUB_TYPE_HIGHLIGHT)
                        || annotation.getSubtype().equals(SUB_TYPE_SQUARE)) {
                    ImageIO.write(getAnnotationImage(page, annotation, pageIndex),
                            "png", new File(Paths.get(path,
                                    String.format("%s-%s.png", pageIndex, annotationIndex)).toString()));
                }
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        String filePath = args[0];
        int resolutionForHiDPIScreenRendering = 220; /* dpi */

        try (PDDocument doc = PDDocument.load(new File(filePath))) {
            AnnotationRenderer renderer = new AnnotationRenderer(doc, resolutionForHiDPIScreenRendering);
            renderer.saveAnnotationsAsImages("./");
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

}