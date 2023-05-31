ALTER TABLE valid_destinations_cache
ADD CONSTRAINT valid_destinations_cache_unique_facilityid_programid UNIQUE (facilityid, programid);

ALTER TABLE valid_sources_cache
    ADD CONSTRAINT valid_sources_cache_unique_facilityid_programid UNIQUE (facilityid, programid);