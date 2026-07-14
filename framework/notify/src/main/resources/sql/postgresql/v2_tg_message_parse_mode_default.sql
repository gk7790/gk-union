-- Align Telegram outbound message default parse mode with application defaults.
ALTER TABLE "tg_message_task"
  MODIFY COLUMN "parse_mode" varchar(16) NOT NULL DEFAULT 'HTML'
