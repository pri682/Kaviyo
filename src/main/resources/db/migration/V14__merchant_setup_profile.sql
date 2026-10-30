ALTER TABLE merchant_setups ADD COLUMN business_type TEXT;
ALTER TABLE merchant_setups ADD COLUMN updated_at TIMESTAMPTZ;
UPDATE merchant_setups SET updated_at = created_at WHERE updated_at IS NULL;
