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

package org.openlmis.stockmanagement.domain.stockpoint;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "critical_stock_points", schema = "stockmanagement",
        indexes = @Index(columnList = "facilitytypeid,facilityId"))
public class StockPoint {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID facilityId;

    @Column(nullable = false)
    private UUID facilityTypeId;

    @Column(nullable = false)
    private String facilityType;

    @Column(nullable = false)
    private String facility;

    @Column(nullable = false)
    private String productType;

    @Column(nullable = false)
    private Long stockOnHand;

    @Column(nullable = false, name="min")
    private Integer minimumStockPoint;

    @Column(nullable = false, name="reorder")
    private Integer reorderStockPoint;

    @Column(nullable = false, name="max")
    private Integer maximumStockPoint;

    @Column(nullable = false)
    private String status;
}