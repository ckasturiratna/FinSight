-- Clean up duplicate email verification entries
-- This script removes duplicate entries and keeps only the most recent one

-- First, let's see what duplicates exist
SELECT email, COUNT(*) as count 
FROM email_verifications 
GROUP BY email 
HAVING COUNT(*) > 1;

-- Delete older duplicate entries, keeping only the most recent one
DELETE ev1 FROM email_verifications ev1
INNER JOIN email_verifications ev2 
WHERE ev1.email = ev2.email 
AND ev1.id < ev2.id;

-- Verify cleanup
SELECT email, COUNT(*) as count 
FROM email_verifications 
GROUP BY email 
HAVING COUNT(*) > 1;



