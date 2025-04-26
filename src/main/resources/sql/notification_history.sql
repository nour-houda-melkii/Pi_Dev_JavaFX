-- Création de la table pour l'historique des notifications
CREATE TABLE IF NOT EXISTS notification_history (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    event_id INT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    details TEXT,
    sent_date TIMESTAMP NULL,
    is_read BOOLEAN DEFAULT FALSE,
    read_date TIMESTAMP NULL,
    
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE CASCADE,
    
    INDEX idx_user_id (user_id),
    INDEX idx_event_id (event_id),
    INDEX idx_sent_date (sent_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Types de notifications possibles
-- REMINDER: Rappel 24h avant un événement
-- EVENT_CHANGE: Modification d'un événement
-- REGISTRATION: Confirmation d'inscription
-- CANCELLATION: Annulation d'un événement 