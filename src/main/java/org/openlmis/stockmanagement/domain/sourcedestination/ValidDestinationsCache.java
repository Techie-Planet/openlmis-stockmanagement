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

package org.openlmis.stockmanagement.domain.sourcedestination;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Data;
import org.hibernate.annotations.Type;
import org.openlmis.stockmanagement.domain.BaseEntity;


@Entity
@Data
@Table(name = "valid_destinations_cache", schema = "stockmanagement",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"facilityid", "programid"})})
public class ValidDestinationsCache extends BaseEntity {
    @Column(nullable = false)
    @Type(type = PG_UUID)
    private UUID facilityId;
    @Column(nullable = false)
    @Type(type = PG_UUID)
    private UUID programId;
    @Column(name = "valid_destinations", columnDefinition = "jsonb")
    private String validDestinations;

}
