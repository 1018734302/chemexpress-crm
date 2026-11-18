package com.chemexpress.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chemexpress.crm.common.Constants;
import com.chemexpress.crm.common.PageResult;
import com.chemexpress.crm.entity.Product;
import com.chemexpress.crm.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;

    public PageResult<Product> page(String keyword, String status, long pageNo, long pageSize) {
        Page<Product> page = new Page<>(Math.max(1, pageNo), Math.min(100, Math.max(1, pageSize)));
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StringUtils.hasText(keyword), w -> w
                .like(Product::getTradeName, keyword)
                .or().like(Product::getGenericName, keyword)
                .or().like(Product::getCode, keyword));
        wrapper.eq(StringUtils.hasText(status), Product::getStatus, status);
        wrapper.orderByAsc(Product::getCode);
        productMapper.selectPage(page, wrapper);
        return PageResult.of(page);
    }

    public List<Product> options() {
        return productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, Constants.PRODUCT_ACTIVE)
                .orderByAsc(Product::getCode));
    }

    public Product get(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw com.chemexpress.crm.common.BizException.notFound("产品不存在");
        }
        return product;
    }
}
