package com.chemexpress.crm.vo;

import com.chemexpress.crm.entity.BusinessEvent;
import com.chemexpress.crm.entity.Order;
import com.chemexpress.crm.entity.Visit;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardVO {
    private List<Kpi> kpis;
    private List<String> months;
    private List<BigDecimal> salesTrend;
    private List<NameValue> customerTypes;
    private List<Visit> todayVisits;
    private List<Order> recentOrders;
    private List<BusinessEvent> recentEvents;

    @Data
    public static class Kpi {
        private String key;
        private String label;
        private String value;
        private String hint;

        public Kpi(String key, String label, String value, String hint) {
            this.key = key;
            this.label = label;
            this.value = value;
            this.hint = hint;
        }
    }

    @Data
    public static class NameValue {
        private String name;
        private long value;

        public NameValue(String name, long value) {
            this.name = name;
            this.value = value;
        }
    }
}
