package com.superstore.product.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.superstore.product.entity.ProductEntity;
import com.superstore.product.mapper.ProductMapper;
import com.superstore.product.pojo.OrderPojo;
import com.superstore.product.pojo.ProductPojo;
import com.superstore.product.repository.ProductRepository;

import tools.jackson.databind.ObjectMapper;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
    
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final VectorStore vectorStore;
    private static final int BATCH_SIZE = 100;

    // OPTION 1 : ApplicationContext
    // proxy is fetched every method call.
    // private final ApplicationContext applicationContext; // inject context
    
    // OPTION 2.1 : @Lazy
    // once at startup.
    // it uses Field Injection, which makes unit testing harder because we cannot easily mock or pass the proxy without relying on reflection tools (like Mockito or Spring JUnit runners).
    // it also prevents the field from being final.
    // @Lazy // CRITICAL - prevents circular dependency error at startup.
    // @Autowired
    // private ProductService self; // proxied version of itself.

    // OPTION 2.2 : setter
    // it leaves our class mutable, the self field cannot be final, and it creates a brief window during bean lifecycle where self is null before the setter is invoked.
    // it is generally considered an outdated style unless a dependency is truly optional (which this isn't, as our search relies on it).
    // @Autowired
    // public void setSelf(ProductService self) {
    //     this.self = self;
    // }
    
    // OPTION 3 (RECOMMENDED) : constructor injection.
    // it allows to mark the self field as final.
    // immutable fields mean our class is thread-safe, easier to test, and guaranteed to be properly initialized once the constructor finishes.
    // it explicitly declares its self-proxy dependency upfront rather than hiding it in a reflection-based field or setter.
    private final ProductService self;

    // OPTION 4 : don't Self-Inject at all.
    // cleanest architectural design is to split the class into two distinct services.
    // ProductCacheService : contains only the @Cacheable getAllProduct() method.
    // ProductService : injects ProductCacheService normally via the constructor (no @Lazy needed, no circular dependencies) and handles the searchProduct filtering logic.

    public ProductService(
            ProductRepository productRepository, 
            ProductMapper productMapper,
            @Lazy ProductService self,
            VectorStore vectorStore) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.self = self;
        this.vectorStore = vectorStore;
    }

    @KafkaListener(topics = "topic-order-created", groupId = "group-product")
    public void updateStock(String order)
	{
		ObjectMapper mapper = new ObjectMapper();
	    OrderPojo orderPojo = mapper.readValue(order, OrderPojo.class);
	    System.out.println("stock update for order # [" + orderPojo.getOrdHdr().getOrdNo() + "] from topic (topic-order-created) to group (group-product) successfully!");
    }

    @CacheEvict(value = "product", allEntries = true)
    public void saveProduct(ProductPojo productPojo) {
        productRepository.save(productMapper.pojoToEntity(productPojo));
    }

    @Transactional
    @CacheEvict(value = "product", allEntries = true)
    public void deleteProduct(String company, String sku) {
        productRepository.deleteByIdCompanyAndIdSku(company, sku);
    }

    @Transactional
    @CacheEvict(value = "product", allEntries = true)
    public void deleteMultiProduct(Set<ProductPojo> multiProduct) {
        List<ProductEntity> listProductEntity = productMapper.pojoListToEntityList(multiProduct);
        productRepository.deleteAllInBatch(listProductEntity); // Set<ProductEntity> is Iterable, works fine.
    }

    // @SuppressWarnings("unchecked")
    // key is hardcoded 'all', so each time it filters and returns only 'searchText' results to Redis.
    // @Cacheable(value = "product", key = "'all'")
    public List<ProductPojo> searchProduct(String searchText) {
        
        // Cache cache = cacheManager.getCache("product");
        // List<ProductPojo> allProduct = null;
        
        // calling getAllProduct() from searchProduct() in SAME class and @Cacheable gets BYPASSED here.
        // List<ProductPojo> allProduct = getAllProduct();
        // call via self proxy -> @Cacheable works correctly.
        // ProductService self = applicationContext.getBean(ProductService.class);
        List<ProductPojo> allProduct = self.getAllProduct();
        logger.info("filtering from getAllProduct for searchText : [{}]", searchText);

        // cache.get("all", List.class) tells Spring "give me a List".
        // but it doesn't know it's a List<ProductPojo>.
        // so Redis deserializes it as List<LinkedHashMap> instead of List<ProductPojo>.
        // if (cache != null) {
        //     allProduct = cache.get("all", List.class);
        // }

        // getAllProduct()
        // → @Cacheable intercepts AFTER method returns List<ProductPojo>
        // → Spring knows the full generic type → stores + retrieves correctly ✅

        // searchProduct()
        // → cache.get("all", List.class)  ← raw List, no generic type info
        // → Redis deserializes elements as LinkedHashMap ❌

        // if (allProduct == null) {
        //     logger.info("CACHE MISS, fetching from DB.");
        //     // if getAllProduct() was never called first, this will call on each time we search.
        //     // List<ProductEntity> listProductEntity = productRepository.findAllByOrderByIdCompanyAscIdSkuAsc();
        //     // allProduct = productMapper.entityListToPojoList(listProductEntity);
        //     // FIX, so this populates "product::all" in Redis too.
        //     allProduct = getAllProduct();
        // } else {
        //     logger.info("CACHE HIT, filtering from Redis.");
        // }

        String search = searchText.toLowerCase().trim();
        return allProduct.stream()
            .filter(p -> 
                (p.getCompany()     != null && p.getCompany().toLowerCase().contains(search))       ||                
                (p.getName()        != null && p.getName().toLowerCase().contains(search))          ||
                (p.getCategory()    != null && p.getCategory().toLowerCase().contains(search))      ||
                (p.getBarcode()     != null && p.getBarcode().toLowerCase().contains(search))
                // (p.getSku()         != null && p.getSku().toLowerCase().contains(search))           ||
                // (p.getDepartment()  != null && p.getDepartment().toLowerCase().contains(search))    ||
            )
            .collect(Collectors.toList());
    }

    @Cacheable(value = "product", key = "'all'")
    public List<ProductPojo> getAllProduct() {
        
        //METHOD 1
        // List<ProductPojo> listProductPojo = new ArrayList<>();
        // List<ProductEntity> listProductEntity = productRepository.findAllByOrderByIdCompanyAscIdSkuAsc();
        // for (ProductEntity eachProductEntity : listProductEntity) {
        //     ProductPojo eachProductPojo = new ProductPojo();
        //     eachProductPojo.setCompany(eachProductEntity.getId().getCompany());
        //     eachProductPojo.setSku(eachProductEntity.getId().getSku());
        //     eachProductPojo.setSkuDesc(eachProductEntity.getSkuDesc());
        //     eachProductPojo.setMrp(eachProductEntity.getMrp());
        //     eachProductPojo.setPkdDate(eachProductEntity.getPkdDate());
        //     eachProductPojo.setExpDate(eachProductEntity.getExpDate());
        //     listProductPojo.add(eachProductPojo);
        // }
        // return listProductPojo;

        //METHOD 2
        // return productRepository.findAllByOrderByIdCompanyAscIdSkuAsc().stream()
        //     .map(this::convertToPojo)
        //     .collect(Collectors.toList());

        //METHOD 3
        logger.info("NO CACHE, fetching from DB.");
        List<ProductEntity> listProductEntity = productRepository.findAllByOrderByIdCompanyAscIdSkuAsc();
        simulateSlowService();
        return productMapper.entityListToPojoList(listProductEntity);
    }

    public ProductPojo getProductDetail(String company, String sku) {                        
        ProductEntity productEntity = productRepository.findByIdCompanyAndIdSku(company, sku);
        return productMapper.entityToPojo(productEntity);
    }

    private void simulateSlowService() {
        try { Thread.sleep(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // private ProductPojo convertToPojo(ProductEntity entity) {
    //     ProductPojo pojo = new ProductPojo();
    //     pojo.setCompany(entity.getId().getCompany());
    //     pojo.setSku(entity.getId().getSku());
    //     pojo.setSkuDesc(entity.getSkuDesc());
    //     pojo.setMrp(entity.getMrp());
    //     pojo.setPkdDate(entity.getPkdDate());
    //     pojo.setExpDate(entity.getExpDate());
    //     pojo.setActiveFlag(entity.getActiveFlag());
    //     return pojo;
    // }

    // reads all product from DB in pages and loads them into PGVector.
    @Transactional(readOnly = false)
    public void loadAllProductsToVectorStore() {
        logger.info("PRODUCT : vector store initialization START!");

        int pageNumber = 0;
        Page<ProductEntity> productPage;

        do {
            productPage = productRepository.findAll(PageRequest.of(pageNumber, BATCH_SIZE));

            List<Document> documents = productPage.getContent().stream()
                // optional : skip inactive products
                .filter(product -> Boolean.TRUE.equals(product.getActive()))
                .map(this::toVectorDocument)
                .collect(Collectors.toList());

            if (!documents.isEmpty()) {
                vectorStore.add(documents);
                logger.info("PRODUCT : batch of {} product(s) ingested into Vector Store.", documents.size());
            }

            pageNumber++;
        } while (productPage.hasNext());

        logger.info("PRODUCT : vector store initialization END!");
    }

    private Document toVectorDocument(ProductEntity product) {
        String company = (product.getId() != null && product.getId().getCompany() != null) ? product.getId().getCompany() : "";
        String sku = (product.getId() != null && product.getId().getSku() != null) ? product.getId().getSku() : "";
        
        // generates a consistent, valid UUID from any String key
        // String documentId = product.getId() != null ? UUID.nameUUIDFromBytes(product.getId().toString().getBytes(StandardCharsets.UTF_8)).toString() : UUID.randomUUID().toString();
        
        // generates our own String key
        String documentId = product.getId() != null ? product.getId().toString() : UUID.randomUUID().toString();

        // 1. unstructured text representation for semantic searching.
        String textContent = String.format(
            "Company: %s. SKU: %s. Product Name: %s. " +
            "Department: %s. Category: %s. " +
            "UPC: %s. Barcode: %s. " +
            "MRP: %s. Cost Price: %s. Sell Price: %s. " +
            "Stock: %s. Threshold: %s. " +
            "HSN Code: %s. Tax Rate: %s. Cess Rate: %s. " +
            "Tax Inclusive: %s. Tax Exempted: %s. " +
            "PKD Date: %s. EXP Date: %s. Active: %s. ",
            company, sku, product.getName() != null ? product.getName() : "",
            product.getDepartment() != null ? product.getDepartment() : "N/A", product.getCategory() != null ? product.getCategory() : "N/A",
            product.getUpc() != null ? product.getUpc() : "N/A", product.getBarcode() != null ? product.getBarcode() : "N/A",
            product.getMrp() != null ? product.getMrp() : BigDecimal.ZERO, product.getSellPrice() != null ? product.getSellPrice() : BigDecimal.ZERO, product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO,
            product.getStock() != null ? product.getStock() : 0L, product.getThreshold() != null ? product.getThreshold() : 0L,
            product.getHsnCode() != null ? product.getHsnCode() : "N/A", product.getTaxRate() != null ? product.getTaxRate() : BigDecimal.ZERO, product.getCessRate() != null ? product.getCessRate() : BigDecimal.ZERO,
            Boolean.TRUE.equals(product.getTaxInclusive()), Boolean.TRUE.equals(product.getTaxExempted()),
            product.getPkdDate(), product.getExpDate(), Boolean.TRUE.equals(product.getActive())
        );

        // 2. metadata fields for filtered searches.
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("company", company);
        metadata.put("sku", sku);
        metadata.put("name", product.getName() != null ? product.getName() : "N/A");
        metadata.put("department", product.getDepartment() != null ? product.getDepartment() : "N/A");
        metadata.put("category", product.getCategory() != null ? product.getCategory() : "N/A");
        metadata.put("upc", product.getUpc() != null ? product.getUpc() : "N/A");
        metadata.put("barcode", product.getBarcode() != null ? product.getBarcode() : "N/A");
        metadata.put("mrp", product.getMrp() != null ? product.getMrp().doubleValue() : 0.0);        
        metadata.put("cost_price", product.getCostPrice() != null ? product.getCostPrice().doubleValue() : 0.0);
        metadata.put("sell_price", product.getSellPrice() != null ? product.getSellPrice().doubleValue() : 0.0);
        metadata.put("stock", product.getStock() != null ? product.getStock() : 0L);
        metadata.put("threshold", product.getThreshold() != null ? product.getThreshold() : 0L);
        metadata.put("hsn_code", product.getHsnCode() != null ? product.getHsnCode() : "N/A");
        metadata.put("tax_rate", product.getTaxRate() != null ? product.getTaxRate().doubleValue() : 0.0);
        metadata.put("cess_rate", product.getCessRate() != null ? product.getCessRate().doubleValue() : 0.0);
        metadata.put("tax_inclusive", Boolean.TRUE.equals(product.getTaxInclusive()));
        metadata.put("tax_exempted", Boolean.TRUE.equals(product.getTaxExempted()));
        metadata.put("pkd_date", product.getPkdDate() != null ? product.getPkdDate() : "N/A");
        metadata.put("exp_date", product.getExpDate() != null ? product.getExpDate() : "N/A");
        metadata.put("active", Boolean.TRUE.equals(product.getActive()));
        metadata.put("in_stock", product.getStock() != null && product.getStock() > 0);

        return new Document(documentId, textContent, metadata);
    }
}
