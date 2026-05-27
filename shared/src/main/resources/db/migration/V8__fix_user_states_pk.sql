-- Fix user_states to support both bots per user simultaneously.
-- Previously telegram_id alone was the PK, so student and admin bots
-- overwrote each other's state for the same Telegram user.

ALTER TABLE user_states DROP CONSTRAINT user_states_pkey;
ALTER TABLE user_states ADD PRIMARY KEY (telegram_id, bot_type);

-- Old single-column index is no longer needed; composite PK covers it
DROP INDEX IF EXISTS idx_user_states_telegram_id;
