package utils;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import java.util.HashMap;
import java.util.Map;

/**
 * Classe utilitaire pour gérer les paiements via la passerelle Stripe
 * Utilise l'API Stripe officielle pour créer des sessions de paiement
 */
public class StripePaymentGateway {
    // Clés API (remplacer par vos clés Stripe réelles)
    private static final String API_KEY = "";
    private static final String PUBLIC_KEY = "";
    
    // URL de redirection après paiement
    private static final String SUCCESS_URL = "http://checkout.stripe.success";
    private static final String CANCEL_URL = "http://checkout.stripe.cancel";
    
    static {
        // Initialiser Stripe avec la clé API
        Stripe.apiKey = API_KEY;
    }
    
    /**
     * Crée une session de paiement Stripe et retourne l'URL de la page de paiement
     * 
     * @param amount Montant du paiement en centimes (100 = 1€)
     * @param currency Code de la devise (EUR, USD, etc.)
     * @param description Description du paiement
     * @param donorName Nom du donateur (ou "Anonyme")
     * @return URL de la page de paiement Stripe
     * @throws StripeException Si une erreur survient lors de la création de la session
     */
    public static String createPaymentSession(int amount, String currency, String description, String donorName) throws StripeException {
        // Configurer les paramètres de la session
        SessionCreateParams params = SessionCreateParams.builder()
            .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .setSuccessUrl(SUCCESS_URL)
            .setCancelUrl(CANCEL_URL)
            .addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(currency.toLowerCase())
                            .setUnitAmount((long)amount)
                            .setProductData(
                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName("Don" + (donorName != null && !donorName.equals("Anonyme") ? " de " + donorName : " anonyme"))
                                    .setDescription(description)
                                    .build())
                            .build())
                    .build())
            .setCustomerEmail(donorName.equals("Anonyme") ? null : donorName.contains("@") ? donorName : null)
            .putMetadata("donor_name", donorName)
            .putMetadata("description", description)
            .build();
        
        // Créer la session de paiement
        Session session = Session.create(params);
        
        // Retourner l'URL de la page de paiement
        return session.getUrl();
    }
    
    /**
     * Récupère le statut d'un paiement
     * 
     * @param sessionId ID de la session Stripe
     * @return Statut du paiement (success, pending, failed)
     */
    public static String getPaymentStatus(String sessionId) throws StripeException {
        Session session = Session.retrieve(sessionId);
        String paymentStatus = session.getPaymentStatus();
        
        switch (paymentStatus) {
            case "paid":
                return "success";
            case "unpaid":
                return "pending";
            default:
                return "failed";
        }
    }
    
    /**
     * Retourne la clé publique Stripe pour l'intégration frontend
     * 
     * @return Clé publique Stripe
     */
    public static String getPublicKey() {
        return PUBLIC_KEY;
    }
} 