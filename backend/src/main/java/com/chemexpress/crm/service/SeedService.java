package com.chemexpress.crm.service;

import com.chemexpress.crm.common.Constants;
import com.chemexpress.crm.entity.BusinessEvent;
import com.chemexpress.crm.entity.Contact;
import com.chemexpress.crm.entity.Customer;
import com.chemexpress.crm.entity.Order;
import com.chemexpress.crm.entity.OrderItem;
import com.chemexpress.crm.entity.Payment;
import com.chemexpress.crm.entity.Product;
import com.chemexpress.crm.entity.User;
import com.chemexpress.crm.entity.Visit;
import com.chemexpress.crm.mapper.BusinessEventMapper;
import com.chemexpress.crm.mapper.ContactMapper;
import com.chemexpress.crm.mapper.CustomerMapper;
import com.chemexpress.crm.mapper.OrderItemMapper;
import com.chemexpress.crm.mapper.OrderMapper;
import com.chemexpress.crm.mapper.PaymentMapper;
import com.chemexpress.crm.mapper.ProductMapper;
import com.chemexpress.crm.mapper.UserMapper;
import com.chemexpress.crm.mapper.VisitMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeedService {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyyMM");
    private static final String DEMO_PASSWORD = "Chem@123";

    private final UserMapper userMapper;
    private final CustomerMapper customerMapper;
    private final ContactMapper contactMapper;
    private final ProductMapper productMapper;
    private final VisitMapper visitMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentMapper paymentMapper;
    private final BusinessEventMapper eventMapper;
    private final PasswordEncoder passwordEncoder;

    private final Random random = new Random(20261118L);
    private final Map<String, Integer> visitSequence = new HashMap<>();
    private final Map<String, Integer> orderSequence = new HashMap<>();

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void ensureSeeded() {
        Long users = userMapper.selectCount(null);
        if (users != null && users > 0) {
            log.info("Demo data already present, users={}", users);
            return;
        }
        seedAll();
    }

    @Transactional
    public void seedAll() {
        visitSequence.clear();
        orderSequence.clear();
        random.setSeed(20261118L);
        LocalDate today = LocalDate.now();

        List<User> users = seedUsers();
        User admin = users.get(0);
        Map<String, User> userMap = new LinkedHashMap<>();
        users.forEach(u -> userMap.put(u.getUsername(), u));
        User dsmEast = userMap.get("dsm.east");
        User dsmNorth = userMap.get("dsm.north");
        User mrChen = userMap.get("mr.chen");
        User mrLiu = userMap.get("mr.liu");
        User mrZhao = userMap.get("mr.zhao");
        User mrSun = userMap.get("mr.sun");
        User ops = userMap.get("ops.zhou");
        List<User> reps = List.of(mrChen, mrLiu, mrZhao, mrSun);

        List<Product> products = seedProducts();
        List<Product> activeProducts = products.stream()
                .filter(p -> Constants.PRODUCT_ACTIVE.equals(p.getStatus())).toList();
        List<Customer> customers = seedCustomers(userMap);
        Map<Long, List<Contact>> contacts = seedContacts(customers);
        seedVisits(customers, contacts, products, activeProducts, reps, dsmEast, dsmNorth, today);
        seedOrders(customers, activeProducts, ops, dsmEast, dsmNorth, today);
        refreshLastVisitDates(customers);
        log.info("Demo data ready: users={}, customers={}, products={}", users.size(), customers.size(), products.size());
    }

    private List<User> seedUsers() {
        String password = passwordEncoder.encode(DEMO_PASSWORD);
        List<User> users = new ArrayList<>();
        users.add(user("admin", "谢震宇", Constants.ROLE_ADMIN, Constants.REGION_HQ, password));
        users.add(user("dsm.east", "李强", Constants.ROLE_DSM, Constants.REGION_EAST, password));
        users.add(user("dsm.north", "王敏", Constants.ROLE_DSM, Constants.REGION_NORTH, password));
        users.add(user("mr.chen", "陈晨", Constants.ROLE_MR, Constants.REGION_EAST, password));
        users.add(user("mr.liu", "刘洋", Constants.ROLE_MR, Constants.REGION_EAST, password));
        users.add(user("mr.zhao", "赵磊", Constants.ROLE_MR, Constants.REGION_NORTH, password));
        users.add(user("mr.sun", "孙悦", Constants.ROLE_MR, Constants.REGION_NORTH, password));
        users.add(user("ops.zhou", "周琳", Constants.ROLE_OPS, Constants.REGION_HQ, password));
        users.forEach(userMapper::insert);
        return users;
    }

    private User user(String username, String name, String role, String region, String password) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setName(name);
        user.setRole(role);
        user.setRegion(region);
        user.setPhone("1380000" + (1000 + random.nextInt(9000)));
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    private List<Product> seedProducts() {
        List<Product> products = new ArrayList<>();
        products.add(product("PRD-001", "皓欣平", "阿莫西林胶囊", "0.25g×24粒", "胶囊", "甲类", "28.50", "ACTIVE", "适用于敏感菌所致的呼吸道、泌尿道等感染。"));
        products.add(product("PRD-002", "皓宁舒", "头孢克肟分散片", "0.1g×12片", "片剂", "乙类", "46.80", "ACTIVE", "适用于敏感菌引起的支气管炎、肺炎等。"));
        products.add(product("PRD-003", "皓清咽", "蒲地蓝消炎口服液", "10ml×12支", "口服液", "乙类", "39.90", "ACTIVE", "清热解毒，消肿利咽。"));
        products.add(product("PRD-004", "皓稳压", "苯磺酸氨氯地平片", "5mg×28片", "片剂", "甲类", "32.60", "ACTIVE", "用于高血压、慢性稳定性心绞痛。"));
        products.add(product("PRD-005", "皓糖安", "二甲双胍缓释片", "0.5g×30片", "片剂", "甲类", "25.80", "ACTIVE", "用于 2 型糖尿病。"));
        products.add(product("PRD-006", "皓脂康", "阿托伐他汀钙片", "20mg×14片", "片剂", "乙类", "58.00", "ACTIVE", "用于高胆固醇血症。"));
        products.add(product("PRD-007", "皓舒胃", "奥美拉唑肠溶胶囊", "20mg×14粒", "胶囊", "甲类", "21.50", "ACTIVE", "用于胃溃疡、十二指肠溃疡。"));
        products.add(product("PRD-008", "皓咳宁", "盐酸氨溴索口服溶液", "100ml", "口服液", "乙类", "36.00", "ACTIVE", "用于痰液粘稠不易咳出。"));
        products.add(product("PRD-009", "皓敏清", "氯雷他定片", "10mg×12片", "片剂", "非医保", "18.80", "ACTIVE", "用于过敏性鼻炎、荨麻疹。"));
        products.add(product("PRD-010", "皓骨康", "碳酸钙D3颗粒", "3g×20袋", "颗粒剂", "非医保", "42.00", "ACTIVE", "用于钙补充与骨质疏松辅助治疗。"));
        products.add(product("PRD-011", "皓维片", "维生素C片", "0.1g×100片", "片剂", "非医保", "9.90", "INACTIVE", "演示停售产品。"));
        products.add(product("PRD-012", "皓旧宁", "复方甘草片", "100片", "片剂", "乙类", "12.50", "INACTIVE", "演示停售产品。"));
        products.forEach(productMapper::insert);
        return products;
    }

    private Product product(String code, String tradeName, String genericName, String spec, String form,
                            String insurance, String price, String status, String indication) {
        Product product = new Product();
        product.setCode(code);
        product.setTradeName(tradeName);
        product.setGenericName(genericName);
        product.setSpec(spec);
        product.setDosageForm(form);
        product.setInsuranceType(insurance);
        product.setPrice(new BigDecimal(price));
        product.setStatus(status);
        product.setIndication(indication);
        return product;
    }

    private List<Customer> seedCustomers(Map<String, User> users) {
        List<String> eastHospitals = List.of("上海示范第一医院", "上海康宁医院", "杭州仁和医院",
                "南京金陵医院", "苏州工业园区医院", "宁波海曙医院");
        List<String> northHospitals = List.of("北京朝阳示范医院", "北京海淀康泰医院", "天津滨海医院",
                "石家庄太行医院", "济南泉城医院", "青岛海慈医院");
        List<String> eastChains = List.of("上海益民连锁药房", "杭州百姓康连锁药房", "南京安康连锁药房");
        List<String> northChains = List.of("北京康惠连锁药房", "天津惠民连锁药房");
        List<String> eastPharmacies = List.of("上海静安仁心药店", "苏州姑苏百草堂药店");
        List<String> northPharmacies = List.of("北京西城德善药店", "天津和平济世药店", "石家庄长安惠民药店");
        List<String> eastDistributors = List.of("上海华康医药商业", "杭州钱塘医药商业", "南京长江医药商业");
        List<String> northDistributors = List.of("北京京北医药商业", "天津渤海医药商业", "石家庄燕赵医药商业");

        List<Customer> customers = new ArrayList<>();
        int index = 1;
        index = addCustomers(customers, eastHospitals, Constants.CUSTOMER_TYPE_HOSPITAL, Constants.REGION_EAST,
                List.of(users.get("mr.chen"), users.get("mr.liu")), index);
        index = addCustomers(customers, northHospitals, Constants.CUSTOMER_TYPE_HOSPITAL, Constants.REGION_NORTH,
                List.of(users.get("mr.zhao"), users.get("mr.sun")), index);
        index = addCustomers(customers, eastChains, Constants.CUSTOMER_TYPE_CHAIN, Constants.REGION_EAST,
                List.of(users.get("mr.chen"), users.get("mr.liu")), index);
        index = addCustomers(customers, northChains, Constants.CUSTOMER_TYPE_CHAIN, Constants.REGION_NORTH,
                List.of(users.get("mr.zhao"), users.get("mr.sun")), index);
        index = addCustomers(customers, eastPharmacies, Constants.CUSTOMER_TYPE_PHARMACY, Constants.REGION_EAST,
                List.of(users.get("mr.chen"), users.get("mr.liu")), index);
        index = addCustomers(customers, northPharmacies, Constants.CUSTOMER_TYPE_PHARMACY, Constants.REGION_NORTH,
                List.of(users.get("mr.zhao"), users.get("mr.sun")), index);
        index = addCustomers(customers, eastDistributors, Constants.CUSTOMER_TYPE_DISTRIBUTOR, Constants.REGION_EAST,
                List.of(users.get("mr.chen"), users.get("mr.liu")), index);
        addCustomers(customers, northDistributors, Constants.CUSTOMER_TYPE_DISTRIBUTOR, Constants.REGION_NORTH,
                List.of(users.get("mr.zhao"), users.get("mr.sun")), index);
        return customers;
    }

    private int addCustomers(List<Customer> customers, List<String> names, String type, String region,
                             List<User> owners, int startIndex) {
        String[] sources = {"陌拜", "转介绍", "市场活动", "商业推荐"};
        for (int i = 0; i < names.size(); i++) {
            Customer customer = new Customer();
            int index = startIndex + i;
            customer.setCode("CUS-%04d".formatted(index));
            customer.setName(names.get(i));
            customer.setType(type);
            String level = index % 3 == 1 ? "A" : (index % 3 == 2 ? "B" : "C");
            customer.setLevel(level);
            customer.setRegion(region);
            customer.setOwnerId(owners.get(i % owners.size()).getId());
            String status;
            if (index == 9 || index == 22) {
                status = Constants.CUSTOMER_STATUS_CHURNED;
            } else if (index == 13 || index == 26) {
                status = Constants.CUSTOMER_STATUS_PAUSED;
            } else if (index == 5 || index == 17) {
                status = Constants.CUSTOMER_STATUS_POTENTIAL;
            } else {
                status = Constants.CUSTOMER_STATUS_ACTIVE;
            }
            customer.setStatus(status);
            customer.setAddress(region + "示范大道 " + (100 + index * 7) + " 号");
            customer.setAnnualPotential("A".equals(level)
                    ? BigDecimal.valueOf(3_000_000L + random.nextInt(8_000_000))
                    : BigDecimal.valueOf(300_000L + random.nextInt(2_000_000)));
            if (index % 4 == 0) {
                customer.setTags("重点开发,集采品种");
            } else if (index % 5 == 0) {
                customer.setTags("学术合作");
            }
            customer.setSource(sources[index % sources.length]);
            customer.setRemark("演示客户，用于产品自测");
            customer.setVersion(0);
            customer.setCreatedAt(LocalDateTime.now().minusDays(30L + index));
            customer.setUpdatedAt(LocalDateTime.now().minusDays(index % 10));
            customerMapper.insert(customer);
            customers.add(customer);
        }
        return startIndex + names.size();
    }

    private Map<Long, List<Contact>> seedContacts(List<Customer> customers) {
        String[] surnames = {"王", "李", "张", "刘", "陈", "杨", "赵", "黄", "周", "吴", "徐", "孙"};
        String[] given = {"主任", "医生", "采购", "经理", "店长", "药师"};
        String[] roles = {"DECISION", "PURCHASE", "PHARMACY", "STORE", "OTHER"};
        Map<Long, List<Contact>> result = new HashMap<>();
        for (int i = 0; i < customers.size(); i++) {
            Customer customer = customers.get(i);
            int count = i < 18 ? 2 : 1;
            List<Contact> list = new ArrayList<>();
            for (int j = 0; j < count; j++) {
                Contact contact = new Contact();
                contact.setCustomerId(customer.getId());
                contact.setName(surnames[(i + j) % surnames.length] + given[(i + j) % given.length]);
                contact.setTitle(j == 0 ? "药剂科主任" : "采购经理");
                contact.setRole(roles[(i + j) % roles.length]);
                contact.setPhone("139%08d".formatted(10000000 + i * 10 + j));
                contact.setEmail("contact" + (i * 2 + j + 1) + "@demo-chemexpress.cn");
                contact.setPrimaryContact(j == 0);
                contact.setStatus("ACTIVE");
                contact.setRemark("演示联系人");
                contact.setCreatedAt(LocalDateTime.now().minusDays(20));
                contact.setUpdatedAt(LocalDateTime.now().minusDays(5));
                contactMapper.insert(contact);
                list.add(contact);
            }
            result.put(customer.getId(), list);
        }
        return result;
    }

    private void seedVisits(List<Customer> customers, Map<Long, List<Contact>> contacts, List<Product> products,
                            List<Product> activeProducts, List<User> reps, User dsmEast, User dsmNorth, LocalDate today) {
        Map<String, User> dsmByRegion = Map.of(Constants.REGION_EAST, dsmEast, Constants.REGION_NORTH, dsmNorth);
        List<Customer> visitable = customers.stream()
                .filter(c -> !Constants.CUSTOMER_STATUS_CHURNED.equals(c.getStatus()))
                .filter(c -> Constants.CUSTOMER_STATUS_ACTIVE.equals(c.getStatus()))
                .toList();
        Set<String> used = new HashSet<>();
        int[] cursor = {0};

        for (User rep : reps) {
            Customer customer = visitable.stream()
                    .filter(c -> c.getOwnerId().equals(rep.getId())).findFirst().orElse(visitable.get(0));
            planVisit(customer, contacts, rep, dsmByRegion, today, Constants.VISIT_PLANNED, used, activeProducts);
        }
        for (User rep : reps) {
            for (int i = 1; i <= 2; i++) {
                Customer customer = visitable.stream()
                        .filter(c -> c.getOwnerId().equals(rep.getId())).skip(i).findFirst().orElse(visitable.get(i % visitable.size()));
                planVisit(customer, contacts, rep, dsmByRegion, today, Constants.VISIT_CHECKED_IN, used, activeProducts);
            }
        }
        for (int i = 0; i < 10; i++) {
            User rep = reps.get(i % reps.size());
            Customer customer = ownedCustomer(visitable, rep, cursor);
            LocalDate date = today.minusDays(random.nextInt(4));
            submittedVisit(customer, contacts, rep, dsmByRegion, date, Constants.VISIT_SUBMITTED, null, used, activeProducts);
        }
        for (int i = 0; i < 6; i++) {
            User rep = reps.get(i % reps.size());
            Customer customer = ownedCustomer(visitable, rep, cursor);
            LocalDate date = today.minusDays(5 + random.nextInt(16));
            submittedVisit(customer, contacts, rep, dsmByRegion, date, Constants.VISIT_REJECTED,
                    "客户反馈记录不够具体，请补充下一步跟进计划。", used, activeProducts);
        }
        for (int i = 0; i < 14; i++) {
            User rep = reps.get(i % reps.size());
            Customer customer = ownedCustomer(visitable, rep, cursor);
            LocalDate date = today.plusDays(1 + random.nextInt(10));
            planVisit(customer, contacts, rep, dsmByRegion, date, Constants.VISIT_PLANNED, used, activeProducts);
        }
        for (int i = 0; i < 78; i++) {
            User rep = reps.get(i % reps.size());
            Customer customer = ownedCustomer(visitable, rep, cursor);
            LocalDate date = today.minusDays(1 + random.nextInt(170));
            submittedVisit(customer, contacts, rep, dsmByRegion, date, Constants.VISIT_APPROVED, null, used, activeProducts);
        }
    }

    private Customer ownedCustomer(List<Customer> customers, User rep, int[] cursor) {
        List<Customer> owned = customers.stream().filter(c -> c.getOwnerId().equals(rep.getId())).toList();
        if (owned.isEmpty()) {
            return customers.get(cursor[0]++ % customers.size());
        }
        return owned.get(cursor[0]++ % owned.size());
    }

    private Visit planVisit(Customer customer, Map<Long, List<Contact>> contacts, User rep,
                            Map<String, User> dsmByRegion, LocalDate date, String status, Set<String> used,
                            List<Product> products) {
        Visit visit = baseVisit(customer, contacts, rep, date, status, used);
        if (Constants.VISIT_CHECKED_IN.equals(status)) {
            visit.setCheckinTime(date.atTime(9, 30));
            visit.setCheckinDistance(60 + random.nextInt(120));
        }
        visitMapper.insert(visit);
        event(visit, rep, "PLAN", "创建拜访计划");
        if (Constants.VISIT_CHECKED_IN.equals(status)) {
            event(visit, rep, "CHECKIN", "完成签到（演示定位）");
        }
        return visit;
    }

    private Visit submittedVisit(Customer customer, Map<Long, List<Contact>> contacts, User rep,
                                 Map<String, User> dsmByRegion, LocalDate date, String status, String rejectReason,
                                 Set<String> used, List<Product> products) {
        Visit visit = baseVisit(customer, contacts, rep, date, status, used);
        visit.setResult(List.of("SUCCESS", "PARTIAL", "FAILED").get(random.nextInt(3)));
        visit.setFeedback("本次拜访与客户沟通了产品备货与临床使用情况，客户对产品疗效表示认可，并提出了下阶段上量建议。");
        visit.setNextStep("两周内回访，确认采购计划并补充产品资料。");
        visit.setCheckinTime(date.atTime(9, 20 + random.nextInt(40)));
        visit.setCheckinDistance(50 + random.nextInt(160));
        if (Constants.VISIT_REJECTED.equals(status)) {
            visit.setRejectReason(rejectReason);
        }
        User dsm = dsmByRegion.get(customer.getRegion());
        if (Constants.VISIT_APPROVED.equals(status) || Constants.VISIT_REJECTED.equals(status)) {
            visit.setApproverId(dsm.getId());
            visit.setApprovedAt(date.plusDays(1).atTime(10, 30));
        }
        visitMapper.insert(visit);
        event(visit, rep, "PLAN", "创建拜访计划");
        event(visit, rep, "SUBMIT", "提交拜访记录");
        if (Constants.VISIT_APPROVED.equals(status)) {
            event(visit, dsm, "APPROVE", "审批通过拜访");
        } else if (Constants.VISIT_REJECTED.equals(status)) {
            event(visit, dsm, "REJECT", "驳回拜访：" + rejectReason);
        }
        int productCount = 2 + random.nextInt(2);
        Set<Long> chosen = new HashSet<>();
        for (int i = 0; i < productCount; i++) {
            Product product = products.get(random.nextInt(products.size()));
            if (chosen.add(product.getId())) {
                visitMapper.insertVisitProduct(visit.getId(), product.getId());
            }
        }
        return visit;
    }

    private Visit baseVisit(Customer customer, Map<Long, List<Contact>> contacts, User rep,
                            LocalDate date, String status, Set<String> used) {
        String key = customer.getId() + "|" + date;
        int guard = 0;
        while (used.contains(key) && guard++ < 30) {
            date = date.minusDays(1);
            key = customer.getId() + "|" + date;
        }
        used.add(key);
        Visit visit = new Visit();
        String month = date.format(MONTH);
        int seq = visitSequence.merge(month, 1, Integer::sum);
        visit.setCode("VIS-" + month + "-" + "%04d".formatted(seq));
        visit.setCustomerId(customer.getId());
        List<Contact> customerContacts = contacts.get(customer.getId());
        if (customerContacts != null && !customerContacts.isEmpty()) {
            visit.setContactId(customerContacts.get(random.nextInt(customerContacts.size())).getId());
        }
        visit.setRepId(rep.getId());
        visit.setRegion(customer.getRegion());
        visit.setVisitDate(date);
        List<String> purposes = new ArrayList<>(Constants.VISIT_PURPOSES.keySet());
        visit.setPurpose(purposes.get(random.nextInt(purposes.size())));
        visit.setStatus(status);
        visit.setVersion(0);
        visit.setCreatedAt(date.minusDays(2).atTime(9, 0));
        visit.setUpdatedAt(date.atTime(16, 0));
        return visit;
    }

    private void seedOrders(List<Customer> customers, List<Product> products, User ops,
                            User dsmEast, User dsmNorth, LocalDate today) {
        Map<String, User> dsmByRegion = Map.of(Constants.REGION_EAST, dsmEast, Constants.REGION_NORTH, dsmNorth);
        List<Customer> activeCustomers = customers.stream()
                .filter(c -> Constants.CUSTOMER_STATUS_ACTIVE.equals(c.getStatus())).toList();
        List<String> statuses = new ArrayList<>();
        statuses.add(Constants.ORDER_PENDING);
        statuses.add(Constants.ORDER_PENDING);
        statuses.add(Constants.ORDER_APPROVED);
        statuses.add(Constants.ORDER_APPROVED);
        statuses.add(Constants.ORDER_APPROVED);
        statuses.add(Constants.ORDER_APPROVED);
        statuses.add(Constants.ORDER_SHIPPED);
        statuses.add(Constants.ORDER_SHIPPED);
        statuses.add(Constants.ORDER_DRAFT);
        statuses.add(Constants.ORDER_REJECTED);
        statuses.add(Constants.ORDER_CANCELLED);
        for (int i = 0; i < 30; i++) {
            statuses.add(Constants.ORDER_COMPLETED);
        }
        for (int i = 0; i < 4; i++) {
            statuses.add(Constants.ORDER_SHIPPED);
        }
        for (int i = 0; i < 2; i++) {
            statuses.add(Constants.ORDER_DRAFT);
        }
        statuses.add(Constants.ORDER_REJECTED);

        for (int i = 0; i < statuses.size(); i++) {
            String status = statuses.get(i);
            Customer customer;
            if (i == 0) {
                customer = activeCustomers.stream().filter(c -> Constants.REGION_EAST.equals(c.getRegion())).findFirst().orElse(activeCustomers.get(0));
            } else if (i == 1) {
                customer = activeCustomers.stream().filter(c -> Constants.REGION_NORTH.equals(c.getRegion())).findFirst().orElse(activeCustomers.get(1));
            } else {
                customer = activeCustomers.get(random.nextInt(activeCustomers.size()));
            }
            LocalDate monthStart = today.withDayOfMonth(1).minusMonths(i % 6);
            LocalDate createdDate = monthStart.plusDays(random.nextInt(20));
            if (createdDate.isAfter(today)) {
                createdDate = today.minusDays(random.nextInt(4));
            }
            Order order = new Order();
            String month = createdDate.format(MONTH);
            int seq = orderSequence.merge(month, 1, Integer::sum);
            order.setCode("SO-" + month + "-" + "%04d".formatted(seq));
            order.setCustomerId(customer.getId());
            order.setRepId(customer.getOwnerId());
            order.setRegion(customer.getRegion());
            order.setStatus(status);
            order.setReceiverAddress(customer.getAddress());
            order.setExpectedDate(createdDate.plusDays(7));
            order.setRemark("演示订单");
            order.setVersion(0);
            order.setCreatedAt(createdDate.atTime(10, 0));
            order.setUpdatedAt(createdDate.atTime(18, 0));
            if (Constants.ORDER_REJECTED.equals(status)) {
                order.setRejectReason("折扣超出审批权限，请调整后重新提交。");
            }
            if (Constants.ORDER_CANCELLED.equals(status)) {
                order.setCancelReason("客户临时调整采购计划。");
            }
            if (List.of(Constants.ORDER_APPROVED, Constants.ORDER_SHIPPED, Constants.ORDER_COMPLETED).contains(status)) {
                order.setApproverId(dsmByRegion.get(customer.getRegion()).getId());
                order.setApprovedAt(createdDate.plusDays(1).atTime(11, 0));
            }
            if (Constants.ORDER_SHIPPED.equals(status) || Constants.ORDER_COMPLETED.equals(status)) {
                LocalDate shipDate = createdDate.plusDays(1 + random.nextInt(4));
                if (shipDate.isAfter(today)) {
                    shipDate = today.minusDays(random.nextInt(2));
                }
                order.setLogisticsCompany(List.of("顺丰医药物流", "京东医药物流", "德邦医药配送").get(random.nextInt(3)));
                order.setTrackingNo("SF" + (100000000 + random.nextInt(899999999)));
                order.setShippedAt(shipDate.atTime(15, 0));
            }
            if (Constants.ORDER_COMPLETED.equals(status)) {
                order.setCompletedAt(createdDate.plusDays(8).atTime(16, 0));
            }
            List<OrderItem> items = buildSeedItems(products);
            BigDecimal total = items.stream().map(OrderItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            order.setTotalAmount(total);
            orderMapper.insert(order);
            for (OrderItem item : items) {
                item.setOrderId(order.getId());
                orderItemMapper.insert(item);
            }

            event(order, IGNORE_USER, "CREATE", "创建订单");
            if (!Constants.ORDER_DRAFT.equals(status)) {
                event(order, IGNORE_USER, "SUBMIT", "提交订单");
            }
            if (List.of(Constants.ORDER_APPROVED, Constants.ORDER_SHIPPED, Constants.ORDER_COMPLETED).contains(status)) {
                event(order, dsmByRegion.get(customer.getRegion()), "APPROVE", "审批通过订单");
            }
            if (Constants.ORDER_SHIPPED.equals(status) || Constants.ORDER_COMPLETED.equals(status)) {
                event(order, ops, "SHIP", "发货：" + order.getLogisticsCompany() + " / " + order.getTrackingNo());
            }
            if (Constants.ORDER_REJECTED.equals(status)) {
                event(order, dsmByRegion.get(customer.getRegion()), "REJECT", "驳回订单：" + order.getRejectReason());
            }
            if (Constants.ORDER_CANCELLED.equals(status)) {
                event(order, ops, "CANCEL", "取消订单：" + order.getCancelReason());
            }
            if (Constants.ORDER_COMPLETED.equals(status)) {
                addSeedPayments(order, total, ops, true);
                event(order, ops, "COMPLETE", "订单回款完成，自动结案");
            } else if (Constants.ORDER_SHIPPED.equals(status) && random.nextBoolean()) {
                BigDecimal partial = total.multiply(BigDecimal.valueOf(0.3 + random.nextInt(3) / 10.0))
                        .setScale(2, RoundingMode.HALF_UP);
                insertPayment(order, partial, ops);
                event(order, ops, "PAYMENT", "登记回款 " + partial + " 元");
            }
        }
    }

    private final User IGNORE_USER = null;

    private List<OrderItem> buildSeedItems(List<Product> products) {
        List<OrderItem> items = new ArrayList<>();
        int count = 1 + random.nextInt(3);
        Set<Long> used = new HashSet<>();
        for (int i = 0; i < count; i++) {
            Product product = products.get(random.nextInt(products.size()));
            if (!used.add(product.getId())) {
                continue;
            }
            int quantity = 10 + random.nextInt(190);
            BigDecimal discount = BigDecimal.valueOf(List.of(0, 5, 10, 15).get(random.nextInt(4)));
            BigDecimal gross = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            BigDecimal amount = gross.multiply(BigDecimal.ONE.subtract(discount.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
                    .setScale(2, RoundingMode.HALF_UP);
            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getTradeName());
            item.setProductSpec(product.getSpec());
            item.setUnitPrice(product.getPrice());
            item.setQuantity(quantity);
            item.setDiscount(discount);
            item.setAmount(amount);
            items.add(item);
        }
        return items;
    }

    private void addSeedPayments(Order order, BigDecimal total, User ops, boolean full) {
        if (!full) {
            return;
        }
        if (random.nextBoolean()) {
            insertPayment(order, total, ops);
        } else {
            BigDecimal first = total.multiply(BigDecimal.valueOf(0.4)).setScale(2, RoundingMode.HALF_UP);
            insertPayment(order, first, ops);
            insertPayment(order, total.subtract(first), ops);
        }
    }

    private void insertPayment(Order order, BigDecimal amount, User ops) {
        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        LocalDate payDate = order.getShippedAt() == null ? LocalDate.now() : order.getShippedAt().toLocalDate();
        if (payDate.isAfter(LocalDate.now())) {
            payDate = LocalDate.now();
        }
        payment.setPayDate(payDate);
        payment.setAmount(amount);
        payment.setMethod("TRANSFER");
        payment.setReferenceNo("PAY" + (10000000 + random.nextInt(89999999)));
        payment.setRemark("演示回款");
        payment.setCreatedBy(ops.getId());
        payment.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(20)));
        paymentMapper.insert(payment);
    }

    private void refreshLastVisitDates(List<Customer> customers) {
        for (Customer customer : customers) {
            List<Visit> approved = visitMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Visit>()
                    .eq(Visit::getCustomerId, customer.getId())
                    .eq(Visit::getStatus, Constants.VISIT_APPROVED)
                    .orderByDesc(Visit::getVisitDate)
                    .last("LIMIT 1"));
            if (!approved.isEmpty()) {
                customer.setLastVisitDate(approved.get(0).getVisitDate());
                customerMapper.updateById(customer);
            }
        }
    }

    private void event(String bizType, Long bizId, String action, String detail, User operator) {
        BusinessEvent event = new BusinessEvent();
        event.setBizType(bizType);
        event.setBizId(bizId);
        event.setAction(action);
        event.setDetail(detail);
        if (operator != null) {
            event.setOperatorId(operator.getId());
            event.setOperatorName(operator.getName());
        } else {
            event.setOperatorId(1L);
            event.setOperatorName("系统");
        }
        event.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(60)));
        eventMapper.insert(event);
    }

    private void event(Visit visit, User operator, String action, String detail) {
        event("VISIT", visit.getId(), action, detail + " " + visit.getCode(), operator);
    }

    private void event(Order order, User operator, String action, String detail) {
        event("ORDER", order.getId(), action, detail + " " + order.getCode(), operator);
    }
}
