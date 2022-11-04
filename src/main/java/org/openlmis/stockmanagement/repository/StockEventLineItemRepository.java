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
import java.util.UUID;
import org.openlmis.stockmanagement.domain.event.StockEventLineItem;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;


public interface StockEventLineItemRepository
    extends PagingAndSortingRepository<StockEventLineItem, UUID> {
    
  @Query("SELECT DISTINCT sel"
      + " FROM StockEventLineItem AS sel"
      + " JOIN Node n on n.id = sel.destinationId and n.isRefDataFacility = true"
      + " WHERE n.referenceId = :facilityId" 
      + " AND sel.stockEvent.programId = :programId"
      + " AND sel.stockEvent.documentNumber = :documentNumberRef")
  List<StockEventLineItem> getAllLineItemIssuedToFacility(
                                            @Param("programId") UUID program,
                                            @Param("facilityId") UUID facility,
                                            @Param("documentNumberRef") String documentNumber);

  @Query("SELECT DISTINCT  sel.stockEvent.documentNumber"
      + " FROM StockEventLineItem AS sel"
      + " JOIN Node n on n.id = sel.destinationId and n.isRefDataFacility = true"
      + " WHERE n.referenceId = :facilityId" 
      + " AND sel.stockEvent.programId = :programId"
      + " AND sel.stockEvent.documentNumber IS NOT NULL"
      + " AND sel.stockEvent.documentNumber NOT IN ("
      + " SELECT DISTINCT sEvent.documentNumber FROM StockEvent sEvent"
      + " WHERE sEvent.programId = :programId"
      + " AND sEvent.facilityId = :facilityId"
      + " AND sEvent.documentNumber IS NOT NULL )")
  List<String> getAllLineItemIssuedToFacilityNumber(
                                            @Param("programId") UUID program,
                                            @Param("facilityId") UUID facility);
}
