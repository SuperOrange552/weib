package com.weib.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

public class CaptchaUtil {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 44;
    private static final int CODE_LENGTH = 4;
    private static final String CHAR_POOL = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();

    public static String generateCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHAR_POOL.charAt(RANDOM.nextInt(CHAR_POOL.length())));
        }
        return sb.toString();
    }

    public static BufferedImage generateImage(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // background
        g.setColor(new Color(20, 20, 30));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // noise dots
        g.setColor(new Color(0, 212, 255, 80));
        for (int i = 0; i < 60; i++) {
            int x = RANDOM.nextInt(WIDTH);
            int y = RANDOM.nextInt(HEIGHT);
            g.fillOval(x, y, 2, 2);
        }

        // noise lines
        for (int i = 0; i < 4; i++) {
            g.setColor(new Color(
                    0, 212, 255,
                    40 + RANDOM.nextInt(60)
            ));
            int x1 = RANDOM.nextInt(WIDTH);
            int y1 = RANDOM.nextInt(HEIGHT);
            int x2 = RANDOM.nextInt(WIDTH);
            int y2 = RANDOM.nextInt(HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }

        // draw characters
        Font[] fonts = {
                new Font("Arial", Font.BOLD, 26),
                new Font("Arial", Font.ITALIC, 26),
                new Font("Arial", Font.BOLD, 24),
        };

        for (int i = 0; i < code.length(); i++) {
            g.setFont(fonts[RANDOM.nextInt(fonts.length)]);
            g.setColor(new Color(
                    139 + RANDOM.nextInt(116),
                    92 + RANDOM.nextInt(116),
                    246,
                    200 + RANDOM.nextInt(55)
            ));
            int x = 10 + i * 26 + RANDOM.nextInt(6);
            int y = 28 + RANDOM.nextInt(8);
            double angle = (RANDOM.nextDouble() - 0.5) * 0.4;
            g.rotate(angle, x, y);
            g.drawString(String.valueOf(code.charAt(i)), x, y);
            g.rotate(-angle, x, y);
        }

        g.dispose();
        return image;
    }
}
