-- Remove redundant PSP code snapshot from PSP account configuration.
-- psp_account should reference psp_provider through psp_id.
ALTER TABLE psp_account DROP COLUMN psp_code;
