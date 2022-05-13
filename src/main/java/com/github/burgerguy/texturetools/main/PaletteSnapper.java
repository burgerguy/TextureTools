/**
 * The MIT License (MIT)
 * Copyright (c) 2012 ColorMine.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.burgerguy.texturetools.main;

import java.awt.Toolkit;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import javax.imageio.ImageIO;
import sun.awt.image.ToolkitImage;

public class PaletteSnapper {

    private static final int[] RGB_COLOR_PALETTE = new int[] {
            0x181818,
            0x272727,
            0x2e2e2e,
            0x303030,
            0x505050,
            0x5a5e63,
            0x536174,
            0x656565,
            0x63666a,
            0x6c6c6c,
            0x6e788c,
            0xb3b5c3,
            0xbebebe,
            0xc8c8c8,
            0xe6e3ce,
            0xdddddd,
            0xe2e2e2,
            0xc81c00,
            0x69f0d9
    };
    private static final double[][] LAB_COLOR_PALETTE = Arrays.stream(RGB_COLOR_PALETTE).mapToObj(PaletteSnapper::rgbToLab).toArray(double[][]::new);
    private static final int ALPHA_MASK = 0xFF000000;
    private static final String IMAGE_PATH = "C:\\Users\\Ryan\\Documents\\GitHub\\recordable\\src\\main\\resources\\assets\\recordable\\textures\\block\\test";
    private static final String IMAGE_FORMAT = "png";
    private static final String EXT_SEPARATOR = ".";

    public static void main(String[] args) throws IOException {
        BufferedImage image = ImageIO.read(new File(IMAGE_PATH + EXT_SEPARATOR + IMAGE_FORMAT));
        ImageFilter paletteSnapFilter = new RGBImageFilter() {
            @Override
            public int filterRGB(int x, int y, int rgb) {
                double[] colorLAB = rgbToLab(rgb);
                // if palette gets too big, swap to KD Tree with nearest neighbor search
                // for now, we can just brute force
                int nearestPaletteColor = 0;
                double nearestDeltaK = Double.POSITIVE_INFINITY; // furthest as possible
                for (int i = 0; i < LAB_COLOR_PALETTE.length; i++) {
                    double[] pColorLAB = LAB_COLOR_PALETTE[i];
                    double deltaK = compareLAB(colorLAB, pColorLAB);
                    if (deltaK < nearestDeltaK) {
                        nearestPaletteColor = RGB_COLOR_PALETTE[i];
                        nearestDeltaK = deltaK;
                    }
                }

                return (nearestPaletteColor & ~ALPHA_MASK) | (rgb & ALPHA_MASK);
            }
        };
        ImageProducer imageProducerSnapped = new FilteredImageSource(image.getSource(), paletteSnapFilter);
        ToolkitImage filteredImage = (ToolkitImage) Toolkit.getDefaultToolkit().createImage(imageProducerSnapped);
        filteredImage.getWidth(null); // triggers an image load
        RenderedImage writableImage = filteredImage.getBufferedImage();
        ImageIO.write(writableImage, IMAGE_FORMAT, new File(IMAGE_PATH + "_modified" + EXT_SEPARATOR + IMAGE_FORMAT));
    }

    public static double compareLAB(double[] lab1, double[] lab2) {
        return Math.sqrt(Math.pow(lab1[0] - lab2[0], 2) + Math.pow(lab1[1] - lab2[1], 2) + Math.pow(lab1[2] - lab2[2], 2));
    }

    public static double[] rgbToLab(int rgb) {
        double r = pivotRgb(((rgb >> 16) & 0xFF) / 255.0);
        double g = pivotRgb(((rgb >> 8) & 0xFF) / 255.0);
        double bl = pivotRgb((rgb & 0xFF) / 255.0);

        // Observer. = 2°, Illuminant = D65
        double x  = r * 0.4124 + g * 0.3576 + bl * 0.1805;
        double y = r * 0.2126 + g * 0.7152 + bl * 0.0722;
        double z = r * 0.0193 + g * 0.1192 + bl * 0.9505;

        final double REF_X = 95.047; // Observer= 2°, Illuminant= D65
        final double REF_Y = 100.000;
        final double REF_Z = 108.883;

        double l = pivotXyz(x / REF_X);
        double a = pivotXyz(y / REF_Y);
        double b = pivotXyz(z / REF_Z);

        return new double[] { l, a, b };
    }

    private static double pivotRgb(double n) {
        return (n > 0.04045 ? Math.pow((n + 0.055) / 1.055, 2.4) : n / 12.92) * 100;
    }

    private static double pivotXyz(double n) {
        double i = Math.cbrt(n);
        return n > 0.008856 ? i : 7.787 * n + 16.0 / 116.0; // n > 0.008856 ? i : 7.787 * n + 16 / 116
    }
}
