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

package org.openlmis.stockmanagement.domain.sublot;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.hibernate.annotations.Type;
import lombok.Data;
import org.openlmis.stockmanagement.domain.BaseEntity;
import org.openlmis.stockmanagement.dto.referencedata.LotDto;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Data
@Table(name = "sublots", schema = "stockmanagement")
public class Sublot extends BaseEntity {
    @ManyToOne
    @Column(nullable = false, name = "lotid")
    @Type(type = PG_UUID)
    private LotDto lot;
    @Column(nullable = false)
    private String sublotCode;
    @Column(nullable = false)
    private UUID facilityId;
}
