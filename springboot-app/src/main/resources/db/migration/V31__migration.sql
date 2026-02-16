CREATE OR REPLACE FUNCTION fn_update_direct_execution_first_street()
    RETURNS TRIGGER AS
$$
BEGIN
    UPDATE direct_execution
    SET
        direct_execution_status = 'IN_PROGRESS',
        started_at = COALESCE(NEW.finished_at, NOW())
    WHERE direct_execution_id = NEW.direct_execution_id
        AND started_at IS NULL;

    RETURN NEW;
END;
$$
    LANGUAGE plpgsql;

CREATE TRIGGER trg_first_direct_execution_street
    AFTER INSERT ON direct_execution_street
    FOR EACH ROW
EXECUTE FUNCTION fn_update_direct_execution_first_street();

CREATE INDEX IF NOT EXISTS idx_direct_execution_street_execution_id
    ON public.direct_execution_street (direct_execution_id);

-- pre_measurement

CREATE OR REPLACE FUNCTION fn_after_pre_measurement_street_finished()
    RETURNS TRIGGER AS
$$
BEGIN
    -- sua lógica aqui
    UPDATE pre_measurement
    SET
        status = 'IN_PROGRESS',
        started_at = COALESCE(NEW.finished_at ,NOW())
    WHERE pre_measurement_id = NEW.pre_measurement_id
      AND started_at IS NULL;

    RETURN NEW;
END;
$$
    LANGUAGE plpgsql;


CREATE TRIGGER trg_pre_measurement_street_finished
    AFTER UPDATE ON pre_measurement_street
    FOR EACH ROW
    WHEN (
        OLD.street_status IS DISTINCT FROM NEW.street_status
            AND NEW.street_status = 'FINISHED'
        )
EXECUTE FUNCTION fn_after_pre_measurement_street_finished();

CREATE INDEX IF NOT EXISTS idx_pre_measurement_street_execution_id
    ON public.pre_measurement_street (pre_measurement_id);