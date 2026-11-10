import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Store art for Wiz Bill Box, drawn with the same geometry as res/drawable/ic_launcher_*.xml.
 *
 *   java tools/StoreArt.java icon     store-assets/icon-512.png 512
 *   java tools/StoreArt.java feature  store-assets/feature-graphic-1024x500.png
 *   java tools/StoreArt.java logo     store-assets/logo-1600x480.png
 *   java tools/StoreArt.java receipts <dir>                 (sample bill photos for the demo data)
 *   java tools/StoreArt.java frame    <in.png> <out.png> "<title>" "<subtitle>"
 */
public class StoreArt {
    static Color c(long argb) { return new Color((int) argb, true); }
    static final Color BG = c(0xFF0A0A0CL), YELLOW = c(0xFFFFD60AL), MUTED = c(0xFF9A9AA6L), TEXT = c(0xFFF2F2F7L);
    static final String SANS = pick("Helvetica Neue", "Helvetica", "Arial", "SansSerif");
    static final String MONO = pick("Menlo", "Courier New", "Monospaced");

    static String pick(String... names) {
        var all = java.util.Set.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String n : names) if (all.contains(n)) return n;
        return names[names.length - 1];
    }

    public static void main(String[] a) throws Exception {
        switch (a[0]) {
            case "icon" -> write(icon(Integer.parseInt(a[2]), true), a[1]);
            case "feature" -> write(feature(), a[1]);
            case "logo" -> write(logo(), a[1]);
            case "receipts" -> receipts(new File(a[1]));
            case "frame" -> write(frame(ImageIO.read(new File(a[1])), a[3], a[4]), a[2]);
            default -> throw new IllegalArgumentException(a[0]);
        }
    }

    static void write(BufferedImage img, String path) throws Exception {
        File f = new File(path);
        f.getAbsoluteFile().getParentFile().mkdirs();
        // Play wants opaque PNGs: flatten onto RGB.
        BufferedImage rgb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setColor(BG); g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.drawImage(img, 0, 0, null); g.dispose();
        ImageIO.write(rgb, path.endsWith(".jpg") ? "jpg" : "png", f);
        System.out.println("wrote " + path + " " + img.getWidth() + "x" + img.getHeight());
    }

    static Graphics2D smooth(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        return g;
    }

    // ---------- Icon ----------

    /** The launcher icon. [withBackground] false draws only the receipt + badge (for use on other art). */
    static BufferedImage icon(int size, boolean withBackground) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = smooth(img);
        double view = 76, s = size / view;
        g.scale(s, s);
        g.translate(-(108 - view) / 2, -(108 - view) / 2);
        if (withBackground) {
            g.setPaint(new RadialGradientPaint(new Point2D.Double(54, 44), 78f, new float[]{0f, .55f, 1f},
                new Color[]{c(0xFF2C2C35L), c(0xFF141418L), c(0xFF08080AL)}));
            g.fill(new Rectangle2D.Double(-10, -10, 128, 128));
            g.setPaint(new RadialGradientPaint(new Point2D.Double(54, 53), 30f, new float[]{0f, 1f},
                new Color[]{c(0x30FFD60AL), c(0x00FFD60AL)}));
            g.fill(new Ellipse2D.Double(24, 23, 60, 60));
        }
        drawGlyph(g);
        g.dispose();
        return img;
    }

    static void drawGlyph(Graphics2D g) {
        Path2D r = new Path2D.Double();
        r.moveTo(36, 30); r.quadTo(36, 27, 39, 27); r.lineTo(69, 27); r.quadTo(72, 27, 72, 30); r.lineTo(72, 79);
        double[] zx = {67.5, 63, 58.5, 54, 49.5, 45, 40.5, 36};
        for (int i = 0; i < zx.length; i++) r.lineTo(zx[i], i % 2 == 0 ? 75.5 : 79);
        r.closePath();
        g.setPaint(new LinearGradientPaint(36, 27, 72, 79, new float[]{0f, .6f, 1f},
            new Color[]{c(0xFFFFE45CL), c(0xFFFFD60AL), c(0xFFFF9F0AL)}));
        g.fill(r);
        g.setColor(c(0xFF1C1400L));
        g.fill(new Rectangle2D.Double(42, 36, 18, 3.2));
        g.fill(new Rectangle2D.Double(42, 43.5, 24, 3.2));
        g.fill(new Rectangle2D.Double(42, 51, 14, 3.2));
        g.fill(new Rectangle2D.Double(57, 60, 9, 5));
        g.setColor(c(0x991C1400L));
        g.fill(new Rectangle2D.Double(42, 61, 10, 3));
        // cloud badge
        g.setColor(c(0xFF141418L));
        g.fill(new Ellipse2D.Double(62, 56, 24, 24));
        Path2D cl = new Path2D.Double();
        cl.moveTo(68.5, 72.5); cl.lineTo(79.5, 72.5);
        cl.curveTo(81.7, 72.5, 83.2, 71, 83.2, 69); cl.curveTo(83.2, 67.1, 81.8, 65.6, 80, 65.5);
        cl.curveTo(79.5, 62.9, 77.3, 61, 74.6, 61); cl.curveTo(72.3, 61, 70.4, 62.4, 69.6, 64.4);
        cl.curveTo(67, 64.5, 65, 66.4, 65, 68.7); cl.curveTo(65, 70.8, 66.6, 72.5, 68.5, 72.5); cl.closePath();
        g.setColor(Color.WHITE); g.fill(cl);
        Path2D tick = new Path2D.Double(); tick.moveTo(71, 67.8); tick.lineTo(73.3, 70); tick.lineTo(77.3, 65.8);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(c(0xFF1E9E4AL)); g.draw(tick);
    }

    /** A rounded app-icon tile, like a launcher shows it. */
    static void drawIconTile(Graphics2D g, int x, int y, int size) {
        BufferedImage ic = icon(size, true);
        Shape clip = new RoundRectangle2D.Double(x, y, size, size, size * 0.46, size * 0.46);
        Shape old = g.getClip();
        g.setClip(clip); g.drawImage(ic, x, y, null); g.setClip(old);
        g.setColor(c(0x22FFFFFFL)); g.setStroke(new BasicStroke(2f)); g.draw(clip);
    }

    // ---------- Feature graphic & logo ----------

    static void darkBackdrop(Graphics2D g, int w, int h, double gx, double gy, float radius) {
        g.setPaint(new LinearGradientPaint(0, 0, w, h, new float[]{0f, 1f}, new Color[]{c(0xFF17171CL), c(0xFF060608L)}));
        g.fillRect(0, 0, w, h);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(gx, gy), radius, new float[]{0f, 1f},
            new Color[]{c(0x40FFD60AL), c(0x00FFD60AL)}));
        g.fillRect(0, 0, w, h);
    }

    static BufferedImage feature() {
        int W = 1024, H = 500;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = smooth(img);
        darkBackdrop(g, W, H, 230, 250, 330f);
        // floating receipts behind the icon
        receiptCard(g, 70, 95, 190, 270, -10, "BILL", 0.55f);
        receiptCard(g, 250, 130, 170, 250, 9, "INVOICE", 0.45f);
        drawIconTile(g, 120, 115, 270);

        int tx = 455;
        g.setColor(TEXT);
        g.setFont(new Font(SANS, Font.BOLD, 76));
        g.drawString("Wiz Bill Box", tx, 205);
        g.setColor(YELLOW);
        g.setFont(new Font(SANS, Font.BOLD, 34));
        g.drawString("Snap it. Save it. Find it.", tx, 260);
        g.setColor(MUTED);
        g.setFont(new Font(SANS, Font.PLAIN, 25));
        g.drawString("Bills & warranties, safe in your Google Drive", tx, 305);
        int cx = tx;
        for (String chip : new String[]{"Photo bills", "Drive backup", "Warranty alerts"}) cx = chip(g, cx, 340, chip) + 14;
        g.dispose();
        return img;
    }

    static int chip(Graphics2D g, int x, int y, String text) {
        g.setFont(new Font(SANS, Font.BOLD, 21));
        int w = g.getFontMetrics().stringWidth(text) + 36;
        g.setColor(c(0xFF1E1E24L)); g.fill(new RoundRectangle2D.Double(x, y, w, 44, 44, 44));
        g.setColor(c(0x66FFD60AL)); g.setStroke(new BasicStroke(1.5f)); g.draw(new RoundRectangle2D.Double(x, y, w, 44, 44, 44));
        g.setColor(TEXT); g.drawString(text, x + 18, y + 29);
        return x + w;
    }

    static void receiptCard(Graphics2D g, int x, int y, int w, int h, double deg, String head, float alpha) {
        Graphics2D r = (Graphics2D) g.create();
        r.rotate(Math.toRadians(deg), x + w / 2.0, y + h / 2.0);
        r.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        Path2D p = new Path2D.Double();
        p.moveTo(x, y); p.lineTo(x + w, y); p.lineTo(x + w, y + h);
        int teeth = 8; double tw = w / (double) teeth;
        for (int i = 1; i <= teeth; i++) p.lineTo(x + w - i * tw + (i % 2 == 1 ? tw / 2 : 0), y + h - (i % 2 == 1 ? 12 : 0));
        p.lineTo(x, y + h); p.closePath();
        r.setColor(c(0xFFF4F1E8L)); r.fill(p);
        r.setColor(c(0xFF3A3A40L));
        r.setFont(new Font(MONO, Font.BOLD, 18)); r.drawString(head, x + 18, y + 36);
        for (int i = 0; i < 6; i++) r.fill(new Rectangle2D.Double(x + 18, y + 58 + i * 26, (i % 3 == 2 ? 0.45 : 0.75) * (w - 36), 7));
        r.fill(new Rectangle2D.Double(x + w - 78, y + h - 58, 60, 12));
        r.dispose();
    }

    static BufferedImage logo() {
        int W = 1600, H = 480;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = smooth(img);
        darkBackdrop(g, W, H, 260, 240, 360f);
        drawIconTile(g, 110, 90, 300);
        g.setColor(TEXT); g.setFont(new Font(SANS, Font.BOLD, 132));
        g.drawString("Wiz Bill Box", 470, 260);
        g.setColor(YELLOW); g.setFont(new Font(SANS, Font.BOLD, 46));
        g.drawString("Bills & warranties in your Google Drive", 476, 340);
        g.dispose();
        return img;
    }

    // ---------- Sample bill photos (fictional shops) ----------

    record Item(String name, String amt) {}

    static void receipts(File dir) throws Exception {
        dir.mkdirs();
        receiptPhoto(new File(dir, "fridge.jpg"), "SHARMA ELECTRONICS", "12 Main Bazaar, Ph 98765 43210",
            "TAX INVOICE  SE/2611/0457", "02-11-2026", List.of(
                new Item("Refrigerator 253L 2-door", "21,177.97"), new Item("Stabilizer (free)", "0.00"),
                new Item("CGST 9%", "1,906.02"), new Item("SGST 9%", "1,906.01")), "24,990.00", "UPI", -2.5, 0xFF8A8F96L);
        receiptPhoto(new File(dir, "grocery.jpg"), "GUPTA KIRANA STORE", "Shop 4, Sector 7 Market",
            "CASH MEMO  4821", "08-11-2026", List.of(
                new Item("Atta 10 kg", "455.00"), new Item("Basmati rice 5 kg", "640.00"), new Item("Toor dal 2 kg", "330.00"),
                new Item("Sunflower oil 5 L", "860.00"), new Item("Tea 1 kg", "520.00"), new Item("Sugar 5 kg", "230.00"),
                new Item("Spices & misc", "230.50")), "3,265.50", "UPI", 1.8, 0xFF6F5B49L);
        receiptPhoto(new File(dir, "phone.jpg"), "TECHZONE MOBILES", "G-18, City Centre Mall",
            "INVOICE  TZ-25-1189", "20-11-2025", List.of(
                new Item("Smartphone 8/128 GB", "15,676.27"), new Item("Back cover", "0.00"),
                new Item("CGST 9%", "1,411.37"), new Item("SGST 9%", "1,411.36")), "18,499.00", "EMI 6 months", -1.2, 0xFF4E5560L);
        receiptPhoto(new File(dir, "table.jpg"), "URBAN FURNISH", "Plot 9, Furniture Market",
            "INVOICE  UF/0938", "18-10-2026", List.of(
                new Item("Study table (engineered wood)", "5,200.00"), new Item("Ergonomic chair", "2,215.25"),
                new Item("GST 18%", "1,334.75")), "8,750.00", "Card", 2.4, 0xFF7B6A58L);
    }

    static void receiptPhoto(File out, String shop, String addr, String inv, String date, List<Item> items,
                             String total, String paid, double deg, long table) throws Exception {
        int W = 1200, H = 1600;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = smooth(img);
        Color t = c(table);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(W / 2.0, H / 2.0), 1100f, new float[]{0f, 1f},
            new Color[]{t.brighter(), t.darker().darker()}));
        g.fillRect(0, 0, W, H);
        int pw = 820, ph = 1320, px = (W - pw) / 2, py = (H - ph) / 2;
        Graphics2D p = (Graphics2D) g.create();
        p.rotate(Math.toRadians(deg), W / 2.0, H / 2.0);
        p.setColor(c(0x55000000L)); p.fill(new RoundRectangle2D.Double(px + 14, py + 18, pw, ph, 8, 8));
        p.setPaint(new LinearGradientPaint(px, py, px + pw, py + ph, new float[]{0f, 1f},
            new Color[]{c(0xFFFBFAF5L), c(0xFFECE8DDL)}));
        p.fill(new RoundRectangle2D.Double(px, py, pw, ph, 8, 8));
        p.setColor(c(0xFF222226L));
        int y = py + 110;
        p.setFont(new Font(MONO, Font.BOLD, 46)); y = center(p, shop, px, pw, y) + 50;
        p.setFont(new Font(MONO, Font.PLAIN, 28)); y = center(p, addr, px, pw, y) + 40;
        y = center(p, "GSTIN 06ABCDE1234F1Z5", px, pw, y) + 50;
        p.setFont(new Font(MONO, Font.BOLD, 30)); y = center(p, inv, px, pw, y) + 44;
        p.setFont(new Font(MONO, Font.PLAIN, 28)); y = center(p, "Date: " + date, px, pw, y) + 36;
        dashed(p, px + 50, px + pw - 50, y); y += 60;
        for (Item it : items) {
            p.drawString(it.name(), px + 60, y);
            int aw = p.getFontMetrics().stringWidth(it.amt());
            p.drawString(it.amt(), px + pw - 60 - aw, y);
            y += 50;
        }
        dashed(p, px + 50, px + pw - 50, y - 10); y += 50;
        p.setFont(new Font(MONO, Font.BOLD, 40));
        p.drawString("TOTAL", px + 60, y);
        String tt = "Rs " + total;
        p.drawString(tt, px + pw - 60 - p.getFontMetrics().stringWidth(tt), y);
        y += 60;
        p.setFont(new Font(MONO, Font.PLAIN, 28));
        p.drawString("Paid by: " + paid, px + 60, y); y += 90;
        center(p, "Thank you! Visit again", px, pw, y);
        // barcode
        int bx = px + 210, by = py + ph - 170;
        java.util.Random rnd = new java.util.Random(shop.hashCode());
        for (int x = 0; x < 400; ) { int w = 2 + rnd.nextInt(5); if (rnd.nextBoolean()) p.fillRect(bx + x, by, w, 80); x += w + 2; }
        p.dispose();
        g.dispose();
        BufferedImage rgb = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        rgb.createGraphics().drawImage(img, 0, 0, null);
        ImageIO.write(rgb, "jpg", out);
        System.out.println("wrote " + out);
    }

    static int center(Graphics2D g, String s, int x, int w, int y) {
        g.drawString(s, x + (w - g.getFontMetrics().stringWidth(s)) / 2, y);
        return y;
    }

    static void dashed(Graphics2D g, int x1, int x2, int y) {
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{10f, 8f}, 0f));
        g.drawLine(x1, y, x2, y);
        g.setStroke(old);
    }

    // ---------- Framed screenshots (1080x1920, 9:16) ----------

    static BufferedImage frame(BufferedImage shot, String title, String subtitle) {
        int W = 1080, H = 1920;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = smooth(img);
        darkBackdrop(g, W, H, W / 2.0, 260, 700f);

        g.setColor(TEXT);
        g.setFont(new Font(SANS, Font.BOLD, 70));
        int y = 170;
        for (String line : wrap(g, title, W - 120)) { center(g, line, 0, W, y); y += 82; }
        g.setColor(YELLOW);
        g.setFont(new Font(SANS, Font.PLAIN, 38));
        y += 4;
        for (String line : wrap(g, subtitle, W - 160)) { center(g, line, 0, W, y); y += 50; }

        int top = Math.max(y + 30, 400);
        int sh = H - top + 60;                       // let the phone run off the bottom edge
        int sw = (int) Math.round(shot.getWidth() * (sh / (double) shot.getHeight()));
        if (sw > 820) { sw = 820; sh = (int) Math.round(shot.getHeight() * (sw / (double) shot.getWidth())); }
        int sx = (W - sw) / 2;
        int bezel = 18;
        RoundRectangle2D body = new RoundRectangle2D.Double(sx - bezel, top - bezel, sw + 2 * bezel, sh + 2 * bezel, 90, 90);
        g.setColor(c(0x66000000L)); g.fill(new RoundRectangle2D.Double(sx - bezel + 8, top - bezel + 16, sw + 2 * bezel, sh + 2 * bezel, 90, 90));
        g.setColor(c(0xFF1B1B20L)); g.fill(body);
        g.setColor(c(0xFF34343CL)); g.setStroke(new BasicStroke(3f)); g.draw(body);
        Shape screen = new RoundRectangle2D.Double(sx, top, sw, sh, 64, 64);
        Shape old = g.getClip();
        g.setClip(screen);
        g.drawImage(shot.getScaledInstance(sw, sh, Image.SCALE_SMOOTH), sx, top, null);
        g.setClip(old);
        g.dispose();
        return img;
    }

    static List<String> wrap(Graphics2D g, String text, int max) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String w : text.split(" ")) {
            String t = line.isEmpty() ? w : line + " " + w;
            if (g.getFontMetrics().stringWidth(t) > max && !line.isEmpty()) { out.add(line.toString()); line = new StringBuilder(w); }
            else line = new StringBuilder(t);
        }
        if (!line.isEmpty()) out.add(line.toString());
        return out;
    }
}
