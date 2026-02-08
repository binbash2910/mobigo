ALTER TABLE ride DROP CONSTRAINT ride_statut_check;
ALTER TABLE ride ADD CONSTRAINT ride_statut_check CHECK (
  statut::text = ANY (ARRAY['OUVERT', 'COMPLET', 'ANNULE', 'EFFECTUE']::text[])
  );

ALTER TABLE booking DROP CONSTRAINT IF EXISTS booking_statut_check;
ALTER TABLE booking ADD CONSTRAINT booking_statut_check CHECK (
  statut::text = ANY (ARRAY['CONFIRME', 'ANNULE', 'EN_ATTENTE', 'REFUSE']::text[])
  );

ALTER TABLE payment DROP CONSTRAINT IF EXISTS payment_methode_check;
ALTER TABLE payment ADD CONSTRAINT payment_methode_check CHECK (
  methode::text = ANY (ARRAY['CARTE_BANCAIRE', 'VIREMENT', 'PAYPAL', 'ORANGE_MONEY',
  'MTN_MOBILE_MONEY']::text[])
  );

ALTER TABLE payment DROP CONSTRAINT IF EXISTS payment_statut_check;
ALTER TABLE payment ADD CONSTRAINT payment_statut_check CHECK (((statut)::text =
  ANY ((ARRAY['REUSSI'::character varying, 'ECHOUE'::character varying, 'EN_ATTENTE'::character varying])::text[])));

ALTER TABLE vehicle ADD COLUMN bagages VARCHAR(255) NULL;
