ALTER TABLE maintenance
    ALTER COLUMN date_of_visit
        TYPE timestamptz
        USING date_of_visit AT TIME ZONE 'UTC';
