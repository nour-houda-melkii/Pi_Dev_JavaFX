-- Add price column to commande_ligne table
ALTER TABLE commande_ligne ADD COLUMN price DOUBLE;

-- Update existing records to use the product's original price
UPDATE commande_ligne cl
JOIN produit p ON cl.produit_id = p.id
SET cl.price = p.price; 