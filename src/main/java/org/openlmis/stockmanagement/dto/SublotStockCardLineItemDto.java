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

package org.openlmis.stockmanagement.dto;

import static org.openlmis.stockmanagement.dto.StockCardLineItemDto.createFrom;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import javax.persistence.Convert;
import lombok.Builder;
import lombok.Data;
import org.openlmis.stockmanagement.domain.ExtraDataConverter;
import org.openlmis.stockmanagement.domain.sublot.SublotStockCardLineItem;
import org.openlmis.stockmanagement.dto.StockCardLineItemDto;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class SublotStockCardLineItemDto {
    @JsonUnwrapped
    private StockCardLineItemDto stockCardLineItemDto;
    @Convert(converter = ExtraDataConverter.class)
    private Map<String, String> sublotStockCardLineItemExtraData;
    private UUID sublotStockCardId;

    public static SublotStockCardLineItemDto createFromLineItem(
            SublotStockCardLineItem sublotStockCardLineItem) {
        return SublotStockCardLineItemDto.builder()
                .stockCardLineItemDto(createFrom(sublotStockCardLineItem.getStockCardLineItem()))
                .sublotStockCardLineItemExtraData(
                        sublotStockCardLineItem.getSublotStockCardLineItemExtraData())
                .sublotStockCardId(sublotStockCardLineItem.getSublotStockCard().getId())
                .build();

    }
}
