package service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.json.JSONArray;

import util.ConfigManager;

/**
 * Service pour accéder aux données météorologiques via une API externe
 */
public class WeatherService {
    
    private final String apiKey;
    private final String apiUrl;
    private final Map<String, Map<String, Object>> weatherCache;
    
    /**
     * Constructeur qui initialise le service météo avec les paramètres de configuration
     */
    public WeatherService() {
        ConfigManager configManager = ConfigManager.getInstance();
        this.apiKey = configManager.getProperty("weather.api.key");
        this.apiUrl = configManager.getProperty("weather.api.url", "https://api.openweathermap.org/data/2.5/forecast");
        this.weatherCache = new HashMap<>();
    }
    
    /**
     * Récupère les prévisions météo pour une date et une localisation données
     * 
     * @param location Nom de la ville ou coordonnées
     * @param date Date pour laquelle on souhaite les prévisions
     * @return Map contenant les informations météo (température, conditions, etc.)
     * @throws IOException En cas d'erreur de communication avec l'API
     */
    public Map<String, Object> getWeatherForecast(String location, LocalDate date) throws IOException {
        // Vérifier si les données sont dans le cache
        String cacheKey = location + "_" + date.format(DateTimeFormatter.ISO_DATE);
        if (weatherCache.containsKey(cacheKey)) {
            return weatherCache.get(cacheKey);
        }
        
        // Préparer l'URL de l'API avec les paramètres
        String urlStr = apiUrl + "?q=" + location + "&appid=" + apiKey + "&units=metric";
        URL url = new URL(urlStr);
        
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        
        // Lire la réponse
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } finally {
            connection.disconnect();
        }
        
        // Analyser JSON et extraire les prévisions pour la date spécifiée
        Map<String, Object> forecast = parseWeatherData(response.toString(), date);
        
        // Mettre en cache
        weatherCache.put(cacheKey, forecast);
        
