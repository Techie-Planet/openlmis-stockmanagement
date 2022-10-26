package org.openlmis.stockmanagement.service.abstracts;

import java.util.List;
import org.openlmis.stockmanagement.domain.stockpoint.StockPoint;

public interface StockPointService {
    List<StockPoint> findStockPoints();
}
