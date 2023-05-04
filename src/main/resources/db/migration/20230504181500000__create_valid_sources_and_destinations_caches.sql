CREATE TABLE valid_sources_cache
(
    id UUID PRIMARY KEY NOT NULL,
    facilityid UUID NOT NULL,
    programid UUID NOT NULL,
    validsources json NOT NULL
);
CREATE TABLE valid_destinations_cache
(
    id UUID PRIMARY KEY NOT NULL,
    facilityid UUID NOT NULL,
    programid UUID NOT NULL,
    validdestinations json
);