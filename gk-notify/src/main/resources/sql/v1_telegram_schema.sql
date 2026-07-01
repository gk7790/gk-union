/*
 Telegram Bot Schema (MVP)

 Source Schema : gk-union
 Target Server Type    : MySQL
 Target Server Version : 80036 (8.0.36)
 File Encoding         : 65001

 说明: Telegram 机器人, 含"出站推送" + "入站指令" 两条链路, 共 5 张表:
   [出站] tg_bot          机器人配置(BotToken 加密落库)
   [出站] tg_chat         推送目标会话/群, 内嵌事件订阅字段
   [出站] tg_message_task 出站消息任务(状态机 + 重试 + 死信 + 抢占锁), 复用 merchant_notify_task 模式
   [入站] tg_account      TG用户 ↔ 系统主体绑定(指令鉴权核心)
   [入站] tg_update_log   入站指令幂等去重 + 审计, 复用 psp_callback_log 思路
 后续可平滑扩展: tg_subscription(订阅拆表)、tg_message_template(模板拆表)
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for tg_bot
-- ----------------------------
DROP TABLE IF EXISTS `tg_bot`;
CREATE TABLE `tg_bot`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `owner_scope` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PLATFORM' COMMENT '归属: PLATFORM/TENANT',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户ID; TENANT必填, PLATFORM为空',
  `bot_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '内部机器人编号',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Bot @username',
  `bot_user_id` bigint NULL DEFAULT NULL COMMENT 'Telegram BotUserId(getMe)',
  `token_cipher` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'BotToken密文(对称加密, 禁止明文)',
  `token_hash` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Token哈希(查重/校验)',
  `secret_token` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Webhook secret_token',
  `webhook_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '已设置的Webhook地址',
  `mode` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'WEBHOOK' COMMENT '模式: WEBHOOK/POLLING',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 1正常 2暂停 3停用',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tg_bot_no`(`bot_no` ASC) USING BTREE,
  UNIQUE INDEX `uk_tg_bot_token`(`token_hash` ASC) USING BTREE,
  INDEX `idx_tg_bot_tenant`(`tenant_id` ASC, `status` ASC) USING BTREE,
  CONSTRAINT `chk_tg_bot_scope` CHECK (`owner_scope` in ('PLATFORM','TENANT')),
  CONSTRAINT `chk_tg_bot_tenant` CHECK (`owner_scope` <> 'TENANT' OR `tenant_id` is not null)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Telegram机器人配置' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tg_chat
-- ----------------------------
DROP TABLE IF EXISTS `tg_chat`;
CREATE TABLE `tg_chat`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户ID; 0=平台',
  `merchant_id` bigint NOT NULL DEFAULT 0 COMMENT '商户ID; 0=租户级/平台级',
  `bot_id` bigint NOT NULL COMMENT '所属机器人ID, 关联tg_bot.id',
  `chat_id` bigint NOT NULL COMMENT 'Telegram ChatId(群为负数)',
  `chat_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型: PRIVATE/GROUP/SUPERGROUP/CHANNEL',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '群/频道名称',
  `purpose` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NOTIFY' COMMENT '用途: NOTIFY/OPS/CUSTOMER',
  `event_types` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '订阅事件(逗号分隔, 空=全部): SYSTEM_ERROR,SYSTEM_WARN,PAYIN_SUCCESS,PAYOUT_SUCCESS,PAYOUT_FAILED,RISK_ALERT',
  `lang` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'en-US' COMMENT '消息语言',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 1正常 2暂停 3停用',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tg_chat_bot_chat`(`bot_id` ASC, `chat_id` ASC) USING BTREE,
  INDEX `idx_tg_chat_tenant`(`tenant_id` ASC, `merchant_id` ASC, `status` ASC) USING BTREE,
  CONSTRAINT `chk_tg_chat_type` CHECK (`chat_type` in ('PRIVATE','GROUP','SUPERGROUP','CHANNEL'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Telegram会话/群组(内嵌事件订阅)' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tg_message_task
-- ----------------------------
DROP TABLE IF EXISTS `tg_message_task`;
CREATE TABLE `tg_message_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户ID; 平台级消息为0',
  `merchant_id` bigint NOT NULL DEFAULT 0 COMMENT '商户ID; 0=非商户级消息',
  `bot_id` bigint NOT NULL COMMENT '机器人ID, 关联tg_bot.id',
  `chat_id` bigint NOT NULL COMMENT '目标ChatId',
  `task_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息任务编号',
  `biz_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '业务类型: PAYIN_ORDER/PAYOUT_ORDER等',
  `biz_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '业务编号',
  `event_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '触发事件',
  `source_event_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '来源Outbox事件ID(幂等)',
  `parse_mode` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MarkdownV2' COMMENT '解析模式: MarkdownV2/HTML/NONE',
  `payload_json` json NOT NULL COMMENT '渲染后消息内容/按钮JSON',
  `payload_hash` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '内容哈希(防重复发送)',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'INIT' COMMENT '状态: INIT/PROCESSING/SUCCESS/FAILED/DEAD',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '已重试次数',
  `max_retry_count` int NOT NULL DEFAULT 8 COMMENT '最大重试次数',
  `next_retry_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '下次重试时间',
  `tg_message_id` bigint NULL DEFAULT NULL COMMENT '发送成功后TG返回的message_id',
  `last_error_code` int NULL DEFAULT NULL COMMENT '最后TG错误码(如429限流)',
  `last_error_msg` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后错误信息',
  `last_attempt_at` datetime(3) NULL DEFAULT NULL COMMENT '最后尝试时间',
  `locked_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '锁定节点',
  `lock_until` datetime(3) NULL DEFAULT NULL COMMENT '锁定过期时间',
  `success_at` datetime(3) NULL DEFAULT NULL COMMENT '成功时间',
  `dead_at` datetime(3) NULL DEFAULT NULL COMMENT '进入死信时间',
  `trace_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '链路追踪ID',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tg_msg_task_no`(`tenant_id` ASC, `task_no` ASC) USING BTREE,
  UNIQUE INDEX `uk_tg_msg_idem`(`bot_id` ASC, `chat_id` ASC, `payload_hash` ASC) USING BTREE,
  INDEX `idx_tg_msg_scan`(`status` ASC, `next_retry_at` ASC, `id` ASC) USING BTREE,
  INDEX `idx_tg_msg_lock`(`lock_until` ASC, `locked_by` ASC) USING BTREE,
  INDEX `idx_tg_msg_biz`(`tenant_id` ASC, `biz_type` ASC, `biz_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Telegram出站消息任务' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tg_account  (TG用户绑定系统主体, 指令鉴权核心)
-- ----------------------------
DROP TABLE IF EXISTS `tg_account`;
CREATE TABLE `tg_account` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',

  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户ID, 冗余自sys_user_subject.tenant_id; 平台主体为空',
  `bot_id` bigint NOT NULL COMMENT '绑定时所用机器人ID, 关联tg_bot.id',

  `tg_user_id` bigint NOT NULL COMMENT 'Telegram用户ID',
  `tg_username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Telegram用户名',
  `language_code` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Telegram语言',

  `user_id` bigint NOT NULL COMMENT '系统用户ID, 关联sys_user.id',
  `subject_id` bigint NOT NULL COMMENT '系统用户主体ID, 关联sys_user_subject.id, 通过该主体确定tenant/merchant范围',

  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 0解绑 1已绑定',
  `bound_at` datetime(3) NULL DEFAULT NULL COMMENT '绑定时间',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',

  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tg_account_bot_user` (`bot_id` ASC, `tg_user_id` ASC) USING BTREE,
  INDEX `idx_tg_account_user` (`user_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_tg_account_subject` (`subject_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_tg_account_tenant` (`tenant_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'Telegram账号绑定'
  ROW_FORMAT = Dynamic;
-- ----------------------------
-- Table structure for tg_update_log  (入站指令幂等 + 审计)
-- ----------------------------
DROP TABLE IF EXISTS `tg_update_log`;
CREATE TABLE `tg_update_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `bot_id` bigint NOT NULL COMMENT '机器人ID, 关联tg_bot.id',
  `update_id` bigint NOT NULL COMMENT 'Telegram update_id(幂等键)',
  `tg_user_id` bigint NULL DEFAULT NULL COMMENT '来源TG用户ID',
  `chat_id` bigint NULL DEFAULT NULL COMMENT '来源ChatId',
  `update_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '类型: message/callback_query等',
  `command` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '命令(如/balance /order)',
  `raw_json` json NOT NULL COMMENT '原始Update报文',
  `handle_status` tinyint NOT NULL DEFAULT 0 COMMENT '处理状态: 0待处理 1成功 2失败',
  `error_msg` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '处理错误信息',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tg_update`(`bot_id` ASC, `update_id` ASC) USING BTREE,
  INDEX `idx_tg_update_chat`(`chat_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Telegram入站更新日志' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
