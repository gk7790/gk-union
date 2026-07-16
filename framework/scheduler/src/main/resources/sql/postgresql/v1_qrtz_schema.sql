-- gk-union PostgreSQL schema generated from the provided MySQL dump.
-- Source: pasted-text.txt. Navicat metadata is intentionally omitted.

DROP TABLE IF EXISTS qrtz_blob_triggers CASCADE;
CREATE TABLE qrtz_blob_triggers (
  sched_name varchar(120) NOT NULL,
  trigger_name varchar(200) NOT NULL,
  trigger_group varchar(200) NOT NULL,
  blob_data bytea NULL,
  CONSTRAINT pk_qrtz_blob_triggers PRIMARY KEY (sched_name, trigger_name, trigger_group)
);
COMMENT ON TABLE qrtz_blob_triggers IS 'Qrtz定时任务';
CREATE INDEX idx_qrtz_blob_triggers_sched_name ON qrtz_blob_triggers (sched_name, trigger_name, trigger_group);

DROP TABLE IF EXISTS qrtz_calendars CASCADE;
CREATE TABLE qrtz_calendars (
  sched_name varchar(120) NOT NULL,
  calendar_name varchar(200) NOT NULL,
  calendar bytea NOT NULL,
  CONSTRAINT pk_qrtz_calendars PRIMARY KEY (sched_name, calendar_name)
);
COMMENT ON TABLE qrtz_calendars IS 'Qrtz定时任务';

DROP TABLE IF EXISTS qrtz_cron_triggers CASCADE;
CREATE TABLE qrtz_cron_triggers (
  sched_name varchar(120) NOT NULL,
  trigger_name varchar(200) NOT NULL,
  trigger_group varchar(200) NOT NULL,
  cron_expression varchar(120) NOT NULL,
  time_zone_id varchar(80) NULL,
  CONSTRAINT pk_qrtz_cron_triggers PRIMARY KEY (sched_name, trigger_name, trigger_group)
);
COMMENT ON TABLE qrtz_cron_triggers IS 'Qrtz定时任务';

DROP TABLE IF EXISTS qrtz_fired_triggers CASCADE;
CREATE TABLE qrtz_fired_triggers (
  sched_name varchar(120) NOT NULL,
  entry_id varchar(95) NOT NULL,
  trigger_name varchar(200) NOT NULL,
  trigger_group varchar(200) NOT NULL,
  instance_name varchar(200) NOT NULL,
  fired_time bigint NOT NULL,
  sched_time bigint NOT NULL,
  priority integer NOT NULL,
  state varchar(16) NOT NULL,
  job_name varchar(200) NULL,
  job_group varchar(200) NULL,
  is_nonconcurrent varchar(1) NULL,
  requests_recovery varchar(1) NULL,
  CONSTRAINT pk_qrtz_fired_triggers PRIMARY KEY (sched_name, entry_id)
);
COMMENT ON TABLE qrtz_fired_triggers IS 'Qrtz定时任务';
CREATE INDEX idx_qrtz_fired_triggers_idx_qrtz_ft_trig_inst_name ON qrtz_fired_triggers (sched_name, instance_name);
CREATE INDEX idx_qrtz_fired_triggers_idx_qrtz_ft_inst_job_req_rcvry ON qrtz_fired_triggers (sched_name, instance_name, requests_recovery);
CREATE INDEX idx_qrtz_fired_triggers_idx_qrtz_ft_j_g ON qrtz_fired_triggers (sched_name, job_name, job_group);
CREATE INDEX idx_qrtz_fired_triggers_idx_qrtz_ft_jg ON qrtz_fired_triggers (sched_name, job_group);
CREATE INDEX idx_qrtz_fired_triggers_idx_qrtz_ft_t_g ON qrtz_fired_triggers (sched_name, trigger_name, trigger_group);
CREATE INDEX idx_qrtz_fired_triggers_idx_qrtz_ft_tg ON qrtz_fired_triggers (sched_name, trigger_group);

