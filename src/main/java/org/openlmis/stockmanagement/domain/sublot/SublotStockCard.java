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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static javax.persistence.CascadeType.ALL;
import static org.hibernate.annotations.LazyCollectionOption.FALSE;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.Data;
//import org.hibernate.annotations.LazyCollection;
import org.openlmis.stockmanagement.domain.BaseEntity;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.sublot.Sublot;
import org.openlmis.stockmanagement.domain.sublot.SublotStockCardLineItem;

@Data
@Entity
@Table(name = "sublot_stock_cards", schema = "stockmanagement")
public class SublotStockCard extends BaseEntity {
    @OneToOne
    private Sublot sublot;
    @JoinColumn(nullable = false,  name = "stockcardid")
    @ManyToOne
    private StockCard stockCard;
    @Column(nullable = false)
    private UUID originEventId;
    @Transient
    private Integer sublotStockOnHand;
    // @LazyCollection(FALSE)
    @OneToMany(cascade = ALL, mappedBy = "sublotStockCard")
    private List<SublotStockCardLineItem> lineItems;

    public List<SublotStockCardLineItem> getLineItems() {
        if (lineItems == null) {
            lineItems = new ArrayList<>();
        }
        return lineItems;
    }

}