        return forecast;
    }
    
    /**
     * Analyse les données météo brutes et extrait les informations pour une date spécifique
     * 
     * @param jsonData Données JSON de l'API météo
     * @param targetDate Date pour laquelle on souhaite les prévisions
     * @return Map contenant les informations météo pour la date spécifiée
     */
    private Map<String, Object> parseWeatherData(String jsonData, LocalDate targetDate) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            JSONObject data = new JSONObject(jsonData);
            JSONArray forecasts = data.getJSONArray("list");
            
            String targetDateStr = targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            // Par défaut, utiliser la première prévision disponible (probablement la plus proche)
            JSONObject bestMatch = null;
            
            // Parcourir toutes les prévisions pour trouver celle qui correspond à la date cible
            for (int i = 0; i < forecasts.length(); i++) {
                JSONObject forecast = forecasts.getJSONObject(i);
                String forecastDate = forecast.getString("dt_txt").split(" ")[0];
                
                if (forecastDate.equals(targetDateStr)) {
                    // Si c'est la première correspondance ou si c'est pour midi (meilleure représentation)
                    if (bestMatch == null || forecast.getString("dt_txt").contains("12:00")) {
                        bestMatch = forecast;
                    }
                }
            }
            
            // Si aucune prévision n'est trouvée pour cette date (trop loin dans le futur)
            if (bestMatch == null) {
                result.put("available", false);
                result.put("message", "Prévisions non disponibles pour cette date");
                return result;
            }
            
            // Extraire les informations pertinentes
            JSONObject main = bestMatch.getJSONObject("main");
            JSONArray weatherArray = bestMatch.getJSONArray("weather");
            JSONObject weather = weatherArray.getJSONObject(0);
            JSONObject wind = bestMatch.getJSONObject("wind");
            
            result.put("available", true);
            result.put("temperature", main.getDouble("temp"));
            result.put("feels_like", main.getDouble("feels_like"));
            result.put("humidity", main.getInt("humidity"));
            result.put("pressure", main.getInt("pressure"));
            result.put("weather_main", weather.getString("main"));
            result.put("weather_description", weather.getString("description"));
            result.put("weather_icon", weather.getString("icon"));
            result.put("wind_speed", wind.getDouble("speed"));
            result.put("wind_direction", wind.has("deg") ? wind.getDouble("deg") : 0);
            result.put("clouds", bestMatch.getJSONObject("clouds").getInt("all"));
            result.put("time", bestMatch.getString("dt_txt"));
            
            // Vérifier s'il y a des précipitations
            if (bestMatch.has("rain")) {
                result.put("rain", bestMatch.getJSONObject("rain").optDouble("3h", 0));
            } else {
                result.put("rain", 0);
            }
            
            if (bestMatch.has("snow")) {
                result.put("snow", bestMatch.getJSONObject("snow").optDouble("3h", 0));
            } else {
                result.put("snow", 0);
            }
            
        } catch (Exception e) {
            result.put("available", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Détermine si les conditions météo sont favorables pour un événement extérieur
     * 
     * @param forecast Prévisions météo obtenues via getWeatherForecast
     * @return true si les conditions sont favorables
     */
    public boolean isWeatherSuitableForOutdoorEvent(Map<String, Object> forecast) {
        if (!(boolean) forecast.getOrDefault("available", false)) {
            // Si les prévisions ne sont pas disponibles, nous supposons que c'est favorable
            return true;
        }
        
        double temperature = (double) forecast.get("temperature");
        double windSpeed = (double) forecast.get("wind_speed");
        double rainAmount = (double) forecast.get("rain");
        String weatherMain = (String) forecast.get("weather_main");
        
        // Conditions défavorables
        boolean isBadWeather = 
            temperature < 10 ||  // Trop froid
            temperature > 35 ||  // Trop chaud
            windSpeed > 50 ||    // Vent trop fort
            rainAmount > 5 ||    // Fortes pluies
            weatherMain.equalsIgnoreCase("Thunderstorm") || // Orage
            weatherMain.equalsIgnoreCase("Snow");           // Neige
            
        return !isBadWeather;
    }
    
    /**
     * Récupère une recommandation textuelle basée sur les prévisions météo
     * 
     * @param forecast Prévisions météo obtenues via getWeatherForecast
     * @return Message de recommandation adapté aux conditions météo
     */
    public String getWeatherRecommendation(Map<String, Object> forecast) {
        if (!(boolean) forecast.getOrDefault("available", false)) {
            return "Prévisions météo non disponibles pour cette date.";
        }
        
        double temperature = (double) forecast.get("temperature");
        double windSpeed = (double) forecast.get("wind_speed");
        double rainAmount = (double) forecast.get("rain");
        double snowAmount = (double) forecast.get("snow");
        String weatherMain = (String) forecast.get("weather_main");
        
        StringBuilder recommendation = new StringBuilder();
        
        // Analyses des conditions et recommandations
        if (temperature < 5) {
            recommendation.append("Températures très froides. Prévoyez un chauffage adéquat pour votre événement. ");
        } else if (temperature < 15) {
            recommendation.append("Températures fraîches. Un chauffage d'appoint pourrait être nécessaire. ");
        } else if (temperature > 30) {
            recommendation.append("Températures élevées. Prévoyez une climatisation ou des zones ombragées. ");
        } else {
            recommendation.append("Températures agréables. ");
        }
        
        if (weatherMain.equalsIgnoreCase("Rain") || rainAmount > 0) {
            recommendation.append("Pluie prévue. Envisagez un lieu couvert ou des tentes imperméables. ");
        }
        
        if (weatherMain.equalsIgnoreCase("Snow") || snowAmount > 0) {
            recommendation.append("Neige prévue. Vérifiez l'accessibilité du lieu et prévoyez un chauffage adéquat. ");
        }
        
        if (weatherMain.equalsIgnoreCase("Thunderstorm")) {
            recommendation.append("Orages prévus. Événement en intérieur fortement recommandé. ");
        }
        
        if (windSpeed > 30) {
            recommendation.append("Vents forts. Sécurisez les structures temporaires et évitez les zones exposées. ");
        }
        
        if (isWeatherSuitableForOutdoorEvent(forecast)) {
            recommendation.append("Dans l'ensemble, les conditions semblent favorables pour un événement extérieur, mais restez vigilant aux changements météorologiques.");
        } else {
            recommendation.append("Ces conditions météorologiques suggèrent qu'un événement en intérieur serait plus approprié.");
        }
        
        return recommendation.toString();
    }
    
    /**
     * Retourne l'URL de l'icône météo pour affichage dans l'interface
     * 
     * @param iconCode Code de l'icône obtenu via les prévisions
     * @return URL de l'image de l'icône
     */
    public String getWeatherIconUrl(String iconCode) {
        return "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";
    }
} 