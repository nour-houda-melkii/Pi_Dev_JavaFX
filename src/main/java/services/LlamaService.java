package services;

import javafx.scene.image.Image;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.Random;
import java.util.UUID;
import javax.imageio.ImageIO;
import java.util.concurrent.CompletableFuture;
import java.awt.geom.Rectangle2D;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.Shape;
import java.awt.font.TextLayout;
import java.awt.GradientPaint;
import java.awt.AlphaComposite;
import java.awt.FontMetrics;

public class LlamaService {
    private static final String IMAGE_DIR = "src/main/resources/affiches/";
    private static final String OLLAMA_EXECUTABLE = "ollama"; // Utiliser la commande ollama

    // Couleurs professionnelles pour les affiches
    private static final Color[] BACKGROUND_COLORS = {
        new Color(41, 128, 185),    // Bleu
        new Color(142, 68, 173),    // Violet
        new Color(22, 160, 133),    // Vert émeraude
        new Color(44, 62, 80),      // Bleu foncé
        new Color(211, 84, 0),      // Rouge orangé
        new Color(46, 64, 89),      // Bleu nuit
        new Color(31, 97, 141)      // Bleu roi
    };
    
    private static final Color[] ACCENT_COLORS = {
        new Color(243, 156, 18),   // Jaune
        new Color(230, 126, 34),   // Orange
        new Color(231, 76, 60),    // Rouge
        new Color(241, 196, 15),   // Jaune doré
        new Color(26, 188, 156),   // Turquoise
        new Color(155, 89, 182),   // Lilas
        new Color(52, 152, 219)    // Bleu clair
    };
    
    /**
     * Génère une affiche professionnelle pour un événement
     * @param title Le titre de l'événement
     * @param startDate La date de début au format string
     * @param endDate La date de fin au format string
     * @return Un CompletableFuture avec le chemin vers l'image générée
     */
    public CompletableFuture<String> generateEventPoster(String title, String startDate, String endDate) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Créer un nom de fichier unique pour l'image générée
                String imageName = "event_" + UUID.randomUUID().toString().substring(0, 8) + ".png";
                String outputPath = IMAGE_DIR + imageName;
                
                // Créer le répertoire s'il n'existe pas
                Files.createDirectories(Paths.get(IMAGE_DIR));
                
                System.out.println("Création d'une affiche avancée pour: " + title);
                
                // Créer une image haute qualité
                int width = 1200;
                int height = 800;
                BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = image.createGraphics();
                
                // Activer l'antialiasing pour un meilleur rendu
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                
                // Sélectionner des couleurs aléatoires
                Random random = new Random(title.hashCode()); // Utiliser le titre comme seed pour obtenir le même design pour le même titre
                int colorIndex = random.nextInt(BACKGROUND_COLORS.length);
                Color bgColor = BACKGROUND_COLORS[colorIndex];
                Color accentColor = ACCENT_COLORS[colorIndex];
                
                // Créer un arrière-plan élaboré avec motif
                createAdvancedBackground(graphics, width, height, bgColor);
                
                // Ajouter un cadre moderne
                drawModernFrame(graphics, width, height, accentColor);
                
                // Ajouter un badge "Actif" dans le coin supérieur droit
                drawStatusBadge(graphics, width, "Actif", new Color(46, 204, 113));
                
                // Ajouter un en-tête
                drawHeader(graphics, width, "validation", new Color(25, 181, 254));
                
                // Dessiner le titre avec effet de texte
                drawStylizedTitle(graphics, title, width/2, height/4 + 10, accentColor);
                
                // Dessiner les dates avec style
                drawStylizedDateRange(graphics, startDate, endDate, width/2, height/4 + 80);
                
                // Ajouter éléments graphiques liés au thème
                drawThematicGraphics(graphics, title.toLowerCase(), width/2, height/2, accentColor, random);
                
                // Ajouter des éléments de marketing
                drawMarketingElements(graphics, width, height, accentColor);
                
                graphics.dispose();
                
                System.out.println("Enregistrement de l'image à: " + outputPath);
                
                // Enregistrer l'image
                ImageIO.write(image, "png", new File(outputPath));
                
                System.out.println("Image enregistrée avec succès!");
                
