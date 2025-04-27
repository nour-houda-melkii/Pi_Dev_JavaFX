-- Mise à jour de la table notification_history pour permettre NULL dans la colonne sent_date
ALTER TABLE notification_history MODIFY COLUMN sent_date TIMESTAMP NULL;
 
-- Mise à jour des indexes
CREATE INDEX IF NOT EXISTS idx_notification_pending ON notification_history (user_id, event_id, notification_type, sent_date)
WHERE sent_date IS NULL; 