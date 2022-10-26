CREATE TABLE stockmanagement.critical_stock_points (
                                                       id uuid NOT NULL,
                                                       facilitytypeid uuid NOT NULL,
                                                       facilitytype text NOT NULL,
                                                       facilityid uuid NOT NULL,
                                                       facility text NOT NULL,
                                                       producttype text NOT NULL,
                                                       stockonhand bigint NOT NULL,
                                                       min integer NOT NULL,
                                                       reorder integer NOT NULL,
                                                       max integer NOT NULL,
                                                       status text NOT NULL
);

INSERT INTO stockmanagement.critical_stock_points (id, facilitytypeid, facilitytype, facilityid, facility, producttype, stockonhand, min, reorder, max, status) VALUES ('e0fd28d3-92f5-4fc2-80c3-390e1f7d4159', 'f3a7c5c3-5db6-4c6d-9894-b4508969d3c2', 'National Cold Store', '4f22127c-43ef-4f48-bbc0-34f39b3c1371', 'National Strategic Cold Store', 'MEASLES', 35000, 391892, 979730, 1959459, 'UNDERSTOCKED');
∑INSERT INTO stockmanagement.critical_stock_points (id, facilitytypeid, facilitytype, facilityid, facility, producttype, stockonhand, min, reorder, max, status) VALUES ('b0580df9-abac-46b0-9070-35e6efc3890a', '876b1d85-9f47-4407-a246-cfb9b13d17cf', 'State Cold Store', '83803c23-4242-45bc-a7e7-52e1c9157a1c', 'FCT State Cold Store', 'MEASLES', 60, 9391, 23479, 46957, 'UNDERSTOCKED');
INSERT INTO stockmanagement.critical_stock_points (id, facilitytypeid, facilitytype, facilityid, facility, producttype, stockonhand, min, reorder, max, status) VALUES ('e0097de6-1b1d-4b16-9edd-8fb5b73d837b', '876b1d85-9f47-4407-a246-cfb9b13d17cf', 'State Cold Store', '83803c23-4242-45bc-a7e7-52e1c9157a1c', 'FCT State Cold Store', 'YF', 5000, 6406, 16014, 32028, 'UNDERSTOCKED');
INSERT INTO stockmanagement.critical_stock_points (id, facilitytypeid, facilitytype, facilityid, facility, producttype, stockonhand, min, reorder, max, status) VALUES ('811d5291-ea2b-4544-851a-1eb05aa52c10', 'b97f1b4f-2c4b-4e0d-a703-f9dd3b398c32', 'Zonal Cold Store', 'f0b13aa4-9112-4ce2-a34c-cb3715fee0bf', 'North Central Zonal Cold Store', 'MEASLES', 85, 146959, 367399, 734797, 'UNDERSTOCKED');
INSERT INTO stockmanagement.critical_stock_points (id, facilitytypeid, facilitytype, facilityid, facility, producttype, stockonhand, min, reorder, max, status) VALUES ('68e87d9e-ef99-4210-9395-b5b650074956', 'b97f1b4f-2c4b-4e0d-a703-f9dd3b398c32', 'Zonal Cold Store', 'f0b13aa4-9112-4ce2-a34c-cb3715fee0bf', 'North Central Zonal Cold Store', 'YF', 49951, 100236, 250590, 501179, 'UNDERSTOCKED');
INSERT INTO stockmanagement.critical_stock_points (id, facilitytypeid, facilitytype, facilityid, facility, producttype, stockonhand, min, reorder, max, status) VALUES ('e1fff827-2c04-4dd2-b873-a2db340d63f5', 'b97f1b4f-2c4b-4e0d-a703-f9dd3b398c32', 'Zonal Cold Store', '2ec70696-7284-4738-afa8-01aeeb339925', 'North West Zonal Cold Store', 'MEASLES', 1000, 146959, 367399, 734797, 'UNDERSTOCKED');


ALTER TABLE ONLY stockmanagement.critical_stock_points
    ADD CONSTRAINT critical_stock_points_pkey PRIMARY KEY (id);

