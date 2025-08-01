ALTER TABLE direct_execution_street
    ADD CONSTRAINT UNIQUE_SEND_STREET UNIQUE (device_id, device_street_id);

ALTER TABLE pre_measurement_street
    ADD CONSTRAINT UNIQUE_PRE_MEASUREMENT_SEND_STREET UNIQUE (device_id, device_street_id);