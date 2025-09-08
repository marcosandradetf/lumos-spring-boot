DROP INDEX IF EXISTS idx_team_name_plate;

CREATE UNIQUE INDEX IF NOT EXISTS idx_team_name_unique
    ON team (team_name);

CREATE UNIQUE INDEX IF NOT EXISTS idx_team_plate_unique
    ON team (plate_vehicle);