                return imageName;
            } catch (Exception e) {
                System.err.println("Erreur lors de la génération de l'image: " + e.getMessage());
                e.printStackTrace();
                try {
                    return generateFallbackImage(title, startDate, endDate);
                } catch (IOException ex) {
                    throw new RuntimeException("Échec de la génération de l'image de secours", ex);
                }
            }
        });
    }
    
    /**
     * Crée un arrière-plan élaboré avec dégradé et motifs
     */
    private void createAdvancedBackground(Graphics2D g, int width, int height, Color baseColor) {
        // Pour le don du sang, utiliser un style différent
        if (baseColor.getRed() > 100 && baseColor.getGreen() < 100) {
            createBloodDonationBackground(g, width, height);
            return;
        }
        
        // Code existant pour les autres thèmes
        Point2D center = new Point2D.Float(width/2, height/2);
        float radius = (float)Math.max(width, height);
        float[] dist = {0.0f, 0.7f, 1.0f};
        
        // Couleurs pour le dégradé (plus clair au centre, plus foncé aux bords)
        Color[] colors = {
            new Color(
                Math.min(255, Math.max(0, baseColor.getRed() + 20)),
                Math.min(255, Math.max(0, baseColor.getGreen() + 20)),
                Math.min(255, Math.max(0, baseColor.getBlue() + 20))
            ),
            baseColor,
            new Color(
                Math.max(0, baseColor.getRed() - 50),
                Math.max(0, baseColor.getGreen() - 50),
                Math.max(0, baseColor.getBlue() - 50)
            )
        };
        
        RadialGradientPaint paint = new RadialGradientPaint(center, radius, dist, colors);
        g.setPaint(paint);
        g.fillRect(0, 0, width, height);
        
        // Ajouter des cercles semi-transparents pour un effet de profondeur
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
        Random random = new Random(12345); // Seed fixe pour obtenir le même motif
        
        for (int i = 0; i < 15; i++) {
            int size = random.nextInt(300) + 50;
            int x = random.nextInt(width) - size/2;
            int y = random.nextInt(height) - size/2;
            
            Color circleColor = new Color(
                Math.min(255, Math.max(0, baseColor.getRed() + random.nextInt(80) - 40)),
                Math.min(255, Math.max(0, baseColor.getGreen() + random.nextInt(80) - 40)),
                Math.min(255, Math.max(0, baseColor.getBlue() + random.nextInt(80) - 40)),
                100 + random.nextInt(100)
            );
            
            g.setColor(circleColor);
            g.fillOval(x, y, size, size);
        }
        
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }
    
    /**
     * Crée un arrière-plan spécifique pour le don du sang
     */
    private void createBloodDonationBackground(Graphics2D g, int width, int height) {
        // Créer un dégradé pour le fond (blanc vers rouge)
        GradientPaint bgGradient = new GradientPaint(
            0, 0, new Color(240, 240, 240),
            width, height, new Color(245, 220, 220)
        );
        g.setPaint(bgGradient);
        g.fillRect(0, 0, width, height);
        
        // Ajouter un en-tête stylisé bleu (comme dans l'image)
        g.setColor(new Color(30, 144, 255));
        g.fillRect(0, 0, width, 50);
        
        // Effet de vague sur l'en-tête
        g.setColor(new Color(65, 169, 255));
        int waveHeight = 15;
        for (int x = 0; x < width; x += 20) {
            g.fillOval(x, 40, 40, waveHeight);
        }
        
        // Ajouter des éléments visuels subtils en arrière-plan
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.05f));
        g.setColor(new Color(200, 0, 0));
        
        // Croix rouges symbolisant la santé
        int crossSize = 100;
        Random random = new Random(54321);
        for (int i = 0; i < 6; i++) {
            int x = random.nextInt(width);
            int y = 100 + random.nextInt(height - 200);
            
            g.fillRect(x - crossSize/6, y - crossSize/2, crossSize/3, crossSize);
            g.fillRect(x - crossSize/2, y - crossSize/6, crossSize, crossSize/3);
        }
        
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }
    
    /**
     * Dessine un cadre moderne avec bordure sophistiquée
     */
    private void drawModernFrame(Graphics2D g, int width, int height, Color accentColor) {
        // Cadre extérieur avec coins arrondis
        int borderThickness = 2;
        int margin = 20;
        int arcSize = 20;
        
        // Bordure avec dégradé subtil
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
        g.setColor(accentColor);
        g.setStroke(new BasicStroke(borderThickness));
        g.drawRoundRect(margin, margin, width - 2*margin, height - 2*margin, arcSize, arcSize);
        
        // Effet de double bordure
        g.setColor(new Color(255, 255, 255, 120)); // Bordure blanche semi-transparente
        g.setStroke(new BasicStroke(1));
        g.drawRoundRect(margin+4, margin+4, width - 2*margin - 8, height - 2*margin - 8, arcSize-4, arcSize-4);
        
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }
    
    /**
     * Dessine un badge de statut dans le coin supérieur droit
     */
    private void drawStatusBadge(Graphics2D g, int width, String status, Color statusColor) {
        // Sauvegarder les paramètres graphiques
        Composite originalComposite = g.getComposite();
        
        // Badge arrondi
        int badgeWidth = 80;
        int badgeHeight = 30;
        int badgeX = width - badgeWidth - 40;
        int badgeY = 75;
        
        // Fond du badge
        g.setColor(statusColor);
        g.fillRoundRect(badgeX, badgeY, badgeWidth, badgeHeight, 15, 15);
        
        // Effet de lueur
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
        g.setColor(Color.WHITE);
        g.fillRoundRect(badgeX+3, badgeY+3, badgeWidth-6, badgeHeight/2-3, 10, 10);
        
        // Texte du badge
        g.setComposite(originalComposite);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        FontMetrics metrics = g.getFontMetrics();
        int textX = badgeX + (badgeWidth - metrics.stringWidth(status)) / 2;
        int textY = badgeY + ((badgeHeight - metrics.getHeight()) / 2) + metrics.getAscent();
        g.drawString(status, textX, textY);
    }
    
    /**
     * Dessine un en-tête élégant
     */
    private void drawHeader(Graphics2D g, int width, String headerText, Color headerColor) {
        // Rectangle arrondi pour l'en-tête
        int headerHeight = 40;
        int headerY = 10;
        
        // Dégradé pour l'en-tête
        GradientPaint headerGradient = new GradientPaint(
            0, headerY, headerColor,
            width, headerY + headerHeight, 
            new Color(
                Math.max(0, headerColor.getRed() - 40),
                Math.max(0, headerColor.getGreen() - 40),
                Math.max(0, headerColor.getBlue() - 40)
            )
        );
        
        g.setPaint(headerGradient);
        g.fillRoundRect(width/8, headerY, 3*width/4, headerHeight, 20, 20);
        
        // Effet de brillance
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
        g.setColor(Color.WHITE);
        g.fillRoundRect(width/8 + 5, headerY + 3, 3*width/4 - 10, headerHeight/2 - 3, 18, 10);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        
        // Texte de l'en-tête
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        FontMetrics metrics = g.getFontMetrics();
        int textX = width/2 - metrics.stringWidth(headerText)/2;
        int textY = headerY + (headerHeight - metrics.getHeight())/2 + metrics.getAscent();
        g.drawString(headerText, textX, textY);
    }
    
    /**
     * Dessine un titre stylisé avec effets
     */
    private void drawStylizedTitle(Graphics2D g, String title, int centerX, int centerY, Color accentColor) {
        // Détection du don de sang pour un style spécifique
        if (title.toLowerCase().contains("sang") || title.toLowerCase().contains("don")) {
            drawBloodDonationTitle(g, title, centerX, centerY);
            return;
        }
        
        // Code existant pour les autres titres
        Font titleFont = new Font("Arial", Font.BOLD, 48);
        g.setFont(titleFont);
        
        // Mesurer le texte
        FontRenderContext frc = g.getFontRenderContext();
        TextLayout textLayout = new TextLayout(title, titleFont, frc);
        Rectangle2D bounds = textLayout.getBounds();
        
        // Calculer la position x pour centrer
        float titleX = (float)(centerX - bounds.getWidth()/2);
        float titleY = centerY;
        
        // Ombre du texte (décalée)
        g.setColor(new Color(0, 0, 0, 80));
        textLayout.draw(g, titleX + 3, titleY + 3);
        
        // Effet de texte en dégradé
        GradientPaint textGradient = new GradientPaint(
            titleX, titleY - 30, Color.WHITE,
            titleX, titleY + 10, accentColor
        );
        g.setPaint(textGradient);
        textLayout.draw(g, titleX, titleY);
        
        // Contour fin du texte (optionnel)
        g.setColor(new Color(255, 255, 255, 120));
        g.setStroke(new BasicStroke(0.5f));
        Shape outline = textLayout.getOutline(AffineTransform.getTranslateInstance(titleX, titleY));
        g.draw(outline);
    }
    
    /**
     * Dessine un titre spécial pour le thème du don de sang
     */
    private void drawBloodDonationTitle(Graphics2D g, String title, int centerX, int centerY) {
        // Police spéciale en gras et grande taille pour le don de sang
        Font bloodFont = new Font("Impact", Font.BOLD, 60);
        g.setFont(bloodFont);
        
        // Mesurer le texte
        FontRenderContext frc = g.getFontRenderContext();
        TextLayout textLayout = new TextLayout(title, bloodFont, frc);
        Rectangle2D bounds = textLayout.getBounds();
        
        // Calculer la position pour centrer
        float titleX = (float)(centerX - bounds.getWidth()/2);
        float titleY = centerY;
        
        // Effet d'ombre portée rouge foncé
        g.setColor(new Color(139, 0, 0, 120));
        textLayout.draw(g, titleX + 4, titleY + 4);
        
        // Titre principal en rouge vif
        g.setColor(new Color(220, 20, 20));
        textLayout.draw(g, titleX, titleY);
        
        // Contour blanc
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(1.5f));
        Shape outline = textLayout.getOutline(AffineTransform.getTranslateInstance(titleX, titleY));
        g.draw(outline);
        
        // Ajouter un effet de texte 3D avec une deuxième version du texte en blanc transparent en dessous
        g.setColor(new Color(255, 255, 255, 160));
        Font shadowFont = new Font("Impact", Font.BOLD, 60);
        TextLayout shadowLayout = new TextLayout(title, shadowFont, frc);
        shadowLayout.draw(g, titleX + 1, titleY + 60);
        
        // Double titre pour effet de profondeur
        g.setColor(new Color(255, 255, 255, 200));
        TextLayout doubleLayout = new TextLayout("Don du", new Font("Arial", Font.BOLD, 30), frc);
        Rectangle2D doubleBounds = doubleLayout.getBounds();
        doubleLayout.draw(g, (float)(centerX - doubleBounds.getWidth()/2), titleY + 70);
        
        TextLayout sangLayout = new TextLayout("Sang", new Font("Arial", Font.BOLD, 50), frc);
        Rectangle2D sangBounds = sangLayout.getBounds();
        g.setColor(new Color(255, 255, 255, 240));
        sangLayout.draw(g, (float)(centerX - sangBounds.getWidth()/2), titleY + 120);
    }
    
    /**
     * Affiche la plage de dates avec style
     */
    private void drawStylizedDateRange(Graphics2D g, String startDate, String endDate, int centerX, int centerY) {
        String dateRange = "Du " + startDate + " au " + endDate;
        
        // Créer un "label" en forme de rectangle avec coins arrondis
        Font dateFont = new Font("Arial", Font.PLAIN, 16);
        g.setFont(dateFont);
        
        FontMetrics metrics = g.getFontMetrics();
        int textWidth = metrics.stringWidth(dateRange);
        int padding = 10;
        
        // Fond léger pour la date
        g.setColor(new Color(255, 255, 255, 40));
        g.fillRoundRect(
            centerX - textWidth/2 - padding,
            centerY - metrics.getAscent() - padding/2,
            textWidth + padding*2,
            metrics.getHeight() + padding,
            10, 10
        );
        
        // Texte des dates
        g.setColor(Color.WHITE);
        g.drawString(dateRange, centerX - textWidth/2, centerY);
    }
    
    /**
     * Dessine des éléments graphiques thématiques selon le titre
     */
    private void drawThematicGraphics(Graphics2D g, String title, int centerX, int centerY, Color accentColor, Random random) {
        // Sauvegarder le composite original
        Composite originalComposite = g.getComposite();
        
        // Essayer de détecter le thème à partir du titre
        if (title.contains("sang") || title.contains("don") || title.contains("blood")) {
            // Thème don du sang
            drawBloodDonationGraphics(g, centerX, centerY, accentColor);
        }
        else if (title.contains("sport") || title.contains("marathon") || title.contains("course")) {
            // Thème sportif
            drawSportsGraphics(g, centerX, centerY, accentColor);
        }
        else if (title.contains("musique") || title.contains("concert") || title.contains("festival")) {
            // Thème musical
            drawMusicGraphics(g, centerX, centerY, accentColor);
        }
        else if (title.contains("conférence") || title.contains("talk") || title.contains("atelier")) {
            // Thème conférence
            drawConferenceGraphics(g, centerX, centerY, accentColor);
        }
        else {
            // Thème générique avec formes géométriques modernes
            drawModernGeometricTheme(g, centerX, centerY, accentColor, random);
        }
        
        // Restaurer le composite
        g.setComposite(originalComposite);
    }
    
    /**
     * Dessine des graphiques spécifiques au thème du don de sang
     */
    private void drawBloodDonationGraphics(Graphics2D g, int centerX, int centerY, Color accentColor) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));
        
        try {
            // Créer directement une illustration stylisée de personnes
            drawStylizedPeople(g, centerX, centerY);
            
            // Décaler le centre pour les gouttes de sang
            centerY -= 120;
            
            // Dessiner des gouttes de sang stylisées autour du titre
            // Symbole représentant le don de sang (plusieurs gouttes)
            int dropSize = 30;
            Random random = new Random(123); // Seed fixe pour avoir les mêmes positions
            
            // Dessiner plusieurs gouttes de sang de différentes tailles
            for (int i = 0; i < 8; i++) {
                // Position aléatoire autour du centre
                int dropX = centerX + (random.nextInt(500) - 250);
                int dropY = centerY - 100 + (random.nextInt(200) - 100);
                int currentDropSize = dropSize + random.nextInt(20) - 10;
                
                // Couleur variable pour les gouttes
                if (random.nextBoolean()) {
                    g.setColor(new Color(231, 76, 60)); // Rouge vif
                } else {
                    g.setColor(new Color(192, 57, 43)); // Rouge plus foncé
                }
                
                // Dessiner la goutte
                int[] xPoints = {
                    dropX, 
                    dropX - currentDropSize/2, 
                    dropX, 
                    dropX + currentDropSize/2
                };
                int[] yPoints = {
                    dropY - currentDropSize,
                    dropY,
                    dropY + currentDropSize/4,
                    dropY
                };
                
                g.fillPolygon(xPoints, yPoints, 4);
                
                // Ajouter brillance
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
                g.setColor(new Color(255, 255, 255, 180));
                
                int[] xHighlight = {
                    dropX - currentDropSize/4,
                    dropX - currentDropSize/3,
                    dropX - currentDropSize/6
                };
                int[] yHighlight = {
                    dropY - currentDropSize/2,
                    dropY - currentDropSize/4,
                    dropY - currentDropSize/3
                };
                
                g.fillPolygon(xHighlight, yHighlight, 3);
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));
            }
            
            // Ajouter une grande goutte de sang au premier plan
            int mainDropX = centerX + 250;
            int mainDropY = centerY - 50;
            int mainDropSize = 60;
            
            g.setColor(new Color(231, 76, 60, 230)); // Rouge vif semi-transparent
            
            // Dessiner la goutte principale
            int[] xMainPoints = {
                mainDropX, 
                mainDropX - mainDropSize/2, 
                mainDropX, 
                mainDropX + mainDropSize/2
            };
            int[] yMainPoints = {
                mainDropY - mainDropSize,
                mainDropY,
                mainDropY + mainDropSize/4,
                mainDropY
            };
            
            g.fillPolygon(xMainPoints, yMainPoints, 4);
            
            // Brillance
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
            g.setColor(new Color(255, 255, 255, 180));
            
            int[] xMainHighlight = {
                mainDropX - mainDropSize/4,
                mainDropX - mainDropSize/3,
                mainDropX - mainDropSize/6
            };
            int[] yMainHighlight = {
                mainDropY - mainDropSize/2,
                mainDropY - mainDropSize/4,
                mainDropY - mainDropSize/3
            };
            
            g.fillPolygon(xMainHighlight, yMainHighlight, 3);
            
            // Ajouter des effets de brillance supplémentaires
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
            g.setColor(Color.WHITE);
            g.fillOval(centerX - 200, centerY - 150, 80, 80);
            g.fillOval(centerX + 150, centerY - 100, 60, 60);
            
        } finally {
            // Restaurer le composite à sa valeur normale
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
    }
    
    /**
     * Dessine une illustration stylisée de personnes faisant un don de sang
     */
    private void drawStylizedPeople(Graphics2D g, int centerX, int centerY) {
        // Créer un fond blanc arrondi pour l'illustration
        int width = 400;
        int height = 200;
        
        // Dessiner un rectangle arrondi comme fond
        g.setColor(new Color(255, 255, 255, 180));
        g.fillRoundRect(centerX - width/2, centerY - height/2, width, height, 20, 20);
        
        // Couleurs pour les silhouettes
        Color[] personColors = {
            new Color(231, 76, 60),  // Rouge
            new Color(41, 128, 185), // Bleu
            new Color(243, 156, 18), // Jaune
            new Color(46, 204, 113), // Vert
            new Color(142, 68, 173), // Violet
            new Color(52, 152, 219), // Bleu clair
            new Color(230, 126, 34), // Orange
            new Color(155, 89, 182)  // Lilas
        };
        
        // Dessiner plusieurs silhouettes côte à côte
        int personCount = 7;
        int personWidth = width / personCount;
        
        for (int i = 0; i < personCount; i++) {
            int x = centerX - width/2 + i * personWidth + personWidth/2;
            int y = centerY;
            
            // Choisir une couleur pour chaque personne
            g.setColor(personColors[i % personColors.length]);
            
            // Dessiner une silhouette simple
            drawSimplePerson(g, x, y, personWidth * 0.7f, personColors[i % personColors.length]);
        }
        
        // Ajouter un bras levé pour certaines personnes (celles au milieu)
        g.setColor(personColors[3]);
        int armX = centerX;
        int armY = centerY - 20;
        g.setStroke(new BasicStroke(5));
        g.drawLine(armX, armY, armX, armY - 30);
        
        // Ajouter un effet de texture/grain léger sur l'image
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.05f));
        Random random = new Random(12345);
        g.setColor(Color.BLACK);
        
        for (int i = 0; i < 5000; i++) {
            int x = centerX - width/2 + random.nextInt(width);
            int y = centerY - height/2 + random.nextInt(height);
            g.fillRect(x, y, 1, 1);
        }
        
        // Ajouter un cadre blanc autour de l'image
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3.0f));
        g.drawRoundRect(centerX - width/2, centerY - height/2, width, height, 20, 20);
    }
    
    /**
     * Dessine une silhouette de personne simple
     */
    private void drawSimplePerson(Graphics2D g, int x, int y, float size, Color color) {
        // Sauvegarder les attributs graphiques actuels
        Stroke originalStroke = g.getStroke();
        Composite originalComposite = g.getComposite();
        
        // Dessiner la tête
        int headSize = (int)(size * 0.3f);
        g.fillOval(x - headSize/2, y - headSize - (int)(size * 0.5f), headSize, headSize);
        
        // Dessiner le corps
        g.setStroke(new BasicStroke((float)(size * 0.15f)));
        g.drawLine(x, y - (int)(size * 0.5f), x, y);
        
        // Dessiner les jambes
        g.drawLine(x, y, x - (int)(size * 0.25f), y + (int)(size * 0.5f));
        g.drawLine(x, y, x + (int)(size * 0.25f), y + (int)(size * 0.5f));
        
        // Dessiner les bras
        g.drawLine(x, y - (int)(size * 0.3f), x - (int)(size * 0.3f), y - (int)(size * 0.2f));
        g.drawLine(x, y - (int)(size * 0.3f), x + (int)(size * 0.3f), y - (int)(size * 0.2f));
        
        // Ajouter un effet de brillance
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
        g.setColor(new Color(255, 255, 255, 120));
        g.fillOval(x - headSize/4, y - headSize - (int)(size * 0.5f) + headSize/4, headSize/4, headSize/4);
        
        // Restaurer les attributs graphiques
        g.setStroke(originalStroke);
        g.setComposite(originalComposite);
        g.setColor(color);
    }
    
    /**
     * Dessine des graphiques spécifiques au thème sportif
     */
    private void drawSportsGraphics(Graphics2D g, int centerX, int centerY, Color accentColor) {
        // Implémentation pour thème sportif
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));
        g.setColor(accentColor);
        
        // Dessiner une silhouette de coureur stylisée
        // TODO: Implémenter les détails graphiques sportifs
    }
    
    /**
     * Dessine des graphiques spécifiques au thème musical
     */
    private void drawMusicGraphics(Graphics2D g, int centerX, int centerY, Color accentColor) {
        // Implémentation pour thème musical
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));
        g.setColor(accentColor);
        
        // Dessiner des notes de musique stylisées
        // TODO: Implémenter les détails graphiques musicaux
    }
    
    /**
     * Dessine des graphiques spécifiques au thème conférence
     */
    private void drawConferenceGraphics(Graphics2D g, int centerX, int centerY, Color accentColor) {
        // Implémentation pour thème conférence
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));
        g.setColor(accentColor);
        
        // Dessiner un symbole de conférence (microphone, podium, etc.)
        // TODO: Implémenter les détails graphiques de conférence
    }
    
    /**
     * Dessine un thème générique avec formes géométriques modernes
     */
    private void drawModernGeometricTheme(Graphics2D g, int centerX, int centerY, Color accentColor, Random random) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
        
        // Dessiner plusieurs formes géométriques avec des rotations
        int numShapes = random.nextInt(3) + 5; // 5-7 formes
        
        for (int i = 0; i < numShapes; i++) {
            // Varier les couleurs
            g.setColor(new Color(
                accentColor.getRed(),
                accentColor.getGreen(),
                accentColor.getBlue(),
                50 + random.nextInt(150)
            ));
            
            // Créer une forme aléatoire
            int shapeType = random.nextInt(3);
            int size = 30 + random.nextInt(30);
            int xOffset = -50 + random.nextInt(100);
            int yOffset = -50 + random.nextInt(100);
            
            // Sauvegarder la transformation actuelle
            AffineTransform oldTransform = g.getTransform();
            
            // Appliquer une rotation
            g.rotate(Math.toRadians(random.nextInt(360)), centerX + xOffset, centerY + yOffset);
            
            // Dessiner la forme selon le type
            if (shapeType == 0) {
                // Rectangle
                g.fillRect(centerX + xOffset - size/2, centerY + yOffset - size/2, size, size);
            } else if (shapeType == 1) {
                // Cercle
                g.fillOval(centerX + xOffset - size/2, centerY + yOffset - size/2, size, size);
            } else {
                // Triangle
                int[] xPoints = {
                    centerX + xOffset,
                    centerX + xOffset - size/2,
                    centerX + xOffset + size/2
                };
                int[] yPoints = {
                    centerY + yOffset - size/2,
                    centerY + yOffset + size/2,
                    centerY + yOffset + size/2
                };
                g.fillPolygon(xPoints, yPoints, 3);
            }
            
            // Restaurer la transformation
            g.setTransform(oldTransform);
        }
    }
    
    /**
     * Ajoute des éléments marketing (appel à l'action, etc.)
     */
    private void drawMarketingElements(Graphics2D g, int width, int height, Color accentColor) {
        // Définir le texte
        String callToAction = "INSCRIPTION OBLIGATOIRE";
        String description = "Nous vous attendons nombreux !";
        String website = "Plus d'informations sur notre site web";
        
        // CTA principal - texte en gras
        g.setColor(Color.WHITE);
        Font ctaFont = new Font("Arial", Font.BOLD, 24);
        g.setFont(ctaFont);
        
        FontMetrics ctaMetrics = g.getFontMetrics();
        int ctaWidth = ctaMetrics.stringWidth(callToAction);
        int ctaX = width/2 - ctaWidth/2;
        int ctaY = height/2 + 100;
        
        // Zone de fond pour le CTA
        g.setColor(accentColor);
        g.fillRoundRect(ctaX - 15, ctaY - ctaMetrics.getAscent(), ctaWidth + 30, ctaMetrics.getHeight() + 10, 10, 10);
        
        // Effet de lueur sur le CTA
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
        g.setColor(Color.WHITE);
        g.fillRoundRect(ctaX - 10, ctaY - ctaMetrics.getAscent() + 2, ctaWidth + 20, (ctaMetrics.getHeight() + 10)/2, 10, 10);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        
        // Texte du CTA
        g.setColor(Color.WHITE);
        g.drawString(callToAction, ctaX, ctaY);
        
        // Description
        g.setFont(new Font("Arial", Font.ITALIC, 20));
        FontMetrics descMetrics = g.getFontMetrics();
        int descWidth = descMetrics.stringWidth(description);
        g.setColor(Color.WHITE);
        g.drawString(description, width/2 - descWidth/2, ctaY - 30);
        
        // Texte website
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        FontMetrics webMetrics = g.getFontMetrics();
        int webWidth = webMetrics.stringWidth(website);
        g.setColor(Color.WHITE);
        g.drawString(website, width/2 - webWidth/2, height - 80);
        
        // Note discrète en bas
        String note = "Affiche créée par SahaTeck";
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        FontMetrics noteMetrics = g.getFontMetrics();
        int noteWidth = noteMetrics.stringWidth(note);
        g.setColor(new Color(255, 255, 255, 120));
        g.drawString(note, width/2 - noteWidth/2, height - 30);
    }
    
    /**
     * Génère une image de secours basique mais professionnelle
     */
    private String generateFallbackImage(String title, String startDate, String endDate) throws IOException {
        // Nom de fichier unique
        String imageName = "fallback_" + UUID.randomUUID().toString().substring(0, 8) + ".png";
        Path outputPath = Paths.get(IMAGE_DIR + imageName);
        
        // Créer le répertoire s'il n'existe pas
        Files.createDirectories(Paths.get(IMAGE_DIR));
        
        // Créer une image simple mais professionnelle
        int width = 800;
        int height = 600;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        
        // Activer l'antialiasing
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Arrière-plan
        graphics.setColor(new Color(44, 62, 80)); // Bleu foncé
        graphics.fillRect(0, 0, width, height);
        
        // Bordure élégante
        graphics.setColor(new Color(230, 126, 34)); // Orange
        graphics.fillRect(0, 0, width, 8);
        graphics.fillRect(0, height - 8, width, 8);
        
        // Titre
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Arial", Font.BOLD, 36));
        drawWrappedText(graphics, title, graphics.getFont(), width/2, 150, width - 100, 0);
        
        // Dates
        graphics.setColor(new Color(236, 240, 241)); // Blanc cassé
        graphics.setFont(new Font("Arial", Font.PLAIN, 24));
        String dateText = "Du " + startDate + " au " + endDate;
        FontMetrics metrics = graphics.getFontMetrics();
        int textWidth = metrics.stringWidth(dateText);
        graphics.drawString(dateText, (width - textWidth) / 2, 250);
        
        // Ligne séparatrice
        graphics.setColor(new Color(230, 126, 34)); // Orange
        graphics.fillRect(width/4, 280, width/2, 3);
        
        // Information supplémentaire
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Arial", Font.ITALIC, 20));
        String infoText = "Événement à ne pas manquer !";
        textWidth = graphics.getFontMetrics().stringWidth(infoText);
        graphics.drawString(infoText, (width - textWidth) / 2, 350);
        
        // Note de génération
        graphics.setColor(new Color(189, 195, 199)); // Gris clair
        graphics.setFont(new Font("Arial", Font.PLAIN, 12));
        String noteText = "Affiche générée automatiquement par SahaTeck";
        textWidth = graphics.getFontMetrics().stringWidth(noteText);
        graphics.drawString(noteText, (width - textWidth) / 2, height - 20);
        
        graphics.dispose();
        
        // Enregistrer l'image
        ImageIO.write(image, "png", outputPath.toFile());
        
        System.out.println("Image de secours enregistrée à: " + outputPath);
        
        return imageName;
    }
    
    /**
     * Dessine du texte avec retour à la ligne automatique
     */
    private void drawWrappedText(Graphics2D g, String text, Font font, int x, int y, int maxWidth, int lineHeight) {
        if (lineHeight == 0) {
            lineHeight = font.getSize() + 10;
        }
        
        FontMetrics metrics = g.getFontMetrics(font);
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        int currentY = y;
        
        for (String word : words) {
            if (metrics.stringWidth(currentLine + " " + word) < maxWidth) {
                currentLine.append(word).append(" ");
            } else {
                String line = currentLine.toString().trim();
                int lineWidth = metrics.stringWidth(line);
                g.drawString(line, x - lineWidth/2, currentY);
                currentY += lineHeight;
                currentLine = new StringBuilder(word + " ");
            }
        }
        
        // Dernière ligne
        if (currentLine.length() > 0) {
            String line = currentLine.toString().trim();
            int lineWidth = metrics.stringWidth(line);
            g.drawString(line, x - lineWidth/2, currentY);
        }
    }
    
    /**
     * Vérifie si Llama est disponible dans le système
     * @return true si Llama est disponible, false sinon
     */
    public boolean isLlamaAvailable() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(OLLAMA_EXECUTABLE, "--version");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            // Lire la sortie
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            System.err.println("Llama n'est pas disponible: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Méthode de secours pour générer une image placeholder quand Llama n'est pas disponible
     * @param title Le titre de l'événement
     * @return Le chemin vers l'image placeholder générée
     * @throws IOException si une erreur survient lors de la génération
     */
    public String generatePlaceholderImage(String title) throws IOException {
        // Pour compatibilité avec le code existant
        return generateFallbackImage(title, "Date non spécifiée", "Date non spécifiée");
    }
}