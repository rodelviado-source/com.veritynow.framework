--liquibase formatted sql

--changeset verity:001-init-schema failOnError:true runOnChange:false
--comment: Baseline schema captured via JDBC from Hibernate auto-DDL at 2026-01-21T12:29:38.799297800Z

SET search_path = "public";

CREATE EXTENSION IF NOT EXISTS ltree;
CREATE TABLE IF NOT EXISTS "public"."vn_dir_entry" (
  "child_id" bigint NOT NULL,
  "created_at" timestamp with time zone NOT NULL,
  "id" bigint NOT NULL,
  "parent_id" bigint NOT NULL,
  "name" varchar(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS "public"."vn_inode" (
  "created_at" timestamp with time zone NOT NULL,
  "id" bigint NOT NULL,
  "scope_key" "ltree"
);

CREATE TABLE IF NOT EXISTS "public"."vn_inode_path_segment" (
  "ord" integer NOT NULL,
  "created_at" timestamp with time zone NOT NULL,
  "dir_entry_id" bigint NOT NULL,
  "id" bigint NOT NULL,
  "inode_id" bigint NOT NULL
);

CREATE TABLE IF NOT EXISTS "public"."vn_lock_group" (
  "lock_group_id" uuid NOT NULL,
  "owner_id" text NOT NULL,
  "fence_token" bigint,
  "active" boolean DEFAULT true NOT NULL,
  "acquired_at" timestamp with time zone DEFAULT now() NOT NULL,
  "expires_at" timestamp with time zone,
  "released_at" timestamp with time zone
);

CREATE TABLE IF NOT EXISTS "public"."vn_node_head" (
  "fence_token" bigint,
  "inode_id" bigint NOT NULL,
  "updated_at" timestamp with time zone NOT NULL,
  "version_id" bigint NOT NULL
);

CREATE TABLE IF NOT EXISTS "public"."vn_node_version" (
  "id" bigint NOT NULL,
  "inode_id" bigint NOT NULL,
  "size" bigint NOT NULL,
  "timestamp" bigint NOT NULL,
  "context_name" varchar(255),
  "correlation_id" varchar(255),
  "hash" varchar(255),
  "mime_type" varchar(255),
  "name" varchar(255),
  "operation" varchar(255),
  "path" varchar(255),
  "principal" varchar(255),
  "transaction_id" varchar(255),
  "transaction_result" varchar(255),
  "workflow_id" varchar(255)
);

CREATE TABLE IF NOT EXISTS "public"."vn_path_lock" (
  "id" bigint DEFAULT nextval('vn_path_lock_id_seq'::regclass) NOT NULL,
  "lock_group_id" uuid NOT NULL,
  "owner_id" text NOT NULL,
  "scope_key" "ltree" NOT NULL,
  "active" boolean DEFAULT true NOT NULL,
  "acquired_at" timestamp with time zone DEFAULT now() NOT NULL,
  "released_at" timestamp with time zone
);

CREATE TABLE IF NOT EXISTS "public"."vn_txn_epoch" (
  "txn_id" text NOT NULL,
  "lock_group_id" uuid,
  "fence_token" bigint,
  "status" text NOT NULL,
  "updated_at" timestamp with time zone DEFAULT now() NOT NULL
);

ALTER TABLE IF EXISTS "public"."vn_dir_entry" ADD CONSTRAINT "vn_dir_entry_pkey" PRIMARY KEY (id);
ALTER TABLE IF EXISTS "public"."vn_dir_entry" ADD CONSTRAINT "uq_dir_parent_name" UNIQUE (parent_id, name);
ALTER TABLE IF EXISTS "public"."vn_dir_entry" ADD CONSTRAINT "fkphtn4icdtoh8xgjuvtw4qcd0i" FOREIGN KEY (child_id) REFERENCES vn_inode(id);
ALTER TABLE IF EXISTS "public"."vn_dir_entry" ADD CONSTRAINT "fkryi5xny4gg8sp06uef12ieeq5" FOREIGN KEY (parent_id) REFERENCES vn_inode(id);
ALTER TABLE IF EXISTS "public"."vn_dir_entry" ADD CONSTRAINT "vn_dir_entry_child_id_not_null" NOT NULL child_id;
ALTER TABLE IF EXISTS "public"."vn_dir_entry" ADD CONSTRAINT "vn_dir_entry_created_at_not_null" NOT NULL created_at;
ALTER TABLE IF EXISTS "public"."vn_dir_entry" ADD CONSTRAINT "vn_dir_entry_id_not_null" NOT NULL id;
ALTER TABLE IF EXISTS "public"."vn_dir_entry" ADD CONSTRAINT "vn_dir_entry_name_not_null" NOT NULL name;
ALTER TABLE IF EXISTS "public"."vn_dir_entry" ADD CONSTRAINT "vn_dir_entry_parent_id_not_null" NOT NULL parent_id;
ALTER TABLE IF EXISTS "public"."vn_inode" ADD CONSTRAINT "vn_inode_pkey" PRIMARY KEY (id);
ALTER TABLE IF EXISTS "public"."vn_inode" ADD CONSTRAINT "vn_inode_created_at_not_null" NOT NULL created_at;
ALTER TABLE IF EXISTS "public"."vn_inode" ADD CONSTRAINT "vn_inode_id_not_null" NOT NULL id;
ALTER TABLE IF EXISTS "public"."vn_inode_path_segment" ADD CONSTRAINT "vn_inode_path_segment_pkey" PRIMARY KEY (id);
ALTER TABLE IF EXISTS "public"."vn_inode_path_segment" ADD CONSTRAINT "uq_inode_ord" UNIQUE (inode_id, ord);
ALTER TABLE IF EXISTS "public"."vn_inode_path_segment" ADD CONSTRAINT "fka7ehnitijgi0le9m875fxw42v" FOREIGN KEY (dir_entry_id) REFERENCES vn_dir_entry(id);
ALTER TABLE IF EXISTS "public"."vn_inode_path_segment" ADD CONSTRAINT "fkg9to0spyux7e4cyp7fpxq27dj" FOREIGN KEY (inode_id) REFERENCES vn_inode(id);
ALTER TABLE IF EXISTS "public"."vn_inode_path_segment" ADD CONSTRAINT "vn_inode_path_segment_created_at_not_null" NOT NULL created_at;
ALTER TABLE IF EXISTS "public"."vn_inode_path_segment" ADD CONSTRAINT "vn_inode_path_segment_dir_entry_id_not_null" NOT NULL dir_entry_id;
ALTER TABLE IF EXISTS "public"."vn_inode_path_segment" ADD CONSTRAINT "vn_inode_path_segment_id_not_null" NOT NULL id;
ALTER TABLE IF EXISTS "public"."vn_inode_path_segment" ADD CONSTRAINT "vn_inode_path_segment_inode_id_not_null" NOT NULL inode_id;
ALTER TABLE IF EXISTS "public"."vn_inode_path_segment" ADD CONSTRAINT "vn_inode_path_segment_ord_not_null" NOT NULL ord;
ALTER TABLE IF EXISTS "public"."vn_lock_group" ADD CONSTRAINT "vn_lock_group_pkey" PRIMARY KEY (lock_group_id);
ALTER TABLE IF EXISTS "public"."vn_lock_group" ADD CONSTRAINT "vn_lock_group_acquired_at_not_null" NOT NULL acquired_at;
ALTER TABLE IF EXISTS "public"."vn_lock_group" ADD CONSTRAINT "vn_lock_group_active_not_null" NOT NULL active;
ALTER TABLE IF EXISTS "public"."vn_lock_group" ADD CONSTRAINT "vn_lock_group_lock_group_id_not_null" NOT NULL lock_group_id;
ALTER TABLE IF EXISTS "public"."vn_lock_group" ADD CONSTRAINT "vn_lock_group_owner_id_not_null" NOT NULL owner_id;
ALTER TABLE IF EXISTS "public"."vn_node_head" ADD CONSTRAINT "vn_node_head_pkey" PRIMARY KEY (inode_id);
ALTER TABLE IF EXISTS "public"."vn_node_head" ADD CONSTRAINT "fkaono5m08t5x8txq0vg3ca3wdh" FOREIGN KEY (inode_id) REFERENCES vn_inode(id);
ALTER TABLE IF EXISTS "public"."vn_node_head" ADD CONSTRAINT "fkod48yv13fhdrs5rxjxmj8u1od" FOREIGN KEY (version_id) REFERENCES vn_node_version(id);
ALTER TABLE IF EXISTS "public"."vn_node_head" ADD CONSTRAINT "vn_node_head_inode_id_not_null" NOT NULL inode_id;
ALTER TABLE IF EXISTS "public"."vn_node_head" ADD CONSTRAINT "vn_node_head_updated_at_not_null" NOT NULL updated_at;
ALTER TABLE IF EXISTS "public"."vn_node_head" ADD CONSTRAINT "vn_node_head_version_id_not_null" NOT NULL version_id;
ALTER TABLE IF EXISTS "public"."vn_node_version" ADD CONSTRAINT "vn_node_version_pkey" PRIMARY KEY (id);
ALTER TABLE IF EXISTS "public"."vn_node_version" ADD CONSTRAINT "fkgsea7tlnqrybidbrinb952lw0" FOREIGN KEY (inode_id) REFERENCES vn_inode(id);
ALTER TABLE IF EXISTS "public"."vn_node_version" ADD CONSTRAINT "vn_node_version_id_not_null" NOT NULL id;
ALTER TABLE IF EXISTS "public"."vn_node_version" ADD CONSTRAINT "vn_node_version_inode_id_not_null" NOT NULL inode_id;
ALTER TABLE IF EXISTS "public"."vn_node_version" ADD CONSTRAINT "vn_node_version_size_not_null" NOT NULL size;
ALTER TABLE IF EXISTS "public"."vn_node_version" ADD CONSTRAINT "vn_node_version_timestamp_not_null" NOT NULL "timestamp";
ALTER TABLE IF EXISTS "public"."vn_path_lock" ADD CONSTRAINT "vn_path_lock_pkey" PRIMARY KEY (id);
ALTER TABLE IF EXISTS "public"."vn_path_lock" ADD CONSTRAINT "vn_path_lock_lock_group_id_fkey" FOREIGN KEY (lock_group_id) REFERENCES vn_lock_group(lock_group_id);
ALTER TABLE IF EXISTS "public"."vn_path_lock" ADD CONSTRAINT "vn_path_lock_acquired_at_not_null" NOT NULL acquired_at;
ALTER TABLE IF EXISTS "public"."vn_path_lock" ADD CONSTRAINT "vn_path_lock_active_not_null" NOT NULL active;
ALTER TABLE IF EXISTS "public"."vn_path_lock" ADD CONSTRAINT "vn_path_lock_id_not_null" NOT NULL id;
ALTER TABLE IF EXISTS "public"."vn_path_lock" ADD CONSTRAINT "vn_path_lock_lock_group_id_not_null" NOT NULL lock_group_id;
ALTER TABLE IF EXISTS "public"."vn_path_lock" ADD CONSTRAINT "vn_path_lock_owner_id_not_null" NOT NULL owner_id;
ALTER TABLE IF EXISTS "public"."vn_path_lock" ADD CONSTRAINT "vn_path_lock_scope_key_not_null" NOT NULL scope_key;
ALTER TABLE IF EXISTS "public"."vn_txn_epoch" ADD CONSTRAINT "vn_txn_epoch_pkey" PRIMARY KEY (txn_id);
ALTER TABLE IF EXISTS "public"."vn_txn_epoch" ADD CONSTRAINT "vn_txn_epoch_status_not_null" NOT NULL status;
ALTER TABLE IF EXISTS "public"."vn_txn_epoch" ADD CONSTRAINT "vn_txn_epoch_txn_id_not_null" NOT NULL txn_id;
ALTER TABLE IF EXISTS "public"."vn_txn_epoch" ADD CONSTRAINT "vn_txn_epoch_updated_at_not_null" NOT NULL updated_at;

CREATE INDEX ix_dir_child ON public.vn_dir_entry USING btree (child_id);
CREATE INDEX ix_dir_parent ON public.vn_dir_entry USING btree (parent_id);
CREATE UNIQUE INDEX uq_dir_parent_name ON public.vn_dir_entry USING btree (parent_id, name);
CREATE UNIQUE INDEX uq_vn_inode_scope_key ON public.vn_inode USING btree (scope_key);
CREATE INDEX ix_inode_path_inode ON public.vn_inode_path_segment USING btree (inode_id);
CREATE UNIQUE INDEX uq_inode_ord ON public.vn_inode_path_segment USING btree (inode_id, ord);
CREATE INDEX ix_vn_lock_group_active_expires ON public.vn_lock_group USING btree (active, expires_at);
CREATE INDEX ix_vn_lock_group_owner_active ON public.vn_lock_group USING btree (owner_id, active);
CREATE INDEX ix_ver_inode_timestamp ON public.vn_node_version USING btree (inode_id, "timestamp" DESC, id DESC);
CREATE INDEX ix_vn_path_lock_group ON public.vn_path_lock USING btree (lock_group_id);
CREATE INDEX ix_vn_path_lock_owner_active ON public.vn_path_lock USING btree (owner_id, active);
CREATE INDEX ix_vn_path_lock_scope_key_gist ON public.vn_path_lock USING gist (scope_key);
CREATE INDEX ix_vn_txn_epoch_status ON public.vn_txn_epoch USING btree (status);
CREATE SEQUENCE IF NOT EXISTS "public"."vn_fence_token_seq";
CREATE SEQUENCE IF NOT EXISTS "public"."vn_path_lock_id_seq";

--rollback: (baseline) no rollback
