-- OCPP 1.6 §7.8: parentIdTag enables group authorization.
ALTER TABLE authorizations ADD COLUMN parent_id_tag VARCHAR(20);
