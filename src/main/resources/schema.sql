DO $$
BEGIN
    IF to_regclass('comments') IS NOT NULL
            AND to_regclass('messages') IS NULL THEN
        ALTER TABLE comments RENAME TO messages;
    END IF;
END
$$;
