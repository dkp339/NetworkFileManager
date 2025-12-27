package com.rubberhuman.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rubberhuman.server.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
