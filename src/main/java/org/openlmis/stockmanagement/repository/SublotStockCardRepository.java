package main.java.org.openlmis.stockmanagement.repository;

import java.util.UUID;
import org.openlmis.stockmanagement.domain.sublot.Sublot;
import org.openlmis.stockmanagement.domain.sublot.SublotStockCard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SublotStockCardRepository extends JpaRepository<SublotStockCard, UUID>  {

}
