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

import java.util.List;
import org.openlmis.stockmanagement.service.StockCardSummaries;
import org.openlmis.stockmanagement.service.StockCardSummariesService;
import org.openlmis.stockmanagement.service.StockCardSummariesV3SearchParams;
import org.openlmis.stockmanagement.web.Pagination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
// import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v3/stockCardSummaries")
public class StockCardSummariesV3Controller {

  private static final Logger LOGGER =
           LoggerFactory.getLogger(StockCardSummariesV3Controller.class);

  @Autowired
  private StockCardSummariesService stockCardSummariesService;

  @Autowired
  private StockCardSummariesV3DtoBuilder stockCardSummariesV3DtoBuilder;

  /**
   * Get stock card summaries by program and facility.
   *
   * @return Stock card summaries.
   */
  // @PreAuthorize("hasRole('ROLE_ANONYMOUS')")
  @GetMapping
  public Page<StockCardSummaryV3Dto> getStockCardSummaries(
      @RequestParam MultiValueMap<String, String> parameters,
      @PageableDefault(size = Integer.MAX_VALUE) Pageable pageable) {

    Profiler profiler = new Profiler("GET_STOCK_CARDS_V3");
    profiler.setLogger(LOGGER);

    profiler.start("VALIDATE_PARAMS");
    StockCardSummariesV3SearchParams params =
            new StockCardSummariesV3SearchParams(parameters);

    profiler.start("GET_STOCK_CARD_SUMMARIES");

    StockCardSummaries summaries = stockCardSummariesService.findStockCards(params);

    profiler.start("TO_DTO");
    List<StockCardSummaryV3Dto> dtos = stockCardSummariesV3DtoBuilder.build(
            summaries.getPageOfApprovedProducts(),
            summaries.getStockCardsForFulfillOrderables(),
            summaries.getOrderableFulfillMap(),
            params.getVvmStatus(),
            params.getLotCode(),
            params.isNonEmptyOnly(),
            params.isHideZeroItems());

    profiler.start("GET_PAGE");
    Page<StockCardSummaryV3Dto> page = Pagination.getPage(dtos, pageable);

    profiler.stop().log();
    return page;
  }
}
