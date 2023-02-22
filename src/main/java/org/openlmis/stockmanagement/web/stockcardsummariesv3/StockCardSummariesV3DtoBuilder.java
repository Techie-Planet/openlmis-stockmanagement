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

package org.openlmis.stockmanagement.web.stockcardsummariesv3;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.MapUtils;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.dto.ObjectReferenceDto;
import org.openlmis.stockmanagement.dto.referencedata.LotDto;
import org.openlmis.stockmanagement.dto.referencedata.OrderableDto;
import org.openlmis.stockmanagement.dto.referencedata.OrderableFulfillDto;
import org.openlmis.stockmanagement.dto.referencedata.VersionObjectReferenceDto;
import org.openlmis.stockmanagement.service.referencedata.LotReferenceDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("PMD.TooManyMethods")
public class StockCardSummariesV3DtoBuilder {

  static final String ORDERABLES = "orderables";
  static final String STOCK_CARDS = "stockCards";
  static final String LOTS = "lots";

  @Value("${service.url}")
  private String serviceUrl;
  @Autowired
  private LotReferenceDataService lotReferenceDataService;

  /**
   * Builds Stock Card Summary dtos from stock cards and orderables.
   *
   * @param approvedProducts list of {@link OrderableDto} that summaries will be based on
   * @param stockCards       list of {@link StockCard} found for orderables
   * @param orderables       map of orderable ids as keys and {@link OrderableFulfillDto}
   * @param vvmStatus       search parameter for VVM Statuses
   * @param nonEmptySummariesOnly       search parameter to filter out only nonEmpty StockCards
   * @return list of {@link StockCardSummaryV3Dto}
   */
  public List<StockCardSummaryV3Dto> build(List<OrderableDto> approvedProducts,
                                           List<StockCard> stockCards,
                                           Map<UUID, OrderableFulfillDto> orderables,
                                           String vvmStatus,
                                           String lotCode,
                                           boolean nonEmptySummariesOnly,
                                           boolean hideZeroItems) {

    List<StockCardSummaryV3Dto> summariesList = buildSummariesList(
            approvedProducts, stockCards, orderables);

    if (Objects.nonNull(vvmStatus)) {
      summariesList = filterVvmStatus(summariesList, vvmStatus);
    }

    if (hideZeroItems) {
      summariesList = filterZeroItems(summariesList, hideZeroItems);
    }

    if (nonEmptySummariesOnly) {
      summariesList = filterNonEmptySummaries(summariesList);
    }
    //filter for lotcode
    if (Objects.nonNull(lotCode)) {
      summariesList = filterLotCode(summariesList, lotCode);
    }

    // return summariesList.sorted().collect(toList());
    return summariesList;
  }

  private StockCardSummaryV3Dto build(List<StockCard> stockCards, UUID orderableId,
                                      Long orderableVersionNumber, OrderableFulfillDto fulfills) {

    Set<CanFulfillForMeEntryExtDto> canFulfillSet = null == fulfills ? new HashSet<>()
            : fulfills.getCanFulfillForMe()
            .stream()
            .map(id -> buildFulfillsEntries(id,
                    findStockCardByOrderableId(id, stockCards)))
            .flatMap(List::stream)
            .collect(toSet());

    canFulfillSet.addAll(
            buildFulfillsEntries(
                    orderableId,
                    findStockCardByOrderableId(orderableId, stockCards)));

    return new StockCardSummaryV3Dto(createVersionReference(
            orderableId, ORDERABLES, orderableVersionNumber),canFulfillSet);
  }

  private List<StockCardSummaryV3Dto> buildSummariesList(
          List<OrderableDto> approvedProducts,
          List<StockCard> stockCards,
          Map<UUID, OrderableFulfillDto> orderables) {

    return approvedProducts.stream()
            .map(p -> build(stockCards, p.getId(), p.getMeta().getVersionNumber(),
                    MapUtils.isEmpty(orderables) ? null : orderables.get(p.getId())))
            .sorted()
            .collect(toList());
  }

  private List<StockCardSummaryV3Dto> filterNonEmptySummaries(
          List<StockCardSummaryV3Dto> summariesList) {

    return summariesList.stream()
            .filter(summary -> !summary.getCanFulfillForMe().isEmpty())
            .collect(toList());
  }

