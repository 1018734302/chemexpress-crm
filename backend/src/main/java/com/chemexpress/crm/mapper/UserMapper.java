package com.chemexpress.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chemexpress.crm.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