DROP TABLE IF EXISTS qrtz_job_details CASCADE;
CREATE TABLE qrtz_job_details (
  sched_name varchar(120) NOT NULL,
  job_name varchar(200) NOT NULL,
  job_group varchar(200) NOT NULL,
  description varchar(250) NULL,
  job_class_name varchar(250) NOT NULL,
  is_durable varchar(1) NOT NULL,
  is_nonconcurrent varchar(1) NOT NULL,
  is_update_data varchar(1) NOT NULL,
  requests_recovery varchar(1) NOT NULL,
  job_data bytea NULL,
  CONSTRAINT pk_qrtz_job_details PRIMARY KEY (sched_name, job_name, job_group)
);
COMMENT ON TABLE qrtz_job_details IS 'Qrtz定时任务';
CREATE INDEX idx_qrtz_job_details_idx_qrtz_j_req_recovery ON qrtz_job_details (sched_name, requests_recovery);
CREATE INDEX idx_qrtz_job_details_idx_qrtz_j_grp ON qrtz_job_details (sched_name, job_group);

DROP TABLE IF EXISTS qrtz_locks CASCADE;
CREATE TABLE qrtz_locks (
  sched_name varchar(120) NOT NULL,
  lock_name varchar(40) NOT NULL,
  CONSTRAINT pk_qrtz_locks PRIMARY KEY (sched_name, lock_name)
);
COMMENT ON TABLE qrtz_locks IS 'Qrtz定时任务';

DROP TABLE IF EXISTS qrtz_paused_trigger_grps CASCADE;
CREATE TABLE qrtz_paused_trigger_grps (
  sched_name varchar(120) NOT NULL,
  trigger_group varchar(200) NOT NULL,
  CONSTRAINT pk_qrtz_paused_trigger_grps PRIMARY KEY (sched_name, trigger_group)
);
COMMENT ON TABLE qrtz_paused_trigger_grps IS 'Qrtz定时任务';

DROP TABLE IF EXISTS qrtz_scheduler_state CASCADE;
CREATE TABLE qrtz_scheduler_state (
  sched_name varchar(120) NOT NULL,
  instance_name varchar(200) NOT NULL,
  last_checkin_time bigint NOT NULL,
  checkin_interval bigint NOT NULL,
  CONSTRAINT pk_qrtz_scheduler_state PRIMARY KEY (sched_name, instance_name)
);
COMMENT ON TABLE qrtz_scheduler_state IS 'Qrtz定时任务';

DROP TABLE IF EXISTS qrtz_simple_triggers CASCADE;
CREATE TABLE qrtz_simple_triggers (
  sched_name varchar(120) NOT NULL,
  trigger_name varchar(200) NOT NULL,
  trigger_group varchar(200) NOT NULL,
  repeat_count bigint NOT NULL,
  repeat_interval bigint NOT NULL,
  times_triggered bigint NOT NULL,
  CONSTRAINT pk_qrtz_simple_triggers PRIMARY KEY (sched_name, trigger_name, trigger_group)
);
COMMENT ON TABLE qrtz_simple_triggers IS 'Qrtz定时任务';

DROP TABLE IF EXISTS qrtz_simprop_triggers CASCADE;
CREATE TABLE qrtz_simprop_triggers (
  sched_name varchar(120) NOT NULL,
  trigger_name varchar(200) NOT NULL,
  trigger_group varchar(200) NOT NULL,
  str_prop_1 varchar(512) NULL,
  str_prop_2 varchar(512) NULL,
  str_prop_3 varchar(512) NULL,
  int_prop_1 integer NULL,
  int_prop_2 integer NULL,
  long_prop_1 bigint NULL,
  long_prop_2 bigint NULL,
  dec_prop_1 numeric(13, 4) NULL,
  dec_prop_2 numeric(13, 4) NULL,
  bool_prop_1 varchar(1) NULL,
  bool_prop_2 varchar(1) NULL,
  CONSTRAINT pk_qrtz_simprop_triggers PRIMARY KEY (sched_name, trigger_name, trigger_group)
);
COMMENT ON TABLE qrtz_simprop_triggers IS 'Qrtz定时任务';

