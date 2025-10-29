-- Add OAuth provider fields to users table
ALTER TABLE users 
ADD COLUMN provider VARCHAR(20) DEFAULT 'local',
ADD COLUMN provider_id VARCHAR(255) NULL;

-- Create index for provider lookups
CREATE INDEX idx_user_provider ON users(provider, provider_id);

-- Update existing users to have 'local' provider
UPDATE users SET provider = 'local' WHERE provider IS NULL;

