/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.openlmis.stockmanagement.repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.openlmis.stockmanagement.domain.sourcedestination.ValidSourceAssignment;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ValidSourceAssignmentRepository extends
    SourceDestinationAssignmentRepository<ValidSourceAssignment> {
    @Query(value = "with facility_geo_level_map as (select * from unnest(:facilityGeoLevelMap))\n" +
            "\n" +
            "select vs.*, f.name from stockmanagement.valid_source_assignments vs\n" +
            "join stockmanagement.nodes node on node.id = vs.nodeid\n" +
            "join referencedata.facilities f on f.id = node.referenceid\n" +
            "join referencedata.geographic_zones gz on gz.id = f.geographiczoneid\n" +
            "join referencedata.geographic_levels gl on gl.id = gz.levelid\n" +
            "join facility_geo_level_map fglm " +
            "on fglm.key = vs.geolevelaffinityid and fglm.value = gz.id\n" +
            "where vs.facilitytypeid = :facilityTypeId\n" +
            "and vs.programid = :programId", nativeQuery = true)
    List<ValidSourceAssignment> findOnlyValidByFacilityGeoLevelMap(
            @Param("facilityGeoLevelMap") List<Map.Entry<UUID, UUID>> facilityGeoLevelMap,
            @Param("facilityTypeId") UUID facilityTypeId,
            @Param("programId") UUID programId);


}
