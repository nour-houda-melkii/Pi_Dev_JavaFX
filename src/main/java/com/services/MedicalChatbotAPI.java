package com.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import okhttp3.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class MedicalChatbotAPI {
    // API Hugging Face (gratuite avec compte)
    private static final String API_URL = "https://api-inference.huggingface.co/models/mistralai/Mistral-7B-Instruct-v0.3";
    // Token Hugging Face
    private static final String API_KEY = "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";
    private final OkHttpClient client;
    private final Gson gson;

    // Liste de salutations et formules de politesse autorisées
    private static final Set<String> ALLOWED_GREETINGS = new HashSet<>(Arrays.asList(
            "bonjour", "salut", "bonsoir", "merci", "au revoir", "à bientôt", "comment ça va",
            "comment allez-vous", "bonne journée", "bonne soirée"
    ));

    // Liste de mots-clés médicaux pour déterminer si une question est médicale
    private static final Set<String> MEDICAL_KEYWORDS = new HashSet<>(Arrays.asList(
            "santé", "médical", "médecine", "docteur", "maladie", "symptôme", "traitement", "hôpital",
            "clinique", "vaccin", "médicament", "douleur", "diagnostic", "thérapie", "chirurgie",
            "patient", "fièvre", "infection", "allergie", "antibiotique", "virus", "bactérie", "cancer",
            "diabète", "hypertension", "tension", "cardiaque", "coeur", "poumon", "respiratoire", "digestif",
            "neurologique", "peau", "dermatologique", "orthopédique", "pédiatrique", "gynécologique",
            "psychiatrique", "psychologique", "radiographie", "IRM", "scanner", "échographie", "ultrason",
            "examen", "prélèvement", "biopsie", "laboratoire", "rééducation", "kinésithérapie", "orthopédie",
            "surgery", "soins", "urgence", "médecine préventive", "vaccination", "diagnostic précoce",
            "médecine générale", "anesthésie", "transfusion", "prothèse", "orthèse", "réadaptation",
            "consultation", "urgence", "pathologie", "consultation", "cardiologie", "oncologie",
            "gastro-entérologie", "dermatose", "tuberculose", "immunité", "infectiologie", "psychothérapie",
            "médecine alternative", "acupuncture", "ostéopathie", "nutrition", "cardiopathie", "aromathérapie",
            "traitement de la douleur", "réhabilitation", "chirurgie esthétique", "chirurgie réparatrice",
            "médecine du travail", "allergologie", "santé mentale", "psychoanalyse", "stress", "trouble anxieux",
            "dépression", "trouble du sommeil", "burn-out", "trouble du comportement alimentaire", "bilan de santé"
    ));


    public MedicalChatbotAPI() {
        // Ajout d'un timeout pour éviter les blocages indefinis
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    /**
     * Envoie une question au chatbot médical et retourne une réponse de manière asynchrone
     */
    public CompletableFuture<String> sendQuestion(String question) {
        CompletableFuture<String> future = new CompletableFuture<>();

        if (question == null || question.trim().isEmpty()) {
            future.completeExceptionally(new IllegalArgumentException("La question ne peut pas être vide"));
            return future;
        }

        // Vérifier si c'est une salutation ou une question médicale
        String normalizedQuestion = question.toLowerCase().trim();

        // Traiter les salutations simples directement sans appeler l'API
        if (isGreeting(normalizedQuestion)) {
            future.complete(handleGreeting(normalizedQuestion));
            return future;
        }

        // Vérifier si la question est médicale
        if (!isMedicalQuestion(normalizedQuestion)) {
            future.complete("Je suis désolé, je ne peux répondre qu'à des questions médicales. " +
                    "Veuillez me poser une question en rapport avec la santé ou la médecine.");
            return future;
        }

        // C'est une question médicale, préparer l'instruction pour obtenir une réponse simple
        String instruction = "Réponds à cette question médicale de manière très simple, avec un langage accessible à tous, " +
                "sans termes techniques complexes. Limite ta réponse à 3-4 phrases maximum: " + question;

        // Préparer le corps de la requête au format attendu par Hugging Face
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("inputs", "<s>[INST] " + instruction + " [/INST]");
        requestBody.addProperty("wait_for_model", true);

        // Construire la requête
        Request request = new Request.Builder()
                .url(API_URL)
                .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .build();

        // Exécuter la requête de façon asynchrone
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String responseText = responseBody != null ? responseBody.string() : "";

                    if (!response.isSuccessful()) {
                        future.completeExceptionally(new IOException("Erreur " + response.code() + ": " + responseText));
                        return;
                    }

                    try {
                        // Traitement de la réponse
                        String answer = extractAnswerFromResponse(responseText);
                        future.complete(simplifyResponse(answer));
                    } catch (JsonParseException e) {
                        future.completeExceptionally(new Exception("Erreur de parsing JSON: " + e.getMessage()));
                    } catch (Exception e) {
                        future.completeExceptionally(new Exception("Erreur lors du traitement de la réponse: " + e.getMessage()));
                    }
                }
            }
        });

        return future;
    }

    /**
     * Détermine si le texte est une salutation ou une formule de politesse
     */
    private boolean isGreeting(String text) {
        String normalized = text.toLowerCase().trim().replaceAll("[!?.,]", "");
        for (String greeting : ALLOWED_GREETINGS) {
            if (normalized.contains(greeting)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Répond aux salutations de manière appropriée
     */
    private String handleGreeting(String greeting) {
        String normalized = greeting.toLowerCase().trim();

        if (normalized.contains("bonjour") || normalized.contains("salut")) {
            return "Bonjour ! Je suis votre assistant médical. Comment puis-je vous aider aujourd'hui ?";
        }
        if (normalized.contains("bonsoir")) {
            return "Bonsoir ! Je suis votre assistant médical. Comment puis-je vous aider ce soir ?";
        }
        if (normalized.contains("merci")) {
            return "Je vous en prie ! N'hésitez pas si vous avez d'autres questions médicales.";
        }
        if (normalized.contains("au revoir") || normalized.contains("à bientôt")) {
            return "Au revoir ! Prenez soin de votre santé et à bientôt !";
        }
        if (normalized.contains("comment ça va") || normalized.contains("comment allez-vous")) {
            return "Je vais bien, merci ! Je suis prêt à répondre à vos questions médicales.";
        }
        if (normalized.contains("bonne journée")) {
            return "Bonne journée à vous aussi ! Restez en bonne santé !";
        }
        if (normalized.contains("bonne soirée")) {
            return "Bonne soirée à vous aussi ! Prenez soin de vous !";
        }

        // Réponse par défaut pour d'autres salutations
        return "Bonjour ! Je suis votre assistant médical, comment puis-je vous aider ?";
    }

    /**
     * Détermine si la question est médicale
     */
    private boolean isMedicalQuestion(String question) {
        String normalized = question.toLowerCase();

        // Vérifier les mots-clés médicaux
        for (String keyword : MEDICAL_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return true;
            }
        }

        // Vérifier les patterns courants de questions médicales
        String[] medicalPatterns = {
                "j'ai mal", "je souffre", "symptômes", "que faire si", "est-ce normal",
                "est-ce grave", "traitement pour", "remède", "guérir", "soigner", "soulager",
                "consultation", "ordonnance", "médicament pour", "vaccin contre", "examen",
                "analyse", "bilan de santé", "douleur au", "fièvre", "mal de tête", "fatigue",
                "nausée", "vomissement", "vertige", "essoufflement", "palpitations", "douleur thoracique",
                "douleur abdominale", "gonflement", "plaie", "infection cutanée", "écoulement",
                "démangeaison", "érythème", "brûlure", "pouls irrégulier", "perte de conscience",
                "sudation excessive", "problème de vue", "perte de poids", "prise de poids", "hypoglycémie",
                "hyperglycémie", "allergie au", "réaction allergique", "test de dépistage", "prise de sang",
                "radio", "échographie", "IRM", "scanner", "analyse d'urine", "examen clinique",
                "avis médical", "conseil de médecin", "consultation spécialisée", "chirurgie",
                "opération", "intervention", "suivi post-opératoire", "traitement médicamenteux",
                "antibiotique", "antidouleur", "anti-inflammatoire", "remède naturel", "homéopathie",
                "rééducation", "kinésithérapie", "orthophonie", "séance de soins", "plan de traitement",
                "prévention des maladies", "vaccination obligatoire", "programme de santé", "santé publique"
        };


        for (String pattern : medicalPatterns) {
            if (normalized.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Extrait la réponse du format JSON retourné par Hugging Face
     */
    private String extractAnswerFromResponse(String responseText) {
        if (responseText == null || responseText.trim().isEmpty()) {
            return "Aucune réponse reçue";
        }

        try {
            if (responseText.startsWith("[")) {
                // Format de réponse en tableau
                JsonArray jsonArray = gson.fromJson(responseText, JsonArray.class);
                if (jsonArray.size() > 0) {
                    JsonObject firstResult = jsonArray.get(0).getAsJsonObject();
                    if (firstResult.has("generated_text")) {
                        String answer = firstResult.get("generated_text").getAsString();
                        return cleanAnswer(answer);
                    }
                }
            } else {
                // Format de réponse en objet simple
                JsonObject jsonResponse = gson.fromJson(responseText, JsonObject.class);
                if (jsonResponse.has("generated_text")) {
                    String answer = jsonResponse.get("generated_text").getAsString();
                    return cleanAnswer(answer);
                }
            }

            return "Je ne peux pas répondre à cette question pour le moment.";
        } catch (JsonParseException e) {
            return "Je suis désolé, je rencontre des difficultés techniques.";
        }
    }

    /**
     * Nettoie la réponse des tags d'instruction et autres artefacts
     */
    private String cleanAnswer(String answer) {
        if (answer == null) return "";

        // Enlever les balises d'instruction et texte d'entrée si présents
        if (answer.contains("[/INST]")) {
            answer = answer.substring(answer.indexOf("[/INST]") + 7).trim();
        }

        // Enlever les tags de fin de séquence
        answer = answer.replace("</s>", "").trim();

        return answer;
    }

    /**
     * Simplifie davantage la réponse pour la rendre plus accessible
     */
    private String simplifyResponse(String response) {
        // Limite la longueur de la réponse
        if (response.length() > 500) {
            // Chercher la dernière phrase complète avant 500 caractères
            int lastSentence = Math.max(
                    response.lastIndexOf(". ", 500),
                    Math.max(response.lastIndexOf("! ", 500),
                            response.lastIndexOf("? ", 500))
            );

            if (lastSentence > 0) {
                response = response.substring(0, lastSentence + 1);
            } else {
                response = response.substring(0, Math.min(500, response.length()));
            }

            response += " [...]";
        }

        // Remplacer les termes médicaux complexes par des termes plus simples
        response = response.replaceAll("hypertension artérielle", "tension élevée")
                .replaceAll("myocarde", "muscle cardiaque")
                .replaceAll("pathologie", "maladie")
                .replaceAll("ischémie", "manque d'oxygène")
                .replaceAll("œdème", "gonflement")
                .replaceAll("hypoglycémie", "baisse de sucre dans le sang")
                .replaceAll("hyperglycémie", "excès de sucre dans le sang");

        return response;
    }
}