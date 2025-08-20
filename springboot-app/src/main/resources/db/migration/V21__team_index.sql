CREATE UNIQUE INDEX IF NOT EXISTS idx_team_name_plate
    ON team (team_name, plate_vehicle);