DROP TABLE IF EXISTS qrtz_triggers CASCADE;
CREATE TABLE qrtz_triggers (
  sched_name varchar(120) NOT NULL,
  trigger_name varchar(200) NOT NULL,
  trigger_group varchar(200) NOT NULL,
  job_name varchar(200) NOT NULL,
  job_group varchar(200) NOT NULL,
  description varchar(250) NULL,
  next_fire_time bigint NULL,
  prev_fire_time bigint NULL,
  priority integer NULL,
  trigger_state varchar(16) NOT NULL,
  trigger_type varchar(8) NOT NULL,
  start_time bigint NOT NULL,
  end_time bigint NULL,
  calendar_name varchar(200) NULL,
  misfire_instr smallint NULL,
  job_data bytea NULL,
  CONSTRAINT pk_qrtz_triggers PRIMARY KEY (sched_name, trigger_name, trigger_group)
);
COMMENT ON TABLE qrtz_triggers IS 'Qrtz定时任务';
CREATE INDEX idx_qrtz_triggers_idx_qrtz_t_j ON qrtz_triggers (sched_name, job_name, job_group);
CREATE INDEX idx_qrtz_triggers_idx_qrtz_t_jg ON qrtz_triggers (sched_name, job_group);
CREATE INDEX idx_qrtz_triggers_idx_qrtz_t_c ON qrtz_triggers (sched_name, calendar_name);
CREATE INDEX idx_qrtz_triggers_idx_qrtz_t_g ON qrtz_triggers (sched_name, trigger_group);
CREATE INDEX idx_qrtz_triggers_idx_qrtz_t_state ON qrtz_triggers (sched_name, trigger_state);
CREATE INDEX idx_qrtz_triggers_idx_qrtz_t_n_state ON qrtz_triggers (sched_name, trigger_name, trigger_group, trigger_state);
CREATE INDEX idx_qrtz_triggers_idx_qrtz_t_n_g_state ON qrtz_triggers (sched_name, trigger_group, trigger_state);
CREATE INDEX idx_qrtz_triggers_idx_qrtz_t_next_fire_time ON qrtz_triggers (sched_name, next_fire_time);
CREATE INDEX idx_qrtz_triggers_idx_qrtz_t_nft_st ON qrtz_triggers (sched_name, trigger_state, next_fire_time);
CREATE INDEX idx_qrtz_triggers_idx_qrtz_t_nft_misfire ON qrtz_triggers (sched_name, misfire_instr, next_fire_time);
CREATE INDEX idx_qrtz_triggers_idx_qrtz_t_nft_st_misfire ON qrtz_triggers (sched_name, misfire_instr, next_fire_time, trigger_state);
CREATE INDEX idx_qrtz_triggers_idx_qrtz_t_nft_st_misfire_grp ON qrtz_triggers (sched_name, misfire_instr, next_fire_time, trigger_group, trigger_state);

