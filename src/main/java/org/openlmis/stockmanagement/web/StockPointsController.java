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

package org.openlmis.stockmanagement.web;

import java.util.List;
import org.openlmis.stockmanagement.domain.stockpoint.StockPoint;
import org.openlmis.stockmanagement.service.JasperReportService;
import org.openlmis.stockmanagement.service.abstracts.StockPointService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/stockPoints")
public class StockPointsController {
  @Autowired
  StockPointService stockPointService;

  @Autowired
  private JasperReportService reportService;

  @PreAuthorize("hasRole('ROLE_ANONYMOUS')")
  @GetMapping
  public List<StockPoint> getStockPoints(@RequestParam MultiValueMap<String, String> parameters) {
    return stockPointService.findStockPoints(parameters);
  }

  /**
   * Get critical stock points report.
   *
   * @return generated PDF report
   */
  @GetMapping(value = "/print")
  @PreAuthorize("hasRole('ROLE_ANONYMOUS')")
  public ResponseEntity<byte[]> getCriticalStockPointsReport() {
    byte[] report = reportService.generateCriticalStockPointsReport();

    return ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header("Content-Disposition",
                    "inline; filename=critical_stock_points" + "_" + ".pdf")
            .body(report);
  }
}