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

import static org.openlmis.stockmanagement.dto.SublotStockCardLineItemDto.createFromLineItem;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Data;
import org.openlmis.stockmanagement.domain.sublot.Sublot;
import org.openlmis.stockmanagement.domain.sublot.SublotStockCard;
import org.openlmis.stockmanagement.domain.sublot.SublotStockCardLineItem;
import org.openlmis.stockmanagement.dto.SublotStockCardLineItemDto;

@Data
@Builder
public class SublotStockCardDto {
    private UUID stockCardId;
    private Sublot sublot;
    private Integer sublotStockOnHand;
    private List<SublotStockCardLineItemDto> sublotLineItems;

    public static SublotStockCardDto createFrom (SublotStockCard sublotStockCard){
        return SublotStockCardDto.builder()
                .stockCardId(sublotStockCard.getStockCard().getId())
                .sublot(sublotStockCard.getSublot())
                .sublotStockOnHand(sublotStockCard.getSublotStockOnHand())
                .sublotLineItems(sublotStockCard.getLineItems().stream()
                    .map(each -> createFromLineItem(each))
                    .collect(Collectors.toList())
                )
                .build();
    }
}