DROP TABLE IF EXISTS schedule_job CASCADE;
CREATE TABLE schedule_job (
  id bigint NOT NULL,
  schedule_group varchar(255) NULL,
  bean_name varchar(200) NULL,
  params varchar(2000) NULL,
  cron_expression varchar(100) NULL,
  status smallint NULL,
  remark varchar(255) NULL,
  created_by bigint NULL,
  created_at timestamptz(3) NULL,
  updated_by bigint NULL,
  updated_at timestamptz(3) NULL,
  CONSTRAINT pk_schedule_job PRIMARY KEY (id)
);
COMMENT ON TABLE schedule_job IS '定时任务';
COMMENT ON COLUMN schedule_job.id IS 'id';
COMMENT ON COLUMN schedule_job.schedule_group IS '任务分组';
COMMENT ON COLUMN schedule_job.bean_name IS 'spring bean名称';
COMMENT ON COLUMN schedule_job.params IS '参数';
COMMENT ON COLUMN schedule_job.cron_expression IS 'cron表达式';
COMMENT ON COLUMN schedule_job.status IS '任务状态  0：暂停  1：正常';
COMMENT ON COLUMN schedule_job.remark IS '备注';
COMMENT ON COLUMN schedule_job.created_by IS '创建者';
COMMENT ON COLUMN schedule_job.created_at IS '创建时间';
COMMENT ON COLUMN schedule_job.updated_by IS '更新者';
COMMENT ON COLUMN schedule_job.updated_at IS '更新时间';
CREATE INDEX idx_schedule_job_idx_created_at ON schedule_job (created_at);

DROP TABLE IF EXISTS schedule_job_log CASCADE;
CREATE TABLE schedule_job_log (
  id bigint NOT NULL,
  job_id bigint NOT NULL,
  bean_name varchar(200) NULL,
  params varchar(2000) NULL,
  status smallint NOT NULL,
  result text NULL,
  error varchar(2000) NULL,
  times integer NOT NULL,
  created_at timestamptz(3) NULL,
  CONSTRAINT pk_schedule_job_log PRIMARY KEY (id)
);
COMMENT ON TABLE schedule_job_log IS '定时任务日志';
COMMENT ON COLUMN schedule_job_log.id IS 'id';
COMMENT ON COLUMN schedule_job_log.job_id IS '任务id';
COMMENT ON COLUMN schedule_job_log.bean_name IS 'spring bean名称';
COMMENT ON COLUMN schedule_job_log.params IS '参数';
COMMENT ON COLUMN schedule_job_log.status IS '任务状态    0：失败    1：成功';
COMMENT ON COLUMN schedule_job_log.result IS '结果';
COMMENT ON COLUMN schedule_job_log.error IS '失败信息';
COMMENT ON COLUMN schedule_job_log.times IS '耗时(单位：毫秒)';
COMMENT ON COLUMN schedule_job_log.created_at IS '创建时间';
CREATE INDEX idx_schedule_job_log_idx_job_id ON schedule_job_log (job_id);
CREATE INDEX idx_schedule_job_log_idx_created_at ON schedule_job_log (created_at);

-- Foreign keys
ALTER TABLE qrtz_blob_triggers
  ADD CONSTRAINT fk_qrtz_blob_triggers_1
  FOREIGN KEY (sched_name, trigger_name, trigger_group)
  REFERENCES qrtz_triggers (sched_name, trigger_name, trigger_group) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE qrtz_cron_triggers
  ADD CONSTRAINT fk_qrtz_cron_triggers_1
  FOREIGN KEY (sched_name, trigger_name, trigger_group)
  REFERENCES qrtz_triggers (sched_name, trigger_name, trigger_group) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE qrtz_simple_triggers
  ADD CONSTRAINT fk_qrtz_simple_triggers_1
  FOREIGN KEY (sched_name, trigger_name, trigger_group)
  REFERENCES qrtz_triggers (sched_name, trigger_name, trigger_group) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE qrtz_simprop_triggers
  ADD CONSTRAINT fk_qrtz_simprop_triggers_1
  FOREIGN KEY (sched_name, trigger_name, trigger_group)
  REFERENCES qrtz_triggers (sched_name, trigger_name, trigger_group) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE qrtz_triggers
  ADD CONSTRAINT fk_qrtz_triggers_1
  FOREIGN KEY (sched_name, job_name, job_group)
  REFERENCES qrtz_job_details (sched_name, job_name, job_group) ON DELETE RESTRICT ON UPDATE RESTRICT;
