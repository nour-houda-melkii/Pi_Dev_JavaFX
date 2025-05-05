package com.services;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentMethodCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.model.checkout.Session;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PaymentService {
    static final Logger LOGGER = Logger.getLogger(PaymentService.class.getName());

    // Replace with your actual Stripe API key
    private static final String STRIPE_API_KEY = "sk_test_51RIn0GPE8H6eXFgj4DYNEXI7TpYxbomvoAcuxBIKPnD6gzFSPhRnbNAohwTynRD5BoMZ4q6Q1b9SEB3aFhw7uUTy00EPjmpBPF";

    public PaymentService() {
        // Initialize Stripe with API key
        Stripe.apiKey = STRIPE_API_KEY;
    }

    /**
     * Creates a Stripe PaymentIntent for processing payment
     *
     * @param amount The payment amount (in dollars)
     * @return PaymentIntent object
     * @throws StripeException if there's an error with the Stripe API
     */
    public PaymentIntent createPaymentIntent(double amount) throws StripeException {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount((long) (amount * 100))  // Convert to cents
                .setCurrency("usd")
                .build();

        return PaymentIntent.create(params);
    }

    /**
     * Creates a card payment method
     *
     * @param cardNumber The card number
     * @param expMonth The expiration month
     * @param expYear The expiration year
     * @param cvc The card security code
     * @return PaymentMethod object
     * @throws StripeException if there's an error with the Stripe API
     */
    public PaymentMethod createPaymentMethod(String cardNumber, int expMonth, int expYear, String cvc) throws StripeException {
        PaymentMethodCreateParams.CardDetails cardDetails = PaymentMethodCreateParams.CardDetails.builder()
                .setNumber(cardNumber)
                .setExpMonth((long) expMonth)
                .setExpYear((long) expYear)
                .setCvc(cvc)
                .build();

        PaymentMethodCreateParams params = PaymentMethodCreateParams.builder()
                .setType(PaymentMethodCreateParams.Type.CARD)
                .setCard(cardDetails)
                .build();

        return PaymentMethod.create(params);
    }

    /**
     * Creates a Stripe Checkout Session for the order
     *
     * @param amount The payment amount (in dollars)
     * @param orderReference The order reference
     * @param customerName The customer's name
     * @param customerEmail The customer's email
     * @return URL to the Stripe Checkout page
     * @throws StripeException if there's an error with the Stripe API
     */
    public String createStripeCheckoutSession(double amount, String orderReference,
                                              String customerName, String customerEmail) throws StripeException {
        SessionCreateParams params = SessionCreateParams.builder()
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("usd")
                                                .setUnitAmount((long) (amount * 100))  // Convert to cents
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Order: " + orderReference)
                                                                .setDescription("Purchase from SahaTech")
                                                                .build()
                                                )
                                                .build()
                                )
                                .setQuantity(1L)
                                .build()
                )
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("https://sahatech.com/success?ref=" + orderReference)
                .setCancelUrl("https://sahatech.com/cancel?ref=" + orderReference)
                .setCustomerEmail(customerEmail)
                .build();

        Session session = Session.create(params);
        return session.getUrl();
    }
}