  private List<StockCardSummaryV3Dto> filterVvmStatus(
          List<StockCardSummaryV3Dto> summariesList, String vvmStatus) {

    // Set<CanFulfillForMeEntryExtDto>
    Stream<StockCardSummaryV3Dto> summariesStream = summariesList.stream().map(summary -> {
      Iterator<CanFulfillForMeEntryExtDto> iterator = summary.getCanFulfillForMe().iterator();
      while (iterator.hasNext()) {
        CanFulfillForMeEntryExtDto cffm = iterator.next();

        Map<String, String> extraData = cffm.getExtraData();
        if (Objects.isNull(cffm.getExtraData()) ||
                Objects.isNull(extraData.get("vvmStatus"))) {
          // if no vvm.... leave it
          if (!vvmStatus.equalsIgnoreCase("noVVM")) {
            iterator.remove();
          }
          continue;
        }

        //if (Objects.isNull(extraData.get("vvmStatus"))) {
        //  iterator.remove();
        //  continue;
        //}

        if (!extraData.get("vvmStatus").equalsIgnoreCase(vvmStatus)) {
          iterator.remove();
        }
      }
      return summary;
    });

    return summariesStream.sorted().collect(toList());
  }

  private List<StockCardSummaryV3Dto> filterLotCode(
          List<StockCardSummaryV3Dto> summariesList, String lotCode) {

    // Set<CanFulfillForMeEntryExtDto>
    Stream<StockCardSummaryV3Dto> summariesStream = summariesList.stream().map(summary -> {
      Iterator<CanFulfillForMeEntryExtDto> iterator = summary.getCanFulfillForMe().iterator();
      while (iterator.hasNext()) {
        CanFulfillForMeEntryExtDto cffm = iterator.next();

        if (Objects.isNull(cffm.getLot())) {
          iterator.remove();
          continue;
        }

        LotDto lot = lotReferenceDataService.findOne(cffm.getLot().getId());
        if (Objects.isNull(lot.getLotCode())) {
          iterator.remove();
          continue;
        }

        if (!lot.getLotCode().equalsIgnoreCase(lotCode)) {
          iterator.remove();
        }
      }
      return summary;
    });

    return summariesStream.sorted().collect(toList());
  }

  private List<StockCardSummaryV3Dto> filterZeroItems(
          List<StockCardSummaryV3Dto> summariesList, boolean hideZeroItems) {

    // Set<CanFulfillForMeEntryExtDto>
    Stream<StockCardSummaryV3Dto> summariesStream = summariesList.stream().map(summary -> {
      Iterator<CanFulfillForMeEntryExtDto> iterator = summary.getCanFulfillForMe().iterator();
      while (iterator.hasNext()) {
        CanFulfillForMeEntryExtDto cffm = iterator.next();

        if (cffm.getStockOnHand() == 0) {
          iterator.remove();
        }
      }
      return summary;
    });

    return summariesStream.sorted().collect(toList());
  }

  private List<CanFulfillForMeEntryExtDto> buildFulfillsEntries(
          UUID orderableId, List<StockCard> stockCards) {

    if (isEmpty(stockCards)) {
      return Collections.emptyList();
    } else {
      return stockCards.stream()
          .map(stockCard -> createCanFulfillForMeEntryExtDto(stockCard, orderableId))
          .collect(Collectors.toCollection(ArrayList::new));
    }
  }

  private CanFulfillForMeEntryExtDto createCanFulfillForMeEntryExtDto(
          StockCard stockCard, UUID orderableId) {

    Optional<StockCardLineItem> lineItem = stockCard
            .getLineItems()
            .stream()
            .reduce((first, second) -> second);

    return new CanFulfillForMeEntryExtDto(
            createStockCardReference(stockCard.getId()),
            createReference(orderableId, ORDERABLES),
            stockCard.getLotId() == null ? null : createLotReference(stockCard.getLotId()),
            stockCard.getStockOnHand(),
            stockCard.getOccurredDate(),
            stockCard.getProcessedDate(),
            stockCard.isActive(),
            lineItem.isPresent() ? lineItem.get().getExtraData() : null
    );
  }

  private List<StockCard> findStockCardByOrderableId(UUID orderableId,
                                                        List<StockCard> stockCards) {
    return stockCards
        .stream()
        .filter(card -> card.getOrderableId().equals(orderableId))
        .collect(toList());
  }

  private ObjectReferenceDto createStockCardReference(UUID id) {
    return createReference(id, STOCK_CARDS);
  }

  private ObjectReferenceDto createLotReference(UUID id) {
    return createReference(id, LOTS);
  }

  private ObjectReferenceDto createReference(UUID id, String resourceName) {
    return new ObjectReferenceDto(serviceUrl, resourceName, id);
  }

  private VersionObjectReferenceDto createVersionReference(
          UUID id, String resourceName,Long versionNumber) {
    return new VersionObjectReferenceDto(id, serviceUrl, resourceName, versionNumber);
  }
}
