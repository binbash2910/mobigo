-- =====================================================================
-- Campay: colonnes nécessaires sur la table `payment`.
--
-- Ce script est INFORMATIF : Spring Boot est configuré avec
-- `spring.jpa.hibernate.ddl-auto=update` (voir application.yml), donc
-- les colonnes sont créées automatiquement au démarrage.
--
-- Lance ce script UNIQUEMENT si tu déploies sans ddl-auto=update.
-- Il est idempotent (IF NOT EXISTS).
-- =====================================================================

ALTER TABLE payment ADD COLUMN IF NOT EXISTS external_reference       VARCHAR(64);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS campay_transaction_id    VARCHAR(64);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS operateur                VARCHAR(16);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS phone_number             VARCHAR(32);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS disbursement_reference   VARCHAR(64);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS disbursement_status      VARCHAR(32);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS commission_plateforme    REAL;
ALTER TABLE payment ADD COLUMN IF NOT EXISTS frais_campay             REAL;
ALTER TABLE payment ADD COLUMN IF NOT EXISTS revenu_net_plateforme    REAL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_payment_external_reference      ON payment(external_reference);
CREATE UNIQUE INDEX IF NOT EXISTS idx_payment_campay_transaction_id   ON payment(campay_transaction_id);
CREATE INDEX        IF NOT EXISTS idx_payment_booking_id              ON payment(booking_id);
