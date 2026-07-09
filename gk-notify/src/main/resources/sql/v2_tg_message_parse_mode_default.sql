-- Align Telegram outbound message default parse mode with application defaults.
ALTER TABLE `tg_message_task`
  MODIFY COLUMN `parse_mode` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'HTML' COMMENT '解析模式: HTML/MarkdownV2/NONE';
