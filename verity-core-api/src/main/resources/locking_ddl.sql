-- VerityNow Locking + Txn DDL (PostgreSQL)


CREATE TABLE IF NOT EXISTS vn_lock_group (
  lock_group_id UUID PRIMARY KEY,
  owner_id      TEXT NOT NULL,
  fence_token   BIGINT NOT NULL,
  active        BOOLEAN NOT NULL DEFAULT TRUE,
  acquired_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  released_at   TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS ix_vn_lock_group_owner_active
  ON vn_lock_group(owner_id, active);

CREATE TABLE IF NOT EXISTS vn_path_lock (
  id            BIGSERIAL PRIMARY KEY,
  lock_group_id UUID NOT NULL REFERENCES vn_lock_group(lock_group_id),
  owner_id      TEXT NOT NULL,
  scope_key     LTREE NOT NULL,
  active        BOOLEAN NOT NULL DEFAULT TRUE,
  acquired_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  released_at   TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS ix_vn_path_lock_scope_key_gist
  ON vn_path_lock USING GIST(scope_key);

CREATE INDEX IF NOT EXISTS ix_vn_path_lock_owner_active
  ON vn_path_lock(owner_id, active);

CREATE INDEX IF NOT EXISTS ix_vn_path_lock_group
  ON vn_path_lock(lock_group_id);

CREATE TABLE IF NOT EXISTS vn_txn_epoch (
  txn_id        TEXT PRIMARY KEY,
  lock_group_id UUID,
  fence_token   BIGINT,
  status        TEXT NOT NULL,
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_vn_txn_epoch_status
  ON vn_txn_epoch(status);
  
BEGIN;
ALTER TABLE vn_txn_epoch DROP CONSTRAINT IF EXISTS fk_txn_epoch_lock_group;
ALTER TABLE vn_txn_epoch DROP CONSTRAINT IF EXISTS ck_txn_epoch_status;
  
ALTER TABLE vn_txn_epoch
  ADD CONSTRAINT fk_txn_epoch_lock_group
  FOREIGN KEY (lock_group_id) REFERENCES vn_lock_group(lock_group_id);

ALTER TABLE vn_txn_epoch
  ADD CONSTRAINT ck_txn_epoch_status
  CHECK (status IN ('IN_FLIGHT','COMMITTED','ROLLED_BACK','FAILED'));
COMMIT;


