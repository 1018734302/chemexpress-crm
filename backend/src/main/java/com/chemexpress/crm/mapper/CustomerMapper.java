package com.chemexpress.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chemexpress.crm.entity.Customer;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {
}